package com.github.keeganwitt.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppListViewModel(
    val repository: AppRepository,
    private val dispatchers: DispatcherProvider,
    private val summaryCalculator: SummaryCalculator,
    private val sizeFormatter: (Long) -> String,
    private val unknownValue: String,
    private val loadingFailedValue: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSyncState().collect { state ->
                _uiState.update { it.copy(syncState = state) }
            }
        }
    }

    private var allApps: List<App> = emptyList()
    private var cachedMappedItems: List<AppItemUiModel>? = null
    private var cachedMappedItemsField: AppInfoField? = null
    private val loadMutex = Mutex()

    fun init(
        initialField: AppInfoField,
        initialSystemAppsOnly: Boolean,
        initialShowArchived: Boolean,
        initialDescending: Boolean,
    ) {
        _uiState.update {
            it.copy(
                selectedField = initialField,
                systemAppsOnly = initialSystemAppsOnly,
                showArchived = initialShowArchived,
                descending = initialDescending,
            )
        }
        loadApps(reload = false)
    }

    fun updateSelectedField(field: AppInfoField) {
        _uiState.update { it.copy(selectedField = field) }
        loadApps(reload = false)
    }

    fun setDescending(descending: Boolean) {
        _uiState.update { it.copy(descending = descending) }
        loadApps(reload = false)
    }

    fun toggleDescending() {
        setDescending(!_uiState.value.descending)
    }

    fun setSystemAppsOnly(enabled: Boolean) {
        _uiState.update { it.copy(systemAppsOnly = enabled) }
        loadApps(reload = true)
    }

    fun setShowArchived(show: Boolean) {
        _uiState.update { it.copy(showArchived = show) }
        loadApps(reload = true)
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        applyFilterAndEmit()
    }

    fun refresh() {
        loadApps(reload = true)
    }

    private var loadJob: Job? = null
    private var summaryJob: Job? = null
    private var cachedLoadOptions: LoadOptions? = null
    private var loadRequestId = 0L

    private fun loadApps(reload: Boolean) {
        val requestId = ++loadRequestId
        loadJob?.cancel()
        summaryJob?.cancel()
        val state = _uiState.value
        val options =
            LoadOptions(
                state.selectedField,
                state.systemAppsOnly,
                state.showArchived || state.selectedField == AppInfoField.ARCHIVED,
                state.descending,
            )
        _uiState.update {
            it.copy(
                isLoading = true,
                loadFailed = false,
                isFullyLoaded = false,
                summary = null,
            )
        }

        loadJob =
            viewModelScope.launch(dispatchers.io) {
                try {
                    loadMutex.withLock {
                        repository
                            .loadApps(
                                field = options.field,
                                systemAppsOnly = options.systemAppsOnly,
                                showArchivedApps = options.showArchivedApps,
                                descending = options.descending,
                                reload = reload,
                            ).collect { apps ->
                                withContext(dispatchers.main) {
                                    allApps = apps
                                    val field = _uiState.value.selectedField
                                    cachedMappedItems = apps.map { mapToItem(it, field) }
                                    cachedMappedItemsField = field
                                    cachedLoadOptions = options
                                    val fullyLoaded = apps.isEmpty() || apps.all { it.isDetailed }
                                    _uiState.update {
                                        it.copy(isLoading = false, loadFailed = false, isFullyLoaded = fullyLoaded)
                                    }
                                    applyFilterAndEmit()
                                }
                            }
                    }
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    withContext(dispatchers.main) {
                        if (requestId != loadRequestId) return@withContext
                        val discardResults = options != cachedLoadOptions || options.field != cachedMappedItemsField
                        if (discardResults) {
                            summaryJob?.cancel()
                            allApps = emptyList()
                            cachedMappedItems = null
                            cachedMappedItemsField = null
                            cachedLoadOptions = null
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadFailed = true,
                                isFullyLoaded = false,
                                items = if (discardResults) emptyList() else it.items,
                                filteredApps = if (discardResults) emptyList() else it.filteredApps,
                                summary = null,
                            )
                        }
                    }
                }
            }
    }

    private fun applyFilterAndEmit() {
        summaryJob?.cancel()
        val state = _uiState.value
        val apps = allApps
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

        summaryJob =
            viewModelScope.launch(dispatchers.default) {
                val filteredApps =
                    if (state.query.isBlank()) {
                        apps
                    } else {
                        val filteredPackageNames = filtered.map { it.packageName }.toSet()
                        apps.filter { app -> app.packageName in filteredPackageNames }
                    }

                if (state.isFullyLoaded) {
                    val summary = summaryCalculator.calculate(filteredApps, state.selectedField)
                    withContext(dispatchers.main) {
                        _uiState.update { it.copy(summary = summary, filteredApps = filteredApps) }
                    }
                } else {
                    withContext(dispatchers.main) {
                        _uiState.update { it.copy(summary = null, filteredApps = filteredApps) }
                    }
                }
            }
    }

    private fun mapToItem(
        app: App,
        field: AppInfoField,
    ): AppItemUiModel {
        val rawValue = field.getValue(app)
        val info =
            if (field == AppInfoField.APP_NAME || field == AppInfoField.PACKAGE_NAME) {
                ""
            } else if (field in app.failedFields) {
                loadingFailedValue
            } else if (field.isSize &&
                rawValue is Long &&
                (rawValue > 0 || (field != AppInfoField.APK_SIZE && field != AppInfoField.TOTAL_SIZE))
            ) {
                sizeFormatter(rawValue)
            } else {
                field.getFormattedValue(app, unknownValue, null)
            }
        return AppItemUiModel(
            packageName = app.packageName,
            appName = app.name,
            infoText = info,
            infoUrl = info.takeIf { field == AppInfoField.STORE_URL && it.isNotBlank() },
            storeUrl = app.storeUrl,
            isLoading = !app.isDetailed,
        )
    }

    private data class LoadOptions(
        val field: AppInfoField,
        val systemAppsOnly: Boolean,
        val showArchivedApps: Boolean,
        val descending: Boolean,
    )
}
