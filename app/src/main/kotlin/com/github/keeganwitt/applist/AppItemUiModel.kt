package com.github.keeganwitt.applist

data class AppItemUiModel(
    val packageName: String,
    val appName: String,
    val infoText: String,
    val packageNameLower: String = packageName.lowercase(),
    val appNameLower: String = appName.lowercase(),
    val infoTextLower: String = infoText.lowercase(),
)
