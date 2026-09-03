package com.github.keeganwitt.applist

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ExportViewModel(
    val repository: AppRepository,
    private val dispatchers: DispatcherProvider,
    private val appComparator: Comparator<App>,
    private val exportFileWriter: ExportFileWriter,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private var allApps: List<App> = emptyList()
    private var selectedPackageNames: Set<String> = emptySet()
    private var hasInitializedSelection = false
    private var cacheObservationJob: Job? = null
    private var pendingExportRequest: ExportRequest? = null
    private var exportWriteStarted = false
    private val _exportCompletion = MutableStateFlow<ExportCompletion?>(null)
    val exportCompletion: StateFlow<ExportCompletion?> = _exportCompletion.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val isRetry = _uiState.value.loadFailed
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        cacheObservationJob?.cancel()
        cacheObservationJob =
            viewModelScope.launch(dispatchers.io) {
                try {
                    var cachedApps = repository.getCachedApps()
                    if (isRetry || cachedApps.isEmpty()) {
                        repository.refreshCache(force = true)
                        cachedApps = repository.getCachedApps()
                    }
                    applyCacheUpdate(cachedApps)
                    repository.observeCachedApps().collect(::applyCacheUpdate)
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
            }
    }

    private suspend fun applyCacheUpdate(apps: List<App>) {
        withContext(dispatchers.main) {
            updateApps(apps)
        }
    }

    private fun updateApps(apps: List<App>) {
        allApps = apps.sortedWith(appComparator)
        val availablePackageNames = allApps.mapTo(mutableSetOf()) { it.packageName }
        selectedPackageNames =
            if (hasInitializedSelection) {
                selectedPackageNames.intersect(availablePackageNames)
            } else {
                allApps
                    .filter { it.isUserInstalled && it.archived != true }
                    .mapTo(mutableSetOf()) { it.packageName }
            }
        hasInitializedSelection = true
        emitState()
    }

    fun setShowArchived(showArchived: Boolean) {
        val current = _uiState.value
        if (current.isExporting || current.showArchived == showArchived) return

        emitState(current.copy(showArchived = showArchived))
    }

    fun setQuery(query: String) {
        if (_uiState.value.isExporting) return
        emitState(_uiState.value.copy(query = query))
    }

    fun setAppTypeFilter(filter: ExportAppTypeFilter) {
        val current = _uiState.value
        if (current.isExporting || current.appTypeFilter == filter) return
        emitState(current.copy(appTypeFilter = filter))
    }

    fun showSelectionReview() {
        val current = _uiState.value
        if (current.isExporting || current.isReviewingSelection) return
        emitState(current.copy(isReviewingSelection = true))
    }

    fun showBrowse() {
        val current = _uiState.value
        if (current.isExporting || !current.isReviewingSelection) return
        emitState(current.copy(isReviewingSelection = false))
    }

    fun setFormat(format: ExportFormat) {
        if (_uiState.value.isExporting) return
        _uiState.update { it.copy(format = format) }
    }

    fun beginExport(): ExportRequest? {
        val state = _uiState.value
        if (!state.canExport) return null
        val request = ExportRequest(state.format, appsForExport().toList())
        pendingExportRequest = request
        _uiState.update { it.copy(isExporting = true) }
        return request
    }

    fun pendingExportRequest(): ExportRequest? = pendingExportRequest

    fun writePendingExport(uri: Uri) {
        val request = pendingExportRequest
        if (request == null) {
            completeExport(ExportCompletion(ExportOutcome.FAILURE))
            return
        }
        if (exportWriteStarted) return
        exportWriteStarted = true
        viewModelScope.launch(dispatchers.io) {
            completeExport(exportFileWriter.write(uri, request))
        }
    }

    fun cancelPendingExport() {
        if (pendingExportRequest == null || exportWriteStarted) return
        completeExport(ExportCompletion(ExportOutcome.CANCELED))
    }

    fun failPendingExport(errorMessage: String?) {
        if (pendingExportRequest == null || exportWriteStarted) return
        completeExport(ExportCompletion(ExportOutcome.FAILURE, errorMessage))
    }

    fun consumeExportCompletion(completion: ExportCompletion): Boolean {
        if (_exportCompletion.value !== completion) return false
        _exportCompletion.value = null
        return true
    }

    private fun completeExport(completion: ExportCompletion) {
        pendingExportRequest = null
        exportWriteStarted = false
        _uiState.update { it.copy(isExporting = false) }
        _exportCompletion.value = completion
    }

    fun selectAllVisible() {
        if (_uiState.value.isExporting) return
        selectedPackageNames = selectedPackageNames + _uiState.value.visibleApps.map { it.packageName }
        emitState()
    }

    fun clearSelection() {
        if (_uiState.value.isExporting) return
        selectedPackageNames = emptySet()
        emitState()
    }

    fun toggleSelection(packageName: String) {
        if (_uiState.value.isExporting) return
        if (allApps.none { it.packageName == packageName }) return
        selectedPackageNames =
            if (packageName in selectedPackageNames) {
                selectedPackageNames - packageName
            } else {
                selectedPackageNames + packageName
            }
        emitState()
    }

    fun appsForExport(): List<App> = allApps.filter { it.packageName in selectedPackageNames }

    private fun emitState(state: ExportUiState = _uiState.value) {
        val selectedItems =
            allApps
                .filter { it.packageName in selectedPackageNames }
                .map { it.toExportItem(isSelected = true) }
        val visibleItems =
            if (state.isReviewingSelection) {
                selectedItems
            } else {
                allApps
                    .asSequence()
                    .filter { it.archived != true || state.showArchived }
                    .filter {
                        when (state.appTypeFilter) {
                            ExportAppTypeFilter.ALL -> true
                            ExportAppTypeFilter.USER -> it.isUserInstalled
                            ExportAppTypeFilter.SYSTEM -> !it.isUserInstalled
                        }
                    }.filter {
                        state.query.isBlank() ||
                            it.name.contains(state.query, ignoreCase = true) ||
                            it.packageName.contains(state.query, ignoreCase = true)
                    }.map { it.toExportItem(it.packageName in selectedPackageNames) }
                    .toList()
            }
        _uiState.value =
            state.copy(
                visibleApps = visibleItems,
                selectedApps = selectedItems,
                isLoading = false,
                loadFailed = false,
            )
    }

    private fun App.toExportItem(isSelected: Boolean): ExportAppItemUiModel =
        ExportAppItemUiModel(
            packageName = packageName,
            appName = name,
            isUserInstalled = isUserInstalled,
            isArchived = archived == true,
            isSelected = isSelected,
        )
}
