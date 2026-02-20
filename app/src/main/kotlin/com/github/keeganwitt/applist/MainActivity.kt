package com.github.keeganwitt.applist

import android.app.AppOpsManager
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.github.keeganwitt.applist.databinding.ActivityMainBinding
import com.github.keeganwitt.applist.services.AndroidPackageService
import com.github.keeganwitt.applist.services.AndroidStorageService
import com.github.keeganwitt.applist.services.AndroidUsageStatsService
import com.github.keeganwitt.applist.services.PlayStoreService
import com.github.keeganwitt.applist.utils.PermissionUtils
import com.github.keeganwitt.applist.utils.nightMode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.Collator

class MainActivity :
    AppCompatActivity(),
    AdapterView.OnItemSelectedListener,
    AppAdapter.OnClickListener {
    private lateinit var appInfoFields: List<AppInfoField>
    private lateinit var appAdapter: AppAdapter
    private lateinit var binding: ActivityMainBinding
    private lateinit var appExporter: AppExporter
    private lateinit var appListViewModel: AppListViewModel
    private lateinit var labelToFieldMap: Map<String, AppInfoField>
    private lateinit var fieldToLabelMap: Map<AppInfoField, String>
    private lateinit var appSettings: AppSettings
    private var latestState: UiState = UiState()
    private var shouldRefreshOnResume = false
    private var ignoreQueryChanges = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        setupSettings()
        setupTheme()
        setupRecyclerView()
        setupViewModel()
        setupExporter()
        setupSpinner()
        setupListeners()
    }

    private fun setupUI() {
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isLightMode =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK !=
                Configuration.UI_MODE_NIGHT_YES
        windowInsetsController.isAppearanceLightStatusBars = isLightMode
    }

    private fun setupSettings() {
        appInfoFields = AppInfoField.entries
        appSettings = SharedPreferencesAppSettings(this)
    }

    private fun setupTheme() {
        val themeMode = appSettings.getThemeMode()
        androidx.appcompat.app.AppCompatDelegate
            .setDefaultNightMode(themeMode.nightMode)
    }

    private fun setupRecyclerView() {
        appAdapter = AppAdapter(this)
        binding.recyclerView.layoutManager = GridAutofitLayoutManager(this, 450)
        binding.recyclerView.adapter = appAdapter
    }

    private fun setupViewModel() {
        val crashReporter = FirebaseCrashReporter()
        appListViewModel =
            ViewModelProvider(
                this,
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        val pkg = AndroidPackageService(applicationContext)
                        val usage = AndroidUsageStatsService(applicationContext)
                        val storage = AndroidStorageService(applicationContext)
                        val store = PlayStoreService()
                        val repo =
                            AndroidAppRepository(
                                pkg,
                                usage,
                                storage,
                                store,
                                crashReporter,
                            )
                        val vm =
                            AppListViewModel(
                                repo,
                                DefaultDispatcherProvider(),
                                SummaryCalculator(applicationContext, store),
                            )
                        @Suppress("UNCHECKED_CAST")
                        return vm as T
                    }
                },
            )[AppListViewModel::class.java]
        observeViewModel()
    }

    private fun setupExporter() {
        val crashReporter = FirebaseCrashReporter()
        appExporter =
            AppExporter(
                this,
                appsProvider = { latestState.filteredApps },
                formatter = ExportFormatter(),
                appSettings = appSettings,
                crashReporter = crashReporter,
            )
    }

    private fun setupSpinner() {
        fieldToLabelMap = AppInfoField.entries.associateWith { getString(it.titleResId) }
        labelToFieldMap = fieldToLabelMap.entries.associate { (k, v) -> v to k }

        val appInfoFieldStrings = fieldToLabelMap.values.toTypedArray()
        val collator = Collator.getInstance()
        appInfoFieldStrings.sortWith(collator::compare)

        val arrayAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, appInfoFieldStrings)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = arrayAdapter

        // initial selection to VERSION
        val lastDisplayedField = appSettings.getLastDisplayedAppInfoField()
        val initialLabel =
            fieldToLabelMap[lastDisplayedField] ?: getString(R.string.appInfoField_version)
        val initialIndex = appInfoFieldStrings.indexOf(initialLabel).coerceAtLeast(0)
        binding.spinner.setSelection(initialIndex, false)
        binding.spinner.onItemSelectedListener = this
        appListViewModel.init(lastDisplayedField)
    }

    private fun setupListeners() {
        binding.toggleButton.setOnCheckedChangeListener { _, _ -> appListViewModel.toggleDescending() }

        binding.swipeRefreshLayout.setOnRefreshListener {
            appListViewModel.refresh()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        latestState = appListViewModel.uiState.value
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                appListViewModel.uiState.collectLatest { state ->
                    val shouldInvalidate =
                        latestState.isFullyLoaded != state.isFullyLoaded ||
                            (latestState.summary == null) != (state.summary == null) ||
                            latestState.showSystem != state.showSystem
                    latestState = state
                    appAdapter.submitList(state.items)
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.recyclerView.visibility = if (state.isLoading) View.GONE else View.VISIBLE
                    if (shouldInvalidate) {
                        ignoreQueryChanges = true
                        invalidateOptionsMenu()
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        ignoreQueryChanges = false
        menuInflater.inflate(R.menu.app_menu, menu)

        menu.findItem(R.id.systemAppToggle).isChecked = latestState.showSystem

        val summaryItem = menu.findItem(R.id.summary)
        if (!latestState.isFullyLoaded) {
            summaryItem.isEnabled = false
            summaryItem.title = getString(R.string.summary_loading)
        } else {
            summaryItem.isEnabled = latestState.summary != null
            summaryItem.title = getString(R.string.summary)
        }
        val icon = summaryItem.icon
        if (icon != null) {
            icon.alpha = if (summaryItem.isEnabled) 255 else 128
        }

        val searchItem = menu.findItem(R.id.search)
        val searchView = searchItem.actionView as? SearchView
        if (latestState.query.isNotEmpty()) {
            searchItem.expandActionView()
            searchView?.setQuery(latestState.query, false)
        }
        searchView?.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    appListViewModel.setQuery(query ?: "")
                    return true
                }

                override fun onQueryTextChange(query: String?): Boolean {
                    if (ignoreQueryChanges) return true
                    appListViewModel.setQuery(query ?: "")
                    return true
                }
            },
        )

        return true
    }

    override fun onClick(position: Int) {
        val app = appAdapter.currentList[position]
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                data = ("package:" + app.packageName).toUri()
            }
        startActivity(intent)
    }

    override fun onItemSelected(
        parent: AdapterView<*>,
        view: View?,
        position: Int,
        id: Long,
    ) {
        val label = parent.getItemAtPosition(position) as String
        val field = labelToFieldMap[label] ?: AppInfoField.VERSION
        appListViewModel.updateSelectedField(field)
        appSettings.setLastDisplayedAppInfoField(field)
        maybeRequestUsagePermission(field)
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        val adapter = binding.spinner.adapter
        val versionText = getString(R.string.appInfoField_version)
        val versionIndex =
            (0 until adapter.count).firstOrNull { adapter.getItem(it) == versionText }
        if (versionIndex != null) {
            binding.spinner.setSelection(versionIndex)
        }
        appListViewModel.updateSelectedField(AppInfoField.VERSION)
    }

    override fun onResume() {
        super.onResume()
        if (shouldRefreshOnResume) {
            appListViewModel.refresh()
            shouldRefreshOnResume = false
        }
    }

    override fun onStop() {
        super.onStop()
        shouldRefreshOnResume = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.export -> {
                appExporter.export()
                return true
            }

            R.id.systemAppToggle -> {
                appListViewModel.setShowSystem(!latestState.showSystem)
                return true
            }

            R.id.summary -> {
                showSummaryDialog()
                return true
            }

            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSummaryDialog() {
        val summary = latestState.summary
        if (summary == null) {
            Toast.makeText(this, "Summary not available yet", Toast.LENGTH_SHORT).show()
            return
        }

        val view = layoutInflater.inflate(R.layout.dialog_summary, null)
        val titleView = view.findViewById<TextView>(R.id.summary_title)
        titleView.text = getString(summary.field.titleResId)

        val recyclerView = view.findViewById<RecyclerView>(R.id.summary_recycler_view)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerView.adapter = SummaryAdapter(summary.buckets.toList())

        androidx.appcompat.app.AlertDialog
            .Builder(this)
            .setTitle(R.string.summary)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun maybeRequestUsagePermission(field: AppInfoField) {
        if (field.requiresUsageStats && !PermissionUtils.hasUsageStatsPermission(this)) {
            PermissionUtils.requestUsageStatsPermission(this)
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
