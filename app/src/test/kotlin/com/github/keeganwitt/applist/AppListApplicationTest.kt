package com.github.keeganwitt.applist

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.keeganwitt.applist.utils.PackageIconFetcher
import com.github.keeganwitt.applist.utils.PackageIconKeyer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppListApplicationTest {
    private lateinit var context: Context
    private lateinit var appSettings: SharedPreferencesAppSettings

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .clear()
            .commit()
        appSettings = SharedPreferencesAppSettings(context)
    }

    @Test
    fun `given crash reporting enabled when onCreate called then crashlytics is enabled`() {
        appSettings.setCrashReportingEnabled(true)
        var enabledCalled = false
        var deleteCalled = false

        val application =
            object : AppListApplication() {
                init {
                    attachBaseContext(context)
                }

                override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
                    if (enabled) enabledCalled = true
                }

                override fun deleteUnsentReports() {
                    deleteCalled = true
                }
            }
        application.onCreate()

        assertTrue(enabledCalled)
        assertFalse(deleteCalled)
    }

    @Test
    fun `given crash reporting disabled when onCreate called then crashlytics is disabled and reports deleted`() {
        appSettings.setCrashReportingEnabled(false)
        var disabledCalled = false
        var deleteCalled = false

        val application =
            object : AppListApplication() {
                init {
                    attachBaseContext(context)
                }

                override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
                    if (!enabled) disabledCalled = true
                }

                override fun deleteUnsentReports() {
                    deleteCalled = true
                }
            }
        application.onCreate()

        assertTrue(disabledCalled)
        assertTrue(deleteCalled)
    }

    @Test
    fun `given theme mode is light when onCreate called then default night mode is NO`() {
        appSettings.setThemeMode(AppSettings.ThemeMode.LIGHT)

        val application =
            object : AppListApplication() {
                init {
                    attachBaseContext(context)
                }

                override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {}

                override fun deleteUnsentReports() {}
            }
        application.onCreate()

        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun `given theme mode is dark when onCreate called then default night mode is YES`() {
        appSettings.setThemeMode(AppSettings.ThemeMode.DARK)

        val application =
            object : AppListApplication() {
                init {
                    attachBaseContext(context)
                }

                override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {}

                override fun deleteUnsentReports() {}
            }
        application.onCreate()

        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun `given theme mode is system when onCreate called then default night mode is FOLLOW_SYSTEM`() {
        appSettings.setThemeMode(AppSettings.ThemeMode.SYSTEM)

        val application =
            object : AppListApplication() {
                init {
                    attachBaseContext(context)
                }

                override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {}

                override fun deleteUnsentReports() {}
            }
        application.onCreate()

        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun `when newImageLoader called then it contains expected components`() {
        val application =
            object : AppListApplication() {
                init {
                    attachBaseContext(context)
                }
            }
        val imageLoader = application.newImageLoader()

        val fetcherFactories = imageLoader.components.fetcherFactories
        assertTrue(fetcherFactories.any { it.first is PackageIconFetcher.Factory })

        val keyers = imageLoader.components.keyers
        assertTrue(keyers.any { it.first is PackageIconKeyer })
    }
}
