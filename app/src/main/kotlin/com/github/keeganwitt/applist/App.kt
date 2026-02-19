package com.github.keeganwitt.applist

data class App(
    val packageName: String,
    val name: String,
    val versionName: String?,
    val archived: Boolean?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val firstInstalled: Long?,
    val lastUpdated: Long?,
    val lastUsed: Long?,
    val sizes: StorageUsage,
    val installerName: String?,
    val existsInStore: Boolean?,
    val grantedPermissionsCount: Int?,
    val requestedPermissionsCount: Int?,
    val enabled: Boolean,
    val isDetailed: Boolean = false,
)
