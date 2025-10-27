package com.github.keeganwitt.applist

data class UiState(
    val selectedField: AppInfoField = AppInfoField.VERSION,
    val showSystem: Boolean = false,
    val descending: Boolean = false,
    val query: String = "",
    val isLoading: Boolean = false,
    val items: List<AppItemUiModel> = emptyList()
)
