package com.github.keeganwitt.applist

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.keeganwitt.applist.databinding.ActivityExportBinding
import com.github.keeganwitt.applist.services.DefaultAppStoreService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.Collator

internal class ExportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExportBinding
    private lateinit var adapter: ExportAppAdapter
    private lateinit var exporter: AppExporter
    private lateinit var viewModel: ExportViewModel
    private var renderingState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupExporter()
        setupViews()
        observeState()
    }

    private fun setupViewModel() {
        val crashReporter = FirebaseCrashReporter()
        val collator = Collator.getInstance()
        val comparator =
            Comparator<App> { first, second ->
                val nameComparison = collator.compare(first.name, second.name)
                if (nameComparison != 0) nameComparison else first.packageName.compareTo(second.packageName)
            }
        viewModel =
            ViewModelProvider(
                this,
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        val store = DefaultAppStoreService(crashReporter = crashReporter)
                        val repository = AppRepositoryFactory.create(applicationContext, store, crashReporter)
                        @Suppress("UNCHECKED_CAST")
                        return ExportViewModel(repository, DefaultDispatcherProvider(), comparator) as T
                    }
                },
            )[ExportViewModel::class.java]
    }

    private fun setupExporter() {
        exporter =
            AppExporter(
                activity = this,
                formatter = ExportFormatter(),
                appSettings = SharedPreferencesAppSettings(this),
                crashReporter = FirebaseCrashReporter(),
                onOutcome = { outcome ->
                    viewModel.handleExportOutcome(outcome)
                    if (outcome == ExportOutcome.SUCCESS) finish()
                },
            )
        viewModel.pendingExportRequest()?.let(exporter::restorePendingRequest)
    }

    private fun setupViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = ExportAppAdapter(viewModel::toggleSelection)
        binding.appList.layoutManager = LinearLayoutManager(this)
        binding.appList.adapter = adapter

        binding.exportScope.setOnCheckedChangeListener { _, checkedId ->
            if (!renderingState) viewModel.setScope(checkedId.toExportScope())
        }
        binding.includeArchived.setOnCheckedChangeListener { _, checked ->
            if (!renderingState) viewModel.setIncludeArchived(checked)
        }
        binding.appTypeFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (!renderingState) {
                val filter =
                    when (checkedIds.firstOrNull()) {
                        R.id.filter_user_apps -> ExportAppTypeFilter.USER
                        R.id.filter_system_apps -> ExportAppTypeFilter.SYSTEM
                        else -> ExportAppTypeFilter.ALL
                    }
                viewModel.setAppTypeFilter(filter)
            }
        }
        binding.exportFormat.setOnCheckedChangeListener { _, checkedId ->
            if (!renderingState) viewModel.setFormat(checkedId.toExportFormat())
        }
        binding.appSearch.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = true

                override fun onQueryTextChange(query: String?): Boolean {
                    if (!renderingState) viewModel.setQuery(query.orEmpty())
                    return true
                }
            },
        )
        binding.selectAllResults.setOnClickListener { viewModel.selectAllVisible() }
        binding.clearSelection.setOnClickListener { viewModel.clearSelection() }
        binding.retry.setOnClickListener { viewModel.refresh() }
        binding.exportButton.setOnClickListener {
            val request = viewModel.beginExport() ?: return@setOnClickListener
            exporter.export(request.format, request.apps)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest(::render)
            }
        }
    }

    private fun render(state: ExportUiState) {
        renderingState = true
        binding.exportScope.check(state.scope.toViewId())
        binding.includeArchived.isChecked = state.includeArchived
        binding.appTypeFilter.check(
            when (state.appTypeFilter) {
                ExportAppTypeFilter.ALL -> R.id.filter_all_types
                ExportAppTypeFilter.USER -> R.id.filter_user_apps
                ExportAppTypeFilter.SYSTEM -> R.id.filter_system_apps
            },
        )
        binding.exportFormat.check(state.format.toViewId())
        if (binding.appSearch.query.toString() != state.query) {
            binding.appSearch.setQuery(state.query, false)
        }
        renderingState = false

        adapter.submitList(state.visibleApps)
        val count = state.selectedCount
        binding.selectedCount.text = resources.getQuantityString(R.plurals.export_selected_count, count, count)
        val hiddenCount = state.hiddenSelectedCount
        binding.hiddenSelectedCount.text =
            resources.getQuantityString(R.plurals.export_hidden_selected_count, hiddenCount, hiddenCount)
        binding.hiddenSelectedCount.visibility = if (hiddenCount > 0) View.VISIBLE else View.GONE
        binding.exportButton.text = resources.getQuantityString(R.plurals.export_action_count, count, count)

        val loaded = !state.isLoading && !state.loadFailed
        binding.loading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.loadFailure.visibility = if (state.loadFailed) View.VISIBLE else View.GONE
        binding.exportOptions.visibility = if (loaded) View.VISIBLE else View.GONE
        binding.customControls.visibility =
            if (loaded && state.scope == ExportScope.CUSTOM) View.VISIBLE else View.GONE
        binding.emptyState.visibility = if (loaded && count == 0) View.VISIBLE else View.GONE
        binding.noResults.visibility = if (state.visibleApps.isEmpty()) View.VISIBLE else View.GONE

        val controlsEnabled = loaded && !state.isExporting
        binding.exportScope.isEnabled = controlsEnabled
        for (index in 0 until binding.exportScope.childCount) {
            binding.exportScope.getChildAt(index).isEnabled = controlsEnabled
        }
        binding.includeArchived.isEnabled = controlsEnabled
        for (index in 0 until binding.appTypeFilter.childCount) {
            binding.appTypeFilter.getChildAt(index).isEnabled = controlsEnabled
        }
        binding.exportFormat.isEnabled = controlsEnabled
        for (index in 0 until binding.exportFormat.childCount) {
            binding.exportFormat.getChildAt(index).isEnabled = controlsEnabled
        }
        binding.appSearch.isEnabled = controlsEnabled
        binding.appList.isEnabled = controlsEnabled
        binding.selectAllResults.isEnabled = controlsEnabled && state.visibleApps.isNotEmpty()
        binding.clearSelection.isEnabled = controlsEnabled && count > 0
        binding.exportButton.isEnabled = state.canExport
    }

    private fun Int.toExportScope(): ExportScope =
        when (this) {
            R.id.scope_system_apps -> ExportScope.SYSTEM_APPS
            R.id.scope_all_apps -> ExportScope.ALL_APPS
            R.id.scope_choose_apps -> ExportScope.CUSTOM
            else -> ExportScope.USER_APPS
        }

    private fun Int.toExportFormat(): ExportFormat =
        when (this) {
            R.id.format_html -> ExportFormat.HTML
            R.id.format_csv -> ExportFormat.CSV
            R.id.format_tsv -> ExportFormat.TSV
            else -> ExportFormat.XML
        }

    private fun ExportScope.toViewId(): Int =
        when (this) {
            ExportScope.USER_APPS -> R.id.scope_user_apps
            ExportScope.SYSTEM_APPS -> R.id.scope_system_apps
            ExportScope.ALL_APPS -> R.id.scope_all_apps
            ExportScope.CUSTOM -> R.id.scope_choose_apps
        }

    private fun ExportFormat.toViewId(): Int =
        when (this) {
            ExportFormat.XML -> R.id.format_xml
            ExportFormat.HTML -> R.id.format_html
            ExportFormat.CSV -> R.id.format_csv
            ExportFormat.TSV -> R.id.format_tsv
        }
}
