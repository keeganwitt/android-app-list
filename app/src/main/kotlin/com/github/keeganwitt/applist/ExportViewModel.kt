package com.github.keeganwitt.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ExportViewModel(
    val repository: AppRepository,
    private val dispatchers: DispatcherProvider,
    private val appComparator: Comparator<App>,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private var allApps: List<App> = emptyList()
    private var selectedPackageNames: Set<String> = emptySet()
    private var pendingExportRequest: ExportRequest? = null

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch(dispatchers.io) {
            try {
                repository.refreshCache(force = true)
                allApps = repository.getCachedApps().sortedWith(appComparator)
                selectedPackageNames = packageNamesForScope(ExportScope.USER_APPS, includeArchived = false)
                emitState()
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun setScope(scope: ExportScope) {
        val current = _uiState.value
        if (current.isExporting) return
        if (scope == ExportScope.CUSTOM) {
            _uiState.value = current.copy(scope = scope)
        } else {
            selectedPackageNames = packageNamesForScope(scope, current.includeArchived)
            _uiState.value = current.copy(scope = scope, query = "")
        }
        emitState()
    }

    fun setIncludeArchived(includeArchived: Boolean) {
        val current = _uiState.value
        if (current.isExporting || current.includeArchived == includeArchived) return

        if (current.scope == ExportScope.CUSTOM) {
            if (!includeArchived) {
                val archivedPackageNames =
                    allApps.filter { it.archived == true }.mapTo(mutableSetOf()) { it.packageName }
                selectedPackageNames = selectedPackageNames - archivedPackageNames
            }
        } else {
            selectedPackageNames = packageNamesForScope(current.scope, includeArchived)
        }
        _uiState.value = current.copy(includeArchived = includeArchived)
        emitState()
    }

    fun setQuery(query: String) {
        if (_uiState.value.isExporting) return
        _uiState.update { it.copy(query = query) }
        emitState()
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

    fun handleExportOutcome(outcome: ExportOutcome) {
        pendingExportRequest = null
        _uiState.update { it.copy(isExporting = false) }
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
        _uiState.update { it.copy(scope = ExportScope.CUSTOM) }
        emitState()
    }

    fun appsForExport(): List<App> = allApps.filter { it.packageName in selectedPackageNames }

    private fun packageNamesForScope(
        scope: ExportScope,
        includeArchived: Boolean,
    ): Set<String> =
        allApps
            .asSequence()
            .filter { app ->
                val visible = if (app.archived == true) includeArchived else app.hasLaunchIntent
                when (scope) {
                    ExportScope.USER_APPS -> app.isUserInstalled && visible
                    ExportScope.SYSTEM_APPS -> !app.isUserInstalled && visible
                    ExportScope.ALL_APPS, ExportScope.CUSTOM -> app.archived != true || includeArchived
                }
            }.map { it.packageName }
            .toSet()

    private fun emitState() {
        val state = _uiState.value
        val selectedItems =
            allApps
                .filter { it.packageName in selectedPackageNames }
                .map { it.toExportItem(isSelected = true) }
        val visibleItems =
            if (state.scope == ExportScope.CUSTOM) {
                allApps
                    .asSequence()
                    .filter { it.archived != true || state.includeArchived }
                    .filter {
                        state.query.isBlank() ||
                            it.name.contains(state.query, ignoreCase = true) ||
                            it.packageName.contains(state.query, ignoreCase = true)
                    }.map { it.toExportItem(it.packageName in selectedPackageNames) }
                    .toList()
            } else {
                emptyList()
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
