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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.text.Collator
import androidx.core.net.toUri

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
            window.decorView.systemUiVisibility = 0
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

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

        val appInfoFieldStrings = AppInfoField.entries.map { getString(it.titleResId) }.toTypedArray()
        val collator = Collator.getInstance()
        appInfoFieldStrings.sortWith(collator::compare)
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, appInfoFieldStrings)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrayAdapter

        val versionIndex = appInfoFieldStrings.indexOf(getString(R.string.appInfoField_version))
        if (versionIndex != -1) {
            spinner.setSelection(versionIndex)
        }
        spinner.onItemSelectedListener = this

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
        loadSelection(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        val adapter = spinner.adapter
        val versionText = getString(R.string.appInfoField_version)
        val versionIndex = (0 until adapter.count).firstOrNull { adapter.getItem(it) == versionText }
        if (versionIndex != null) {
            spinner.setSelection(versionIndex)
            loadSelection(versionIndex)
        }
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

    private fun loadSelection(position: Int) {
        selectedAppInfoField = appInfoFields[position]
        val field = selectedAppInfoField
        if (field != null && (field == AppInfoField.CACHE_SIZE ||
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
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}
