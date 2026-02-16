package com.github.keeganwitt.applist

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.github.keeganwitt.applist.services.AndroidPackageService
import com.github.keeganwitt.applist.utils.PackageIconFetcher
import com.github.keeganwitt.applist.utils.PackageIconKeyer
import com.github.keeganwitt.applist.utils.nightMode

open class AppListApplication :
    Application(),
    ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        val appSettings = SharedPreferencesAppSettings(this)
        val crashReportingEnabled = appSettings.isCrashReportingEnabled()
        setCrashlyticsCollectionEnabled(crashReportingEnabled)
        if (!crashReportingEnabled) {
            deleteUnsentReports()
        }

        val themeMode = appSettings.getThemeMode()
        androidx.appcompat.app.AppCompatDelegate
            .setDefaultNightMode(themeMode.nightMode)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader
            .Builder(this)
            .components {
                add(PackageIconFetcher.Factory(AndroidPackageService(this@AppListApplication)))
                add(PackageIconKeyer())
            }.build()

    protected open fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("AppListApplication", "Setting crashlytics enabled: $enabled")
        }
        com.google.firebase.crashlytics.FirebaseCrashlytics
            .getInstance()
            .isCrashlyticsCollectionEnabled = enabled
    }

    protected open fun deleteUnsentReports() {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("AppListApplication", "Deleting unsent reports")
        }
        com.google.firebase.crashlytics.FirebaseCrashlytics
            .getInstance()
            .deleteUnsentReports()
    }
}
