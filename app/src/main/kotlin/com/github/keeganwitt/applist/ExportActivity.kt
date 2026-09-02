package com.github.keeganwitt.applist

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
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
                        val writer =
                            DefaultExportFileWriter(
                                contentResolver = applicationContext.contentResolver,
                                formatter = ExportFormatter(),
                                appSettings = SharedPreferencesAppSettings(applicationContext),
                                loadingFailedValue = applicationContext.getString(R.string.export_loading_failed),
                                crashReporter = crashReporter,
                            )
                        @Suppress("UNCHECKED_CAST")
                        return ExportViewModel(repository, DefaultDispatcherProvider(), comparator, writer) as T
                    }
                },
            )[ExportViewModel::class.java]
    }

    private fun setupExporter() {
        exporter =
            AppExporter(
                activity = this,
                crashReporter = FirebaseCrashReporter(),
                onResult = { uri ->
                    if (uri == null) viewModel.cancelPendingExport() else viewModel.writePendingExport(uri)
                },
                onLaunchFailure = viewModel::failPendingExport,
            )
    }

    private fun setupViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { handleBackNavigation() }
        onBackPressedDispatcher.addCallback(this) { handleBackNavigation() }

        adapter = ExportAppAdapter(viewModel::toggleSelection)
        binding.appList.layoutManager = LinearLayoutManager(this)
        binding.appList.adapter = adapter

        binding.showArchived.setOnCheckedChangeListener { _, checked ->
            if (!renderingState) viewModel.setShowArchived(checked)
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
        binding.appSearch.doAfterTextChanged { query ->
            if (!renderingState) viewModel.setQuery(query?.toString().orEmpty())
        }
        binding.selectAllResults.setOnClickListener { viewModel.selectAllVisible() }
        binding.clearSelection.setOnClickListener { viewModel.clearSelection() }
        binding.reviewSelected.setOnClickListener { viewModel.showSelectionReview() }
        binding.returnToBrowse.setOnClickListener { handleBackNavigation() }
        binding.retry.setOnClickListener { viewModel.refresh() }
        binding.exportButton.setOnClickListener {
            val request = viewModel.beginExport() ?: return@setOnClickListener
            exporter.export(request.format)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest(::render)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exportCompletion.collectLatest { completion ->
                    completion ?: return@collectLatest
                    if (!viewModel.consumeExportCompletion(completion)) return@collectLatest
                    when (completion.outcome) {
                        ExportOutcome.SUCCESS -> {
                            Toast.makeText(this@ExportActivity, R.string.export_successful, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        ExportOutcome.FAILURE ->
                            Toast
                                .makeText(
                                    this@ExportActivity,
                                    exportFailureMessage(this@ExportActivity, completion.errorMessage),
                                    Toast.LENGTH_SHORT,
                                ).show()
                        ExportOutcome.CANCELED -> Unit
                    }
                }
            }
        }
    }

    private fun render(state: ExportUiState) {
        renderingState = true
        binding.showArchived.isChecked = state.showArchived
        binding.appTypeFilter.check(
            when (state.appTypeFilter) {
                ExportAppTypeFilter.ALL -> R.id.filter_all_types
                ExportAppTypeFilter.USER -> R.id.filter_user_apps
                ExportAppTypeFilter.SYSTEM -> R.id.filter_system_apps
            },
        )
        binding.exportFormat.check(state.format.toViewId())
        if (binding.appSearch.text.toString() != state.query) {
            binding.appSearch.setText(state.query)
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
        binding.selectionControls.visibility = if (loaded) View.VISIBLE else View.GONE
        binding.selectionFooter.visibility = if (loaded) View.VISIBLE else View.GONE
        binding.browseFilters.visibility = if (state.isReviewingSelection) View.GONE else View.VISIBLE
        binding.reviewHeader.visibility = if (state.isReviewingSelection) View.VISIBLE else View.GONE
        binding.selectAllResults.visibility = if (state.isReviewingSelection) View.GONE else View.VISIBLE
        binding.reviewSelected.visibility = if (state.isReviewingSelection) View.GONE else View.VISIBLE
        binding.clearSelection.text =
            getString(if (state.isReviewingSelection) R.string.export_clear_all else R.string.export_clear_selection)
        binding.noResults.text =
            getString(if (state.isReviewingSelection) R.string.export_empty else R.string.export_no_results)
        binding.noResults.visibility = if (state.visibleApps.isEmpty()) View.VISIBLE else View.GONE

        val controlsEnabled = loaded && !state.isExporting
        binding.showArchived.isEnabled = controlsEnabled
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
        binding.reviewSelected.isEnabled = controlsEnabled && count > 0
        binding.returnToBrowse.isEnabled = controlsEnabled
        binding.exportButton.isEnabled = state.canExport
    }

    private fun handleBackNavigation() {
        if (viewModel.uiState.value.isReviewingSelection) {
            viewModel.showBrowse()
        } else {
            finish()
        }
    }

    private fun Int.toExportFormat(): ExportFormat =
        when (this) {
            R.id.format_html -> ExportFormat.HTML
            R.id.format_csv -> ExportFormat.CSV
            R.id.format_tsv -> ExportFormat.TSV
            else -> ExportFormat.XML
        }

    private fun ExportFormat.toViewId(): Int =
        when (this) {
            ExportFormat.XML -> R.id.format_xml
            ExportFormat.HTML -> R.id.format_html
            ExportFormat.CSV -> R.id.format_csv
            ExportFormat.TSV -> R.id.format_tsv
        }
}

internal fun exportFailureMessage(
    context: Context,
    errorMessage: String?,
): String =
    if (errorMessage.isNullOrBlank()) {
        context.getString(R.string.export_failed_generic)
    } else {
        context.getString(R.string.export_failed, errorMessage)
    }
