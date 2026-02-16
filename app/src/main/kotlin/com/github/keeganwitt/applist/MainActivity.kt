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
import com.github.keeganwitt.applist.databinding.ActivityMainBinding
import com.github.keeganwitt.applist.services.AndroidPackageService
import com.github.keeganwitt.applist.services.AndroidStorageService
import com.github.keeganwitt.applist.services.AndroidUsageStatsService
import com.github.keeganwitt.applist.services.PlayStoreService
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
    private var showSystemApps = false
    private lateinit var appExporter: AppExporter
    private lateinit var appListViewModel: AppListViewModel
    private lateinit var labelToFieldMap: Map<String, AppInfoField>
    private lateinit var fieldToLabelMap: Map<AppInfoField, String>
    private lateinit var appSettings: AppSettings
    private var latestState: UiState = UiState()
    private var shouldRefreshOnResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isLightMode =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK !=
                Configuration.UI_MODE_NIGHT_YES
        windowInsetsController.isAppearanceLightStatusBars = isLightMode

        appInfoFields = AppInfoField.entries
        appSettings = SharedPreferencesAppSettings(this)

        val themeMode = appSettings.getThemeMode()
        val nightMode =
            when (themeMode) {
                AppSettings.ThemeMode.LIGHT -> {
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                }

                AppSettings.ThemeMode.DARK -> {
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                }

                AppSettings.ThemeMode.SYSTEM -> {
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            }
        androidx.appcompat.app.AppCompatDelegate
            .setDefaultNightMode(nightMode)

        appAdapter = AppAdapter(this)
        binding.recyclerView.layoutManager = GridAutofitLayoutManager(this, 450)
        binding.recyclerView.adapter = appAdapter

        val crashReporter = FirebaseCrashReporter()
        appExporter =
            AppExporter(
                this,
                itemsProvider = { appAdapter.currentList },
                formatter = ExportFormatter(),
                crashReporter = crashReporter,
            )

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
                        val vm = AppListViewModel(repo, DefaultDispatcherProvider())
                        @Suppress("UNCHECKED_CAST")
                        return vm as T
                    }
                },
            )[AppListViewModel::class.java]
        observeViewModel()

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

        binding.toggleButton.setOnCheckedChangeListener { _, _ -> appListViewModel.toggleDescending() }

        binding.swipeRefreshLayout.setOnRefreshListener {
            appListViewModel.refresh()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                appListViewModel.uiState.collectLatest { state ->
                    latestState = state
                    appAdapter.submitList(state.items)
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.recyclerView.visibility = if (state.isLoading) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_menu, menu)

        menu.findItem(R.id.systemAppToggle).isChecked = showSystemApps

        val searchItem = menu.findItem(R.id.search)
        (searchItem.actionView as? SearchView)?.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    appListViewModel.setQuery(query ?: "")
                    return true
                }

                override fun onQueryTextChange(query: String?): Boolean {
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

            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
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
        if ((
                field == AppInfoField.CACHE_SIZE ||
                    field == AppInfoField.DATA_SIZE ||
                    field == AppInfoField.EXTERNAL_CACHE_SIZE ||
                    field == AppInfoField.TOTAL_SIZE ||
                    field == AppInfoField.LAST_USED
            ) && !hasUsageStatsPermission()
        ) {
            requestUsageStatsPermission()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode =
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName,
            )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        val intent =
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            val fallbackIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            if (fallbackIntent.resolveActivity(packageManager) != null) {
                startActivity(fallbackIntent)
            } else {
                Log.w(TAG, "No Activity found to handle USAGE_ACCESS_SETTINGS intent.")
                Toast
                    .makeText(
                        this,
                        "Please enable Usage Access permission manually in Settings",
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
