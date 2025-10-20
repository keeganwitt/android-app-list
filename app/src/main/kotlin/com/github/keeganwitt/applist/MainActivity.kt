package com.github.keeganwitt.applist

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
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
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.text.Collator

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener, AppInfoAdapter.OnClickListener {
    private lateinit var appInfoFields: List<AppInfoField>
    private var selectedAppInfoField: AppInfoField? = null
    private var descendingSortOrder = false
    private lateinit var appInfoAdapter: AppInfoAdapter
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

    companion object {
        private const val PREFS_NAME = "applist_prefs"
        private const val KEY_SELECTED_FIELD = "selected_app_info_field"
    }

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

        appInfoAdapter = AppInfoAdapter(this, getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager, this)
        recyclerView.layoutManager = GridAutofitLayoutManager(this, 450)
        recyclerView.adapter = appInfoAdapter

        appExporter = AppExporter(this, appInfoAdapter, packageManager, getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager)

        appListViewModel = ViewModelProvider(this)[AppListViewModel::class.java]
        observeViewModel()

        fieldToLabelMap = AppInfoField.entries.associateWith { getString(it.titleResId) }
        labelToFieldMap = fieldToLabelMap.entries.associate { (k, v) -> v to k }

        val appInfoFieldStrings = fieldToLabelMap.values.toTypedArray()
        val collator = Collator.getInstance()
        appInfoFieldStrings.sortWith(collator::compare)

        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, appInfoFieldStrings)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrayAdapter

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedName = prefs.getString(KEY_SELECTED_FIELD, null)
        var initialField = savedName?.let {
            try {
                AppInfoField.valueOf(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        if (initialField == null) {
            // Invalid or missing pref; default to VERSION and clear any bad value
            initialField = AppInfoField.VERSION
            if (savedName != null) {
                prefs.edit().remove(KEY_SELECTED_FIELD).apply()
            }
        }

        // Apply selection in spinner
        val initialLabel = fieldToLabelMap[initialField] ?: getString(R.string.appInfoField_version)
        val initialIndex = appInfoFieldStrings.indexOf(initialLabel).coerceAtLeast(0)
        spinner.setSelection(initialIndex, false)

        // Listener and initial load
        spinner.onItemSelectedListener = this
        loadSelection(initialField)

        toggleButton.setOnCheckedChangeListener { _, _ ->
            descendingSortOrder = !descendingSortOrder
            loadApplications(false)
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadApplications(true)
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        appListViewModel.appList.observe(this) { appList ->
            appInfoAdapter.setUnfilteredList(appList)
            appInfoAdapter.submitList(appList)
        }

        appListViewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading == true) View.VISIBLE else View.GONE
            recyclerView.visibility = if (isLoading == true) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_menu, menu)

        menu.findItem(R.id.systemAppToggle).isChecked = showSystemApps

        val searchItem = menu.findItem(R.id.search)
        (searchItem.actionView as? SearchView)?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                appInfoAdapter.filter.filter(query)
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                appInfoAdapter.filter.filter(query)
                return true
            }
        })

        return true
    }

    override fun onClick(position: Int) {
        val app = appInfoAdapter.currentList[position]
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            data = ("package:" + app.applicationInfo.packageName).toUri()
        }
        startActivity(intent)
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        val label = parent.getItemAtPosition(position) as String
        val field = labelToFieldMap[label] ?: AppInfoField.VERSION
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_FIELD, field.name)
            .apply()
        loadSelection(field)
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        val adapter = spinner.adapter
        val versionText = getString(R.string.appInfoField_version)
        val versionIndex = (0 until adapter.count).firstOrNull { adapter.getItem(it) == versionText }
        if (versionIndex != null) {
            spinner.setSelection(versionIndex)
        }
        loadSelection(AppInfoField.VERSION)
    }

    override fun onResume() {
        super.onResume()
        loadApplications(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.export -> {
                selectedAppInfoField?.let { appExporter.export(it) }
                return true
            }
            R.id.systemAppToggle -> {
                item.isChecked = !item.isChecked
                showSystemApps = item.isChecked
                updateSystemAppToggleIcon(item)
                loadApplications(true)
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

    private fun loadSelection(field: AppInfoField) {
        selectedAppInfoField = field
        if ((field == AppInfoField.CACHE_SIZE ||
                    field == AppInfoField.DATA_SIZE ||
                    field == AppInfoField.EXTERNAL_CACHE_SIZE ||
                    field == AppInfoField.TOTAL_SIZE ||
                    field == AppInfoField.LAST_USED) && !hasUsageStatsPermission()
        ) {
            requestUsageStatsPermission()
        }
        loadApplications(false)
    }

    private fun loadApplications(reload: Boolean) {
        selectedAppInfoField?.let {
            appListViewModel.loadApps(it, showSystemApps, descendingSortOrder, reload)
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
