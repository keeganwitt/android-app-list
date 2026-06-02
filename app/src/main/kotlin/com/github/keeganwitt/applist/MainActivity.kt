package com.github.keeganwitt.applist

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import com.github.keeganwitt.applist.db.AppDatabase
import com.github.keeganwitt.applist.services.AndroidPackageService
import com.github.keeganwitt.applist.services.AndroidStorageService
import com.github.keeganwitt.applist.services.AndroidUsageStatsService
import com.github.keeganwitt.applist.services.DefaultAppStoreService
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
    private lateinit var appRepository: AppRepository
    private lateinit var labelToFieldMap: Map<String, AppInfoField>
    private lateinit var fieldToLabelMap: Map<AppInfoField, String>
    private lateinit var appSettings: AppSettings
    private var latestState: UiState = UiState()
    private var shouldRefreshOnResume = false
    private var ignoreQueryChanges = false
    private var pendingField: AppInfoField? = null

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
        AppCompatDelegate
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
                        val usage = AndroidUsageStatsService(applicationContext, crashReporter = crashReporter)
                        val storage = AndroidStorageService(applicationContext, crashReporter = crashReporter)
                        val store = DefaultAppStoreService(crashReporter = crashReporter)
                        val repository =
                            AndroidAppRepository(
                                pkg,
                                usage,
                                storage,
                                store,
                                AppDatabase.getDatabase(applicationContext).appDao(),
                                crashReporter,
                            )
                        val vm =
                            AppListViewModel(
                                repository,
                                DefaultDispatcherProvider(),
                                SummaryCalculator(applicationContext, store),
                                sizeFormatter = {
                                    Formatter
                                        .formatFileSize(applicationContext, it)
                                },
                                unknownValue = getString(R.string.unknown),
                                loadingFailedValue = getString(R.string.loading_failed),
                            )
                        @Suppress("UNCHECKED_CAST")
                        return vm as T
                    }
                },
            )[AppListViewModel::class.java]
        appRepository = appListViewModel.repository
        observeViewModel()
    }

    private fun setupExporter() {
        val crashReporter = FirebaseCrashReporter()
        appExporter =
            AppExporter(
                this,
                repository = appRepository,
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

        val lastDisplayedField = appSettings.getLastDisplayedAppInfoField()
        if (lastDisplayedField.requiresUsageStats && !PermissionUtils.hasUsageStatsPermission(this)) {
            val initialLabel = getString(AppInfoField.DEFAULT.titleResId)
            val initialIndex = appInfoFieldStrings.indexOf(initialLabel).coerceAtLeast(0)
            binding.spinner.setSelection(initialIndex, false)
            binding.spinner.onItemSelectedListener = this
            appListViewModel.init(
                AppInfoField.DEFAULT,
                appSettings.isShowSystemAppsEnabled(),
                appSettings.isShowArchivedAppsEnabled(),
                appSettings.isDescending(),
            )
            maybeRequestUsagePermission(lastDisplayedField) {
                appListViewModel.updateSelectedField(lastDisplayedField)
                appSettings.setLastDisplayedAppInfoField(lastDisplayedField)
            }
        } else {
            val initialLabel =
                fieldToLabelMap[lastDisplayedField] ?: getString(AppInfoField.DEFAULT.titleResId)
            val initialIndex = appInfoFieldStrings.indexOf(initialLabel).coerceAtLeast(0)
            binding.spinner.setSelection(initialIndex, false)
            binding.spinner.onItemSelectedListener = this
            appListViewModel.init(
                lastDisplayedField,
                appSettings.isShowSystemAppsEnabled(),
                appSettings.isShowArchivedAppsEnabled(),
                appSettings.isDescending(),
            )
        }
    }

    private fun setupListeners() {
        binding.toggleButton.isChecked = appSettings.isDescending()
        binding.toggleButton.setOnCheckedChangeListener { _, isChecked ->
            appListViewModel.setDescending(isChecked)
            appSettings.setDescending(isChecked)
        }

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
                            latestState.showSystem != state.showSystem ||
                            latestState.showArchived != state.showArchived ||
                            latestState.selectedField != state.selectedField
                    latestState = state
                    appAdapter.submitList(state.items)
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.recyclerView.visibility = if (state.isLoading) View.GONE else View.VISIBLE
                    if (shouldInvalidate) {
                        ignoreQueryChanges = true
                        invalidateOptionsMenu()
                    }
                    updateSyncUi(state.syncState)
                }
            }
        }
    }

    private fun updateSyncUi(syncState: SyncState) {
        when (syncState) {
            is SyncState.BuildingInitial -> {
                binding.initialSyncOverlay.visibility = View.VISIBLE
                binding.initialSyncProgressBar.isIndeterminate = syncState.total == 0
                if (syncState.total > 0) {
                    binding.initialSyncProgressBar.progress = ((syncState.progress * 100) / syncState.total).coerceIn(0, 100)
                }
                binding.initialSyncProgressText.text = getString(R.string.sync_progress, syncState.progress, syncState.total)
            }

            SyncState.SyncingBackground -> {
                binding.initialSyncOverlay.visibility = View.GONE
            }

            SyncState.Idle -> {
                binding.initialSyncOverlay.visibility = View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        ignoreQueryChanges = false
        menuInflater.inflate(R.menu.app_menu, menu)

        menu.findItem(R.id.systemAppToggle).isChecked = latestState.showSystem

        val archivedToggle = menu.findItem(R.id.archivedAppToggle)
        val isArchivedFieldSelected = latestState.selectedField == AppInfoField.ARCHIVED
        archivedToggle.isChecked = latestState.showArchived || isArchivedFieldSelected
        archivedToggle.isEnabled = !isArchivedFieldSelected

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

    override fun onStoreUrlClick(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    override fun onItemSelected(
        parent: AdapterView<*>,
        view: View?,
        position: Int,
        id: Long,
    ) {
        val label = parent.getItemAtPosition(position) as String
        val field = labelToFieldMap[label] ?: AppInfoField.DEFAULT
        if (field == appListViewModel.uiState.value.selectedField) return
        maybeRequestUsagePermission(field) {
            appListViewModel.updateSelectedField(field)
            appSettings.setLastDisplayedAppInfoField(field)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        updateFieldSelection(AppInfoField.DEFAULT)
    }

    override fun onResume() {
        super.onResume()
        val hasPermission = PermissionUtils.hasUsageStatsPermission(this)
        val currentField = appListViewModel.uiState.value.selectedField

        if (pendingField != null) {
            val fieldToSelect = if (hasPermission) pendingField!! else AppInfoField.DEFAULT
            pendingField = null
            updateFieldSelection(fieldToSelect)
        } else if (currentField.requiresUsageStats && !hasPermission) {
            updateFieldSelection(AppInfoField.DEFAULT)
        }

        if (shouldRefreshOnResume) {
            appListViewModel.refresh()
            shouldRefreshOnResume = false
        }
    }

    private fun updateFieldSelection(field: AppInfoField) {
        if (appListViewModel.uiState.value.selectedField != field) {
            appListViewModel.updateSelectedField(field)
            appSettings.setLastDisplayedAppInfoField(field)
        }
        val label = fieldToLabelMap[field]
        val adapter = binding.spinner.adapter
        val index = (0 until adapter.count).indexOfFirst { adapter.getItem(it) == label }.coerceAtLeast(0)
        if (binding.spinner.selectedItemPosition != index) {
            binding.spinner.setSelection(index, false)
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
                val newValue = !latestState.showSystem
                appListViewModel.setShowSystem(newValue)
                appSettings.setShowSystemAppsEnabled(newValue)
                return true
            }

            R.id.archivedAppToggle -> {
                val newValue = !latestState.showArchived
                appListViewModel.setShowArchived(newValue)
                appSettings.setShowArchivedAppsEnabled(newValue)
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

        AlertDialog
            .Builder(this)
            .setTitle(R.string.summary)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun maybeRequestUsagePermission(
        field: AppInfoField,
        onAllowed: () -> Unit,
    ) {
        if (field.requiresUsageStats && !PermissionUtils.hasUsageStatsPermission(this)) {
            PermissionUtils.showUsageStatsPermissionDialog(
                this,
                onConfirm = {
                    pendingField = field
                    PermissionUtils.requestUsageStatsPermission(this)
                },
                onCancel = {
                    pendingField = null
                    updateFieldSelection(AppInfoField.DEFAULT)
                },
            )
        } else {
            pendingField = null
            onAllowed()
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
