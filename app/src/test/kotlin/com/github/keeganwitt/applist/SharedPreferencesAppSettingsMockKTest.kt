package com.github.keeganwitt.applist

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SharedPreferencesAppSettingsMockKTest {
    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var appSettings: SharedPreferencesAppSettings

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        preferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.packageName } returns "com.github.keeganwitt.applist"
        every {
            context.getSharedPreferences(any(), any())
        } returns preferences
        every { preferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor

        appSettings = SharedPreferencesAppSettings(context)
    }

    @Test
    fun `isCrashReportingEnabled returns true by default`() {
        every { preferences.getBoolean(AppSettings.KEY_CRASH_REPORTING_ENABLED, true) } returns true
        assertTrue(appSettings.isCrashReportingEnabled())
    }

    @Test
    fun `setCrashReportingEnabled updates preference`() {
        appSettings.setCrashReportingEnabled(false)
        verify { editor.putBoolean(AppSettings.KEY_CRASH_REPORTING_ENABLED, false) }
        verify { editor.apply() }
    }

    @Test
    fun `getLastDisplayedAppInfoField returns VERSION by default`() {
        every { preferences.getString(AppSettings.KEY_LAST_DISPLAYED_APP_INFO_FIELD, null) } returns null
        assertEquals(AppInfoField.VERSION, appSettings.getLastDisplayedAppInfoField())
    }

    @Test
    fun `getLastDisplayedAppInfoField returns stored field`() {
        every { preferences.getString(AppSettings.KEY_LAST_DISPLAYED_APP_INFO_FIELD, null) } returns AppInfoField.APP_SIZE.name
        assertEquals(AppInfoField.APP_SIZE, appSettings.getLastDisplayedAppInfoField())
    }

    @Test
    fun `getLastDisplayedAppInfoField returns VERSION when stored field is invalid`() {
        every { preferences.getString(AppSettings.KEY_LAST_DISPLAYED_APP_INFO_FIELD, null) } returns "INVALID_FIELD"
        assertEquals(AppInfoField.VERSION, appSettings.getLastDisplayedAppInfoField())
    }

    @Test
    fun `getLastDisplayedAppInfoField returns VERSION when stored field is empty`() {
        every { preferences.getString(AppSettings.KEY_LAST_DISPLAYED_APP_INFO_FIELD, null) } returns ""
        assertEquals(AppInfoField.VERSION, appSettings.getLastDisplayedAppInfoField())
    }

    @Test
    fun `setLastDisplayedAppInfoField updates preference`() {
        appSettings.setLastDisplayedAppInfoField(AppInfoField.APK_SIZE)
        verify { editor.putString(AppSettings.KEY_LAST_DISPLAYED_APP_INFO_FIELD, AppInfoField.APK_SIZE.name) }
        verify { editor.apply() }
    }

    @Test
    fun `getThemeMode returns SYSTEM by default`() {
        every { preferences.getString(AppSettings.KEY_THEME_MODE, null) } returns null
        assertEquals(AppSettings.ThemeMode.SYSTEM, appSettings.getThemeMode())
    }

    @Test
    fun `getThemeMode returns stored mode`() {
        every { preferences.getString(AppSettings.KEY_THEME_MODE, null) } returns AppSettings.ThemeMode.DARK.name
        assertEquals(AppSettings.ThemeMode.DARK, appSettings.getThemeMode())
    }

    @Test
    fun `getThemeMode returns SYSTEM when stored mode is invalid`() {
        every { preferences.getString(AppSettings.KEY_THEME_MODE, null) } returns "INVALID_MODE"
        assertEquals(AppSettings.ThemeMode.SYSTEM, appSettings.getThemeMode())
    }

    @Test
    fun `getThemeMode returns SYSTEM when stored mode is empty`() {
        every { preferences.getString(AppSettings.KEY_THEME_MODE, null) } returns ""
        assertEquals(AppSettings.ThemeMode.SYSTEM, appSettings.getThemeMode())
    }

    @Test
    fun `setThemeMode updates preference`() {
        appSettings.setThemeMode(AppSettings.ThemeMode.LIGHT)
        verify { editor.putString(AppSettings.KEY_THEME_MODE, AppSettings.ThemeMode.LIGHT.name) }
        verify { editor.apply() }
    }

    @Test
    fun `isIncludeUsageStatsInExportEnabled returns false by default`() {
        every { preferences.getBoolean(AppSettings.KEY_INCLUDE_USAGE_STATS_IN_EXPORT, false) } returns false
        assertFalse(appSettings.isIncludeUsageStatsInExportEnabled())
    }

    @Test
    fun `setIncludeUsageStatsInExportEnabled updates preference`() {
        appSettings.setIncludeUsageStatsInExportEnabled(true)
        verify { editor.putBoolean(AppSettings.KEY_INCLUDE_USAGE_STATS_IN_EXPORT, true) }
        verify { editor.apply() }
    }
}
