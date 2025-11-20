package com.github.keeganwitt.applist

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

interface AppSettings {
    fun isCrashReportingEnabled(): Boolean

    fun setCrashReportingEnabled(enabled: Boolean)

    companion object {
        const val KEY_CRASH_REPORTING_ENABLED = "crash_reporting_enabled"
    }
}

class SharedPreferencesAppSettings(
    context: Context,
) : AppSettings {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(APP_SETTINGS_NAME, Context.MODE_PRIVATE)

    override fun isCrashReportingEnabled(): Boolean = preferences.getBoolean(AppSettings.KEY_CRASH_REPORTING_ENABLED, true)

    override fun setCrashReportingEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(AppSettings.KEY_CRASH_REPORTING_ENABLED, enabled) }
    }

    companion object {
        const val APP_SETTINGS_NAME = "app_settings"
    }
}
