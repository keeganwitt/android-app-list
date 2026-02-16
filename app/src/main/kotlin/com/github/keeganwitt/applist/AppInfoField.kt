package com.github.keeganwitt.applist

import java.text.DateFormat
import java.util.Date

enum class AppInfoField(
    val titleResId: Int,
    val requiresUsageStats: Boolean = false,
) {
    APK_SIZE(R.string.appInfoField_apkSize) {
        override fun getValue(app: App) = app.sizes.apkBytes
    },
    APP_SIZE(R.string.appInfoField_appSize) {
        override fun getValue(app: App) = app.sizes.appBytes
    },
    ARCHIVED(R.string.appInfoField_archived) {
        override fun getValue(app: App) = app.archived ?: false

        override fun getFormattedValue(app: App) = (app.archived ?: false).toString()
    },
    CACHE_SIZE(R.string.appInfoField_cacheSize, requiresUsageStats = true) {
        override fun getValue(app: App) = app.sizes.cacheBytes
    },
    DATA_SIZE(R.string.appInfoField_dataSize, requiresUsageStats = true) {
        override fun getValue(app: App) = app.sizes.dataBytes
    },
    ENABLED(R.string.appInfoField_enabled) {
        override fun getValue(app: App) = app.enabled

        override fun getFormattedValue(app: App) = app.enabled.toString()
    },
    EXISTS_IN_APP_STORE(R.string.appInfoField_exists_in_app_store) {
        override fun getValue(app: App) = app.existsInStore ?: false

        override fun getFormattedValue(app: App) = (app.existsInStore ?: false).toString()
    },
    EXTERNAL_CACHE_SIZE(R.string.appInfoField_externalCacheSize, requiresUsageStats = true) {
        override fun getValue(app: App) = app.sizes.externalCacheBytes
    },
    FIRST_INSTALLED(R.string.appInfoField_firstInstalled) {
        override fun getValue(app: App) = app.firstInstalled

        override fun getFormattedValue(app: App) = app.firstInstalled?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: ""
    },
    GRANTED_PERMISSIONS(R.string.appInfoField_grantedPermissions) {
        override fun getValue(app: App) = app.grantedPermissionsCount ?: 0
    },
    LAST_UPDATED(R.string.appInfoField_lastUpdated) {
        override fun getValue(app: App) = app.lastUpdated

        override fun getFormattedValue(app: App) = app.lastUpdated?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: ""
    },
    LAST_USED(R.string.appInfoField_lastUsed, requiresUsageStats = true) {
        override fun getValue(app: App) = app.lastUsed

        override fun getFormattedValue(app: App) = app.lastUsed?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: ""
    },
    MIN_SDK(R.string.appInfoField_minSdk) {
        override fun getValue(app: App) = app.minSdk ?: 0

        override fun getFormattedValue(app: App) = app.minSdk?.toString() ?: ""
    },
    PACKAGE_MANAGER(R.string.appInfoField_packageManager) {
        override fun getValue(app: App) = app.installerName ?: ""
    },
    REQUESTED_PERMISSIONS(R.string.appInfoField_requestedPermissions) {
        override fun getValue(app: App) = app.requestedPermissionsCount ?: 0
    },
    TARGET_SDK(R.string.appInfoField_targetSdk) {
        override fun getValue(app: App) = app.targetSdk ?: 0

        override fun getFormattedValue(app: App) = app.targetSdk?.toString() ?: ""
    },
    TOTAL_SIZE(R.string.appInfoField_totalSize, requiresUsageStats = true) {
        override fun getValue(app: App) = app.sizes.totalBytes
    },
    VERSION(R.string.appInfoField_version) {
        override fun getValue(app: App) = app.versionName ?: ""
    },
    ;

    abstract fun getValue(app: App): Comparable<*>?

    open fun getFormattedValue(app: App): String = getValue(app)?.toString() ?: ""
}
