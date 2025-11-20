package com.github.keeganwitt.applist

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics

class AppListApplication : Application() {
    private lateinit var appSettings: AppSettings

    override fun onCreate() {
        super.onCreate()
        appSettings = SharedPreferencesAppSettings(this)
        val crashReportingEnabled = appSettings.isCrashReportingEnabled()
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = crashReportingEnabled
        if (!crashReportingEnabled) {
            FirebaseCrashlytics.getInstance().deleteUnsentReports()
        }
    }
}
