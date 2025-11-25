package com.github.keeganwitt.applist

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

interface AppSettings {
    fun isCrashReportingEnabled(): Boolean

    fun setCrashReportingEnabled(enabled: Boolean)

    fun getLastDisplayedAppInfoField(): AppInfoField

    fun setLastDisplayedAppInfoField(field: AppInfoField)

    fun getThemeMode(): ThemeMode

    fun setThemeMode(mode: ThemeMode)

    enum class ThemeMode {
        LIGHT,
        DARK,
        SYSTEM,
    }

    companion object {
        const val KEY_CRASH_REPORTING_ENABLED = "crash_reporting_enabled"
        const val KEY_LAST_DISPLAYED_APP_INFO_FIELD = "last_displayed_app_info_field"
        const val KEY_THEME_MODE = "theme_mode"
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

    override fun getLastDisplayedAppInfoField(): AppInfoField {
        val name = preferences.getString(AppSettings.KEY_LAST_DISPLAYED_APP_INFO_FIELD, null)
        return if (name != null) {
            try {
                AppInfoField.valueOf(name)
            } catch (e: IllegalArgumentException) {
                AppInfoField.VERSION
            }
        } else {
            AppInfoField.VERSION
        }
    }

    override fun setLastDisplayedAppInfoField(field: AppInfoField) {
        preferences.edit { putString(AppSettings.KEY_LAST_DISPLAYED_APP_INFO_FIELD, field.name) }
    }

    override fun getThemeMode(): AppSettings.ThemeMode {
        val mode = preferences.getString(AppSettings.KEY_THEME_MODE, null)
        return if (mode != null) {
            try {
                AppSettings.ThemeMode.valueOf(mode)
            } catch (e: IllegalArgumentException) {
                AppSettings.ThemeMode.SYSTEM
            }
        } else {
            AppSettings.ThemeMode.SYSTEM
        }
    }

    override fun setThemeMode(mode: AppSettings.ThemeMode) {
        preferences.edit { putString(AppSettings.KEY_THEME_MODE, mode.name) }
    }
}
