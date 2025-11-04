package com.github.keeganwitt.applist

data class AppItemUiModel(
    val packageName: String,
    val appName: String,
    val infoText: String,
    val icon: android.graphics.drawable.Drawable? = null,
)
