package com.github.keeganwitt.applist

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = TestAppListApplication::class)
class SharedPreferencesAppSettingsTest {
    private lateinit var appSettings: SharedPreferencesAppSettings

    @Before
    fun `set up`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .clear()
            .commit()
        appSettings = SharedPreferencesAppSettings(context)
    }

    @Test
    fun `isCrashReportingEnabled returns true by default`() {
        assertTrue(appSettings.isCrashReportingEnabled())
    }

    @Test
    fun `setCrashReportingEnabled correctly updates the value`() {
        appSettings.setCrashReportingEnabled(false)
        assertFalse(appSettings.isCrashReportingEnabled())

        appSettings.setCrashReportingEnabled(true)
        assertTrue(appSettings.isCrashReportingEnabled())
    }

    @Test
    fun `getLastDisplayedAppInfoField returns VERSION by default`() {
        val field = appSettings.getLastDisplayedAppInfoField()
        assertTrue(field == AppInfoField.VERSION)
    }

    @Test
    fun `setLastDisplayedAppInfoField correctly updates the value`() {
        appSettings.setLastDisplayedAppInfoField(AppInfoField.APP_SIZE)
        assertTrue(appSettings.getLastDisplayedAppInfoField() == AppInfoField.APP_SIZE)

        appSettings.setLastDisplayedAppInfoField(AppInfoField.LAST_USED)
        assertTrue(appSettings.getLastDisplayedAppInfoField() == AppInfoField.LAST_USED)
    }

    @Test
    fun `getThemeMode returns SYSTEM by default`() {
        val mode = appSettings.getThemeMode()
        assertTrue(mode == AppSettings.ThemeMode.SYSTEM)
    }

    @Test
    fun `setThemeMode correctly updates the value`() {
        appSettings.setThemeMode(AppSettings.ThemeMode.LIGHT)
        assertTrue(appSettings.getThemeMode() == AppSettings.ThemeMode.LIGHT)

        appSettings.setThemeMode(AppSettings.ThemeMode.DARK)
        assertTrue(appSettings.getThemeMode() == AppSettings.ThemeMode.DARK)
    }

    @Test
    fun `getLastDisplayedAppInfoField returns VERSION when value is invalid`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putString(AppSettings.KEY_LAST_DISPLAYED_APP_INFO_FIELD, "INVALID_VALUE")
            .commit()

        val field = appSettings.getLastDisplayedAppInfoField()
        assertTrue(field == AppInfoField.VERSION)
    }
}
