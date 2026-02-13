package com.github.keeganwitt.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.keeganwitt.applist.services.PackageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

class AppListViewModel(
    private val repository: AppRepository,
    private val dispatchers: DispatcherProvider,
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
                val q = state.query.lowercase()
                list.filter { item ->
                    item.appNameLower.contains(q) ||
                        item.packageNameLower.contains(q) ||
                        item.infoTextLower.contains(q)
                }
            }
        _uiState.update { it.copy(items = filtered) }
    }

    private fun mapToItem(
        app: App,
        field: AppInfoField,
    ): AppItemUiModel {
        val info =
            when (field) {
                AppInfoField.APK_SIZE -> app.sizes.apkBytes.toString()
                AppInfoField.APP_SIZE -> app.sizes.appBytes.toString()
                AppInfoField.CACHE_SIZE -> app.sizes.cacheBytes.toString()
                AppInfoField.DATA_SIZE -> app.sizes.dataBytes.toString()
                AppInfoField.ENABLED -> app.enabled.toString()
                AppInfoField.ARCHIVED -> (app.archived ?: false).toString()
                AppInfoField.EXISTS_IN_APP_STORE -> (app.existsInStore ?: false).toString()
                AppInfoField.EXTERNAL_CACHE_SIZE -> app.sizes.externalCacheBytes.toString()
                AppInfoField.FIRST_INSTALLED -> app.firstInstalled?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: ""
                AppInfoField.LAST_UPDATED -> app.lastUpdated?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: ""
                AppInfoField.LAST_USED -> app.lastUsed?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: ""
                AppInfoField.MIN_SDK -> app.minSdk?.toString() ?: ""
                AppInfoField.PACKAGE_MANAGER -> app.installerName ?: ""
                AppInfoField.GRANTED_PERMISSIONS -> app.grantedPermissionsCount?.toString() ?: "0"
                AppInfoField.REQUESTED_PERMISSIONS -> app.requestedPermissionsCount?.toString() ?: "0"
                AppInfoField.TARGET_SDK -> app.targetSdk?.toString() ?: ""
                AppInfoField.TOTAL_SIZE -> app.sizes.totalBytes.toString()
                AppInfoField.VERSION -> app.versionName ?: ""
            }
        return AppItemUiModel(
            packageName = app.packageName,
            appName = app.name,
            infoText = info,
        )
    }
}
