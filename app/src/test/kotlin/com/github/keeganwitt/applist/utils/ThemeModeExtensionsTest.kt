package com.github.keeganwitt.applist.utils

import androidx.appcompat.app.AppCompatDelegate
import com.github.keeganwitt.applist.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeExtensionsTest {
    @Test
    fun `nightMode returns MODE_NIGHT_NO for LIGHT theme`() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppSettings.ThemeMode.LIGHT.nightMode)
    }

    @Test
    fun `nightMode returns MODE_NIGHT_YES for DARK theme`() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppSettings.ThemeMode.DARK.nightMode)
    }

    @Test
    fun `nightMode returns MODE_NIGHT_FOLLOW_SYSTEM for SYSTEM theme`() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppSettings.ThemeMode.SYSTEM.nightMode)
    }
}
