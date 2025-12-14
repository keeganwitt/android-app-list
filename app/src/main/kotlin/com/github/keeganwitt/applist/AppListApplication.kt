package com.github.keeganwitt.applist

import android.app.Application

open class AppListApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val appSettings = SharedPreferencesAppSettings(this)
        val crashReportingEnabled = appSettings.isCrashReportingEnabled()
        setCrashlyticsCollectionEnabled(crashReportingEnabled)
        if (!crashReportingEnabled) {
            deleteUnsentReports()
        }

        val themeMode = appSettings.getThemeMode()
        val nightMode =
            when (themeMode) {
                AppSettings.ThemeMode.LIGHT -> {
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                }

                AppSettings.ThemeMode.DARK -> {
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                }

                AppSettings.ThemeMode.SYSTEM -> {
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            }
        androidx.appcompat.app.AppCompatDelegate
            .setDefaultNightMode(nightMode)
    }

    protected open fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        android.util.Log.d("AppListApplication", "Setting crashlytics enabled: $enabled")
        com.google.firebase.crashlytics.FirebaseCrashlytics
            .getInstance()
            .isCrashlyticsCollectionEnabled = enabled
    }

    protected open fun deleteUnsentReports() {
        android.util.Log.d("AppListApplication", "Deleting unsent reports")
        com.google.firebase.crashlytics.FirebaseCrashlytics
            .getInstance()
            .deleteUnsentReports()
    }
}
