package com.github.keeganwitt.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListViewModel(
    private val repository: AppRepository,
    private val dispatchers: DispatcherProvider,
    private val summaryCalculator: SummaryCalculator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var allApps: List<App> = emptyList()
    private var cachedMappedItems: List<AppItemUiModel>? = null
    private var cachedMappedItemsField: AppInfoField? = null

    fun init(initialField: AppInfoField) {
        _uiState.update { it.copy(selectedField = initialField) }
        loadApps(reload = false)
    }

    fun updateSelectedField(field: AppInfoField) {
        _uiState.update { it.copy(selectedField = field) }
        loadApps(reload = false)
    }

    fun toggleDescending() {
        _uiState.update { it.copy(descending = !it.descending) }
        loadApps(reload = false)
    }

    fun setShowSystem(show: Boolean) {
        _uiState.update { it.copy(showSystem = show) }
        loadApps(reload = true)
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        applyFilterAndEmit()
    }

    fun refresh() {
        loadApps(reload = true)
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    private fun loadApps(reload: Boolean) {
        loadJob?.cancel()
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true) }

        loadJob =
            viewModelScope.launch(dispatchers.io) {
                repository
                    .loadApps(
                        field = state.selectedField,
                        showSystemApps = state.showSystem,
                        descending = state.descending,
                        reload = reload,
                    ).collect { apps ->
                        withContext(dispatchers.main) {
                            allApps = apps
                            val field = _uiState.value.selectedField
                            cachedMappedItems = apps.map { mapToItem(it, field) }
                            cachedMappedItemsField = field
                            _uiState.update { it.copy(isLoading = false) }
                            applyFilterAndEmit()
                        }
                    }
            }
    }

    private fun applyFilterAndEmit() {
        val state = _uiState.value
        if (cachedMappedItems == null || cachedMappedItemsField != state.selectedField) {
            cachedMappedItems = allApps.map { mapToItem(it, state.selectedField) }
            cachedMappedItemsField = state.selectedField
        }
        val list = cachedMappedItems ?: emptyList()
        val filtered =
            if (state.query.isBlank()) {
                list
            } else {
                list.filter { item ->
                    item.appName.contains(state.query, ignoreCase = true) ||
                        item.packageName.contains(state.query, ignoreCase = true) ||
                        item.infoText.contains(state.query, ignoreCase = true)
                }
            }
        _uiState.update { it.copy(items = filtered) }

        viewModelScope.launch(dispatchers.default) {
            val filteredApps =
                if (state.query.isBlank()) {
                    allApps
                } else {
                    val filteredPackageNames = filtered.map { it.packageName }.toSet()
                    allApps.filter { app -> app.packageName in filteredPackageNames }
                }

            val summary = summaryCalculator.calculate(filteredApps, state.selectedField)
            withContext(dispatchers.main) {
                _uiState.update { it.copy(summary = summary) }
            }
        }
    }

    private fun mapToItem(
        app: App,
        field: AppInfoField,
    ): AppItemUiModel {
        val info = field.getFormattedValue(app)
        return AppItemUiModel(
            packageName = app.packageName,
            appName = app.name,
            infoText = info,
            isLoading = !app.isDetailed,
        )
    }
}
