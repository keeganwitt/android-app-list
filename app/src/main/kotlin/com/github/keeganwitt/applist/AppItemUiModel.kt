package com.github.keeganwitt.applist

data class AppItemUiModel(
    val packageName: String,
    val appName: String,
    val infoText: String,
    val isFinalValue: Boolean = true,
)
