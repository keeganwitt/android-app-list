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
