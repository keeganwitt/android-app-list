package com.github.keeganwitt.applist

import android.app.AppOpsManager
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.ToggleButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.keeganwitt.applist.services.AndroidPackageService
import com.github.keeganwitt.applist.services.AndroidStorageService
import com.github.keeganwitt.applist.services.AndroidUsageStatsService
import com.github.keeganwitt.applist.services.PlayStoreService
import java.text.Collator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener, AppAdapter.OnClickListener {
    private lateinit var appInfoFields: List<AppInfoField>
    private lateinit var appAdapter: AppAdapter
    private lateinit var spinner: Spinner
    private lateinit var toggleButton: ToggleButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private var showSystemApps = false
    private lateinit var appExporter: AppExporter
    private lateinit var appListViewModel: AppListViewModel
    private lateinit var labelToFieldMap: Map<String, AppInfoField>
    private lateinit var fieldToLabelMap: Map<AppInfoField, String>
    private var latestState: UiState = UiState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isLightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES
        windowInsetsController.isAppearanceLightStatusBars = isLightMode

        appInfoFields = AppInfoField.entries

        progressBar = findViewById(R.id.progress_bar)
        recyclerView = findViewById(R.id.recycler_view)
        spinner = findViewById(R.id.spinner)
        toggleButton = findViewById(R.id.toggleButton)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        appAdapter = AppAdapter(this)
        recyclerView.layoutManager = GridAutofitLayoutManager(this, 450)
        recyclerView.adapter = appAdapter

        val crashReporter = FirebaseCrashReporter()
        appExporter = AppExporter(this, itemsProvider = { appAdapter.currentList }, formatter = ExportFormatter(), crashReporter = crashReporter)

        appListViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val pkg = AndroidPackageService(applicationContext)
                val usage = AndroidUsageStatsService(applicationContext)
                val storage = AndroidStorageService(applicationContext)
                val store = PlayStoreService()
                val repo = AndroidAppRepository(pkg, usage, storage, store, crashReporter)
                val vm = AppListViewModel(repo, DefaultDispatcherProvider(), pkg)
                @Suppress("UNCHECKED_CAST")
                return vm as T
            }
        })[AppListViewModel::class.java]
        observeViewModel()

        fieldToLabelMap = AppInfoField.entries.associateWith { getString(it.titleResId) }
        labelToFieldMap = fieldToLabelMap.entries.associate { (k, v) -> v to k }

        val appInfoFieldStrings = fieldToLabelMap.values.toTypedArray()
        val collator = Collator.getInstance()
        appInfoFieldStrings.sortWith(collator::compare)

        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, appInfoFieldStrings)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrayAdapter

        // initial selection to VERSION
        val initialLabel = fieldToLabelMap[AppInfoField.VERSION] ?: getString(R.string.appInfoField_version)
        val initialIndex = appInfoFieldStrings.indexOf(initialLabel).coerceAtLeast(0)
        spinner.setSelection(initialIndex, false)
        spinner.onItemSelectedListener = this
        appListViewModel.init(AppInfoField.VERSION)

        toggleButton.setOnCheckedChangeListener { _, _ ->
            appListViewModel.toggleDescending()
        }

        swipeRefreshLayout.setOnRefreshListener {
            appListViewModel.refresh()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                appListViewModel.uiState.collectLatest { state ->
                    latestState = state
                    appAdapter.submitList(state.items)
                    progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (state.isLoading) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_menu, menu)

        menu.findItem(R.id.systemAppToggle).isChecked = showSystemApps

        val searchItem = menu.findItem(R.id.search)
        (searchItem.actionView as? SearchView)?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                appListViewModel.setQuery(query ?: "")
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                appListViewModel.setQuery(query ?: "")
                return true
            }
        })

        return true
    }

    override fun onClick(position: Int) {
        val app = appAdapter.currentList[position]
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            data = ("package:" + app.packageName).toUri()
        }
        startActivity(intent)
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        val label = parent.getItemAtPosition(position) as String
        val field = labelToFieldMap[label] ?: AppInfoField.VERSION
        appListViewModel.updateSelectedField(field)
        maybeRequestUsagePermission(field)
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        val adapter = spinner.adapter
        val versionText = getString(R.string.appInfoField_version)
        val versionIndex = (0 until adapter.count).firstOrNull { adapter.getItem(it) == versionText }
        if (versionIndex != null) {
            spinner.setSelection(versionIndex)
        }
        appListViewModel.updateSelectedField(AppInfoField.VERSION)
    }

    override fun onResume() {
        super.onResume()
        appListViewModel.refresh()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.export -> {
                appExporter.export(latestState.selectedField)
                return true
            }
            R.id.systemAppToggle -> {
                item.isChecked = !item.isChecked
                showSystemApps = item.isChecked
                updateSystemAppToggleIcon(item)
                appListViewModel.setShowSystem(showSystemApps)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateSystemAppToggleIcon(item: MenuItem) {
        if (item.isChecked) {
            item.setIcon(R.drawable.ic_system_apps_on)
        } else {
            item.setIcon(R.drawable.ic_system_apps_off)
        }
    }

    private fun maybeRequestUsagePermission(field: AppInfoField) {
        if ((field == AppInfoField.CACHE_SIZE ||
                    field == AppInfoField.DATA_SIZE ||
                    field == AppInfoField.EXTERNAL_CACHE_SIZE ||
                    field == AppInfoField.TOTAL_SIZE ||
                    field == AppInfoField.LAST_USED) && !hasUsageStatsPermission()
        ) {
            requestUsageStatsPermission()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}
