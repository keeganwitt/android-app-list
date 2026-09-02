package com.github.keeganwitt.applist

internal data class ExportAppItemUiModel(
    val packageName: String,
    val appName: String,
    val isUserInstalled: Boolean,
    val isArchived: Boolean,
    val isSelected: Boolean,
)

internal data class ExportRequest(
    val format: ExportFormat,
    val apps: List<App>,
)

internal data class ExportUiState(
    val format: ExportFormat = ExportFormat.XML,
    val showArchived: Boolean = false,
    val query: String = "",
    val appTypeFilter: ExportAppTypeFilter = ExportAppTypeFilter.USER,
    val visibleApps: List<ExportAppItemUiModel> = emptyList(),
    val selectedApps: List<ExportAppItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isExporting: Boolean = false,
    val isReviewingSelection: Boolean = false,
) {
    val selectedCount: Int
        get() = selectedApps.size

    val hiddenSelectedCount: Int
        get() = if (isReviewingSelection) 0 else selectedCount - visibleApps.count { it.isSelected }

    val canExport: Boolean
        get() = selectedApps.isNotEmpty() && !isLoading && !loadFailed && !isExporting
}
