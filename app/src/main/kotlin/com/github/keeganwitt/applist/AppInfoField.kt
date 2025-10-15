package com.github.keeganwitt.applist

enum class AppInfoField(val titleResId: Int) {
    APK_SIZE(R.string.appInfoField_apkSize),
    APP_SIZE(R.string.appInfoField_appSize),
    ARCHIVED(R.string.appInfoField_archived),
    CACHE_SIZE(R.string.appInfoField_cacheSize),
    DATA_SIZE(R.string.appInfoField_dataSize),
    ENABLED(R.string.appInfoField_enabled),
    EXISTS_IN_APP_STORE(R.string.appInfoField_exists_in_app_store),
    EXTERNAL_CACHE_SIZE(R.string.appInfoField_externalCacheSize),
    FIRST_INSTALLED(R.string.appInfoField_firstInstalled),
    GRANTED_PERMISSIONS(R.string.appInfoField_grantedPermissions),
    LAST_UPDATED(R.string.appInfoField_lastUpdated),
    LAST_USED(R.string.appInfoField_lastUsed),
    MIN_SDK(R.string.appInfoField_minSdk),
    PACKAGE_MANAGER(R.string.appInfoField_packageManager),
    REQUESTED_PERMISSIONS(R.string.appInfoField_requestedPermissions),
    TARGET_SDK(R.string.appInfoField_targetSdk),
    TOTAL_SIZE(R.string.appInfoField_totalSize),
    VERSION(R.string.appInfoField_version)
}
