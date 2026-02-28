package com.github.keeganwitt.applist

import java.text.DateFormat
import java.util.Date
import java.util.Locale

enum class AppInfoField(
    val titleResId: Int,
    val requiresUsageStats: Boolean = false,
    val isSize: Boolean = false,
    private val isDate: Boolean = false,
) {
    APK_SIZE(R.string.appInfoField_apkSize, isSize = true) {
        override fun getValue(app: App) = app.sizes.apkBytes
    },
    APP_SIZE(R.string.appInfoField_appSize, requiresUsageStats = true, isSize = true) {
        override fun getValue(app: App) = app.sizes.appBytes
    },
    ARCHIVED(R.string.appInfoField_archived) {
        override fun getValue(app: App) = app.archived ?: false
    },
    CACHE_SIZE(R.string.appInfoField_cacheSize, requiresUsageStats = true, isSize = true) {
        override fun getValue(app: App) = app.sizes.cacheBytes
    },
    DATA_SIZE(R.string.appInfoField_dataSize, requiresUsageStats = true, isSize = true) {
        override fun getValue(app: App) = app.sizes.dataBytes
    },
    ENABLED(R.string.appInfoField_enabled) {
        override fun getValue(app: App) = app.enabled
    },
    EXISTS_IN_APP_STORE(R.string.appInfoField_exists_in_app_store) {
        override fun getValue(app: App) = app.existsInStore ?: false
    },
    EXTERNAL_CACHE_SIZE(R.string.appInfoField_externalCacheSize, requiresUsageStats = true, isSize = true) {
        override fun getValue(app: App) = app.sizes.externalCacheBytes
    },
    FIRST_INSTALLED(R.string.appInfoField_firstInstalled, isDate = true) {
        override fun getValue(app: App) = app.firstInstalled
    },
    GRANTED_PERMISSIONS(R.string.appInfoField_grantedPermissions) {
        override fun getValue(app: App) = app.grantedPermissionsCount ?: 0
    },
    LAST_UPDATED(R.string.appInfoField_lastUpdated, isDate = true) {
        override fun getValue(app: App) = app.lastUpdated
    },
    LAST_USED(R.string.appInfoField_lastUsed, requiresUsageStats = true, isDate = true) {
        override fun getValue(app: App) = app.lastUsed
    },
    MIN_SDK(R.string.appInfoField_minSdk) {
        override fun getValue(app: App) = app.minSdk ?: 0
    },
    PACKAGE_MANAGER(R.string.appInfoField_packageManager) {
        override fun getValue(app: App) = app.installerName ?: ""
    },
    REQUESTED_PERMISSIONS(R.string.appInfoField_requestedPermissions) {
        override fun getValue(app: App) = app.requestedPermissionsCount ?: 0
    },
    TARGET_SDK(R.string.appInfoField_targetSdk) {
        override fun getValue(app: App) = app.targetSdk ?: 0
    },
    TOTAL_SIZE(R.string.appInfoField_totalSize, requiresUsageStats = true, isSize = true) {
        override fun getValue(app: App) = app.sizes.totalBytes
    },
    VERSION(R.string.appInfoField_version) {
        override fun getValue(app: App) = app.versionName ?: ""
    },
    ;

    abstract fun getValue(app: App): Comparable<*>?

    open fun getFormattedValue(app: App): String {
        val value = getValue(app)
        return if (isDate) {
            formatDate(value as? Long)
        } else {
            value?.toString() ?: ""
        }
    }

    protected fun formatDate(timestamp: Long?): String = timestamp?.let {
        val currentLocale = Locale.getDefault()
        var cache = dateFormatCache.get()!!
        if (cache.locale != currentLocale) {
            cache = DateFormatCache(currentLocale, DateFormat.getDateTimeInstance())
            dateFormatCache.set(cache)
        }
        cache.dateFormat.format(Date(it))
    } ?: ""

    private class DateFormatCache(val locale: Locale, val dateFormat: DateFormat)

    companion object {
        private val dateFormatCache = object : ThreadLocal<DateFormatCache>() {
            override fun initialValue(): DateFormatCache {
                val locale = Locale.getDefault()
                return DateFormatCache(locale, DateFormat.getDateTimeInstance())
            }
        }
    }
}
