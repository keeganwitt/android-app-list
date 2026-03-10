package com.github.keeganwitt.applist

import android.content.Context
import com.github.keeganwitt.applist.services.AppStoreService
import java.text.DateFormat
import java.util.Date
import java.util.Locale

enum class SummaryType {
    BOOLEAN,
    SIZE,
    SDK,
    PERMISSION,
    DATE,
    PACKAGE_MANAGER,
    NONE,
}

enum class AppInfoField(
    val titleResId: Int,
    val summaryType: SummaryType = SummaryType.NONE,
) {
    APK_SIZE(R.string.appInfoField_apkSize, summaryType = SummaryType.SIZE) {
        override fun getValue(app: App) = app.sizes.apkBytes
    },
    APP_SIZE(R.string.appInfoField_appSize, summaryType = SummaryType.SIZE) {
        override fun getValue(app: App) = app.sizes.appBytes
    },
    ARCHIVED(R.string.appInfoField_archived, summaryType = SummaryType.BOOLEAN) {
        override fun getValue(app: App) = app.archived ?: false

        override fun getSummaryKey(
            app: App,
            context: Context,
            appStoreService: AppStoreService,
        ) = if (app.archived == true) context.getString(R.string.archived) else context.getString(R.string.installed)
    },
    CACHE_SIZE(R.string.appInfoField_cacheSize, summaryType = SummaryType.SIZE) {
        override fun getValue(app: App) = app.sizes.cacheBytes
    },
    DATA_SIZE(R.string.appInfoField_dataSize, summaryType = SummaryType.SIZE) {
        override fun getValue(app: App) = app.sizes.dataBytes
    },
    ENABLED(R.string.appInfoField_enabled, summaryType = SummaryType.BOOLEAN) {
        override fun getValue(app: App) = app.enabled

        override fun getSummaryKey(
            app: App,
            context: Context,
            appStoreService: AppStoreService,
        ) = if (app.enabled) context.getString(R.string.enabled) else context.getString(R.string.disabled)
    },
    EXISTS_IN_APP_STORE(R.string.appInfoField_exists_in_app_store, summaryType = SummaryType.BOOLEAN) {
        override fun getValue(app: App) = app.existsInStore ?: false

        override fun getSummaryKey(
            app: App,
            context: Context,
            appStoreService: AppStoreService,
        ) = if (app.existsInStore == true) context.getString(R.string.boolean_true) else context.getString(R.string.boolean_false)
    },
    EXTERNAL_CACHE_SIZE(R.string.appInfoField_externalCacheSize, summaryType = SummaryType.SIZE) {
        override fun getValue(app: App) = app.sizes.externalCacheBytes
    },
    FIRST_INSTALLED(R.string.appInfoField_firstInstalled, summaryType = SummaryType.DATE) {
        override fun getValue(app: App) = app.firstInstalled
    },
    GRANTED_PERMISSIONS(R.string.appInfoField_grantedPermissions, summaryType = SummaryType.PERMISSION) {
        override fun getValue(app: App) = app.grantedPermissionsCount ?: 0
    },
    LAST_UPDATED(R.string.appInfoField_lastUpdated, summaryType = SummaryType.DATE) {
        override fun getValue(app: App) = app.lastUpdated
    },
    LAST_USED(R.string.appInfoField_lastUsed, summaryType = SummaryType.DATE) {
        override fun getValue(app: App) = app.lastUsed
    },
    MIN_SDK(R.string.appInfoField_minSdk, summaryType = SummaryType.SDK) {
        override fun getValue(app: App) = app.minSdk ?: 0
    },
    PACKAGE_MANAGER(R.string.appInfoField_packageManager, summaryType = SummaryType.PACKAGE_MANAGER) {
        override fun getValue(app: App) = app.installerName ?: ""

        override fun getSummaryKey(
            app: App,
            context: Context,
            appStoreService: AppStoreService,
        ) = appStoreService.installerDisplayName(app.installerName)
    },
    REQUESTED_PERMISSIONS(R.string.appInfoField_requestedPermissions, summaryType = SummaryType.PERMISSION) {
        override fun getValue(app: App) = app.requestedPermissionsCount ?: 0
    },
    TARGET_SDK(R.string.appInfoField_targetSdk, summaryType = SummaryType.SDK) {
        override fun getValue(app: App) = app.targetSdk ?: 0
    },
    TOTAL_SIZE(R.string.appInfoField_totalSize, summaryType = SummaryType.SIZE) {
        override fun getValue(app: App) = app.sizes.totalBytes
    },
    VERSION(R.string.appInfoField_version) {
        override fun getValue(app: App) = app.versionName ?: ""
    },
    ;

    val requiresUsageStats: Boolean
        get() = summaryType == SummaryType.DATE || summaryType == SummaryType.SIZE

    abstract fun getValue(app: App): Comparable<*>?

    open fun getSummaryKey(
        app: App,
        context: Context,
        appStoreService: AppStoreService,
    ): String? = null

    open fun getFormattedValue(app: App): String {
        val value = getValue(app)
        return if (summaryType == SummaryType.DATE) {
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
