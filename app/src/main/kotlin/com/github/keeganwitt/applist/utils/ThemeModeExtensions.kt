package com.github.keeganwitt.applist.utils

import androidx.appcompat.app.AppCompatDelegate
import com.github.keeganwitt.applist.AppSettings

/**
 * Maps the [AppSettings.ThemeMode] to the corresponding [AppCompatDelegate] night mode constant.
 */
val AppSettings.ThemeMode.nightMode: Int
    get() =
        when (this) {
            AppSettings.ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppSettings.ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            AppSettings.ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
