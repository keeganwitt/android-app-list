package com.github.keeganwitt.applist.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.keeganwitt.applist.App
import com.github.keeganwitt.applist.AppInfoField
import com.github.keeganwitt.applist.StorageUsage

@Entity(tableName = "apps")
data class AppCacheEntity(
    @PrimaryKey val packageName: String,
    val name: String,
    val versionName: String?,
    val archived: Boolean?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val firstInstalled: Long?,
    val lastUpdated: Long?,
    val lastUsed: Long?,
    @Embedded(prefix = "storage_") val sizes: StorageUsage,
    val installerName: String?,
    val existsInStore: Boolean?,
    val grantedPermissionsCount: Int?,
    val requestedPermissionsCount: Int?,
    val enabled: Boolean,
    val isUserInstalled: Boolean,
    val hasLaunchIntent: Boolean,
    val isDetailed: Boolean,
    val failedFields: Set<AppInfoField>,
    val lastCachedAt: Long,
    val storeUrl: String? = null,
)

fun AppCacheEntity.toDomainModel(): App =
    App(
        packageName = packageName,
        name = name,
        versionName = versionName,
        archived = archived,
        minSdk = minSdk,
        targetSdk = targetSdk,
        firstInstalled = firstInstalled,
        lastUpdated = lastUpdated,
        lastUsed = lastUsed,
        sizes = sizes,
        installerName = installerName,
        existsInStore = existsInStore,
        grantedPermissionsCount = grantedPermissionsCount,
        requestedPermissionsCount = requestedPermissionsCount,
        enabled = enabled,
        isUserInstalled = isUserInstalled,
        hasLaunchIntent = hasLaunchIntent,
        isDetailed = isDetailed,
        failedFields = failedFields,
        storeUrl = storeUrl,
    )

fun App.toCacheEntity(lastCachedAt: Long): AppCacheEntity =
    AppCacheEntity(
        packageName = packageName,
        name = name,
        versionName = versionName,
        archived = archived,
        minSdk = minSdk,
        targetSdk = targetSdk,
        firstInstalled = firstInstalled,
        lastUpdated = lastUpdated,
        lastUsed = lastUsed,
        sizes = sizes,
        installerName = installerName,
        existsInStore = existsInStore,
        grantedPermissionsCount = grantedPermissionsCount,
        requestedPermissionsCount = requestedPermissionsCount,
        enabled = enabled,
        isUserInstalled = isUserInstalled,
        hasLaunchIntent = hasLaunchIntent,
        isDetailed = isDetailed,
        failedFields = failedFields,
        lastCachedAt = lastCachedAt,
        storeUrl = storeUrl,
    )
