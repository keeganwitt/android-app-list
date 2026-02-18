package com.github.keeganwitt.applist

import java.text.DateFormat
import java.util.Date

enum class AppInfoField(
    val titleResId: Int,
    val requiresUsageStats: Boolean = false,
    val isBasic: Boolean = false,
) {
    APK_SIZE(R.string.appInfoField_apkSize) {
        override fun getValue(app: App) = app.sizes.apkBytes
    },
    APP_SIZE(R.string.appInfoField_appSize) {
        override fun getValue(app: App) = app.sizes.appBytes
    },
    ARCHIVED(R.string.appInfoField_archived, isBasic = true) {
        override fun getValue(app: App) = app.archived ?: false
    },
    CACHE_SIZE(R.string.appInfoField_cacheSize, requiresUsageStats = true) {
        override fun getValue(app: App) = app.sizes.cacheBytes
    },
    DATA_SIZE(R.string.appInfoField_dataSize, requiresUsageStats = true) {
        override fun getValue(app: App) = app.sizes.dataBytes
    },
    ENABLED(R.string.appInfoField_enabled, isBasic = true) {
        override fun getValue(app: App) = app.enabled
    },
    EXISTS_IN_APP_STORE(R.string.appInfoField_exists_in_app_store) {
        override fun getValue(app: App) = app.existsInStore ?: false
    },
    EXTERNAL_CACHE_SIZE(R.string.appInfoField_externalCacheSize, requiresUsageStats = true) {
        override fun getValue(app: App) = app.sizes.externalCacheBytes
    },
    FIRST_INSTALLED(R.string.appInfoField_firstInstalled) {
        override fun getValue(app: App) = app.firstInstalled

        override fun getFormattedValue(app: App) = formatDate(app.firstInstalled)
    },
    GRANTED_PERMISSIONS(R.string.appInfoField_grantedPermissions) {
        override fun getValue(app: App) = app.grantedPermissionsCount ?: 0
    },
    LAST_UPDATED(R.string.appInfoField_lastUpdated) {
        override fun getValue(app: App) = app.lastUpdated

        override fun getFormattedValue(app: App) = formatDate(app.lastUpdated)
    },
    LAST_USED(R.string.appInfoField_lastUsed, requiresUsageStats = true) {
        override fun getValue(app: App) = app.lastUsed

        override fun getFormattedValue(app: App) = formatDate(app.lastUsed)
    },
    MIN_SDK(R.string.appInfoField_minSdk, isBasic = true) {
        override fun getValue(app: App) = app.minSdk ?: 0
    },
    PACKAGE_MANAGER(R.string.appInfoField_packageManager) {
        override fun getValue(app: App) = app.installerName ?: ""
    },
    REQUESTED_PERMISSIONS(R.string.appInfoField_requestedPermissions) {
        override fun getValue(app: App) = app.requestedPermissionsCount ?: 0
    },
    TARGET_SDK(R.string.appInfoField_targetSdk, isBasic = true) {
        override fun getValue(app: App) = app.targetSdk ?: 0
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

    protected fun formatDate(timestamp: Long?): String {
        return timestamp?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: ""
    }
}
