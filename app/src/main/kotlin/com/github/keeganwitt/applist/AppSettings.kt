package com.github.keeganwitt.applist

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

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
        PreferenceManager.getDefaultSharedPreferences(context)

    override fun isCrashReportingEnabled(): Boolean = preferences.getBoolean(AppSettings.KEY_CRASH_REPORTING_ENABLED, true)

    override fun setCrashReportingEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(AppSettings.KEY_CRASH_REPORTING_ENABLED, enabled) }
    }
}
