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
}
