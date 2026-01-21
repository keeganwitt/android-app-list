package com.github.keeganwitt.applist

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.github.keeganwitt.applist.services.AppStoreService
import com.github.keeganwitt.applist.services.PackageService
import com.github.keeganwitt.applist.services.StorageService
import com.github.keeganwitt.applist.services.UsageStatsService
import java.text.Collator

interface AppRepository {
    suspend fun loadApps(
        field: AppInfoField,
        showSystemApps: Boolean,
        descending: Boolean,
        reload: Boolean,
    ): List<App>
}

class AndroidAppRepository(
    private val packageService: PackageService,
    private val usageStatsService: UsageStatsService,
    private val storageService: StorageService,
    private val appStoreService: AppStoreService,
    private val crashReporter: CrashReporter? = null,
) : AppRepository {
    override suspend fun loadApps(
        field: AppInfoField,
        showSystemApps: Boolean,
        descending: Boolean,
        reload: Boolean,
    ): List<App> {
        var flags =
            (PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS)
                .toLong()
        if (Build.VERSION.SDK_INT >= 35) {
            flags = flags or PackageManager.MATCH_ARCHIVED_PACKAGES
        }
        val allInstalled = packageService.getInstalledApplications(flags)
        val filtered = filterApplications(allInstalled, showSystemApps)
        val lastUsedEpochs = usageStatsService.getLastUsedEpochs(reload)
        val apps = filtered.mapNotNull { mapToApp(it, lastUsedEpochs) }
        return sortApps(apps, field, descending)
    }

    private fun filterApplications(
        apps: List<ApplicationInfo>,
        showSystemApps: Boolean,
    ): List<ApplicationInfo> =
        apps.filter { ai ->
            val include = showSystemApps || isUserInstalledApp(ai)
            val archived = isArchived(ai) ?: false
            val hasLaunch = packageService.getLaunchIntentForPackage(ai.packageName) != null
            include && (archived || hasLaunch)
        }

    private fun mapToApp(
        ai: ApplicationInfo,
        lastUsedEpochs: Map<String, Long>,
    ): App? =
        try {
            val pkgInfo = packageService.getPackageInfo(ai)
            val storage = storageService.getStorageUsage(ai)
            val installerPackage = packageService.getInstallerPackageName(ai)
            val installerName = appStoreService.installerDisplayName(installerPackage)
            val existsInStore = appStoreService.existsInAppStore(ai.packageName ?: "", installerPackage)
            val flagsArr = pkgInfo.requestedPermissionsFlags
            val grantedCount =
                flagsArr?.count { flags -> (flags and PACKAGEINFO_REQUESTED_PERMISSION_GRANTED) != 0 }
                    ?: 0
            val requestedCount = pkgInfo.requestedPermissions?.size ?: 0

            App(
                packageName = ai.packageName ?: "",
                name = packageService.loadLabel(ai),
                versionName = pkgInfo.versionName,
                archived = isArchived(ai),
                minSdk = ai.minSdkVersion,
                targetSdk = ai.targetSdkVersion,
                firstInstalled = pkgInfo.firstInstallTime,
                lastUpdated = pkgInfo.lastUpdateTime,
                lastUsed = lastUsedEpochs[ai.packageName] ?: 0L,
                sizes = storage,
                installerName = installerName,
                existsInStore = existsInStore,
                grantedPermissionsCount = grantedCount,
                requestedPermissionsCount = requestedCount,
                enabled = ai.enabled,
            )
        } catch (e: Exception) {
            crashReporter?.recordException(e, "AndroidAppRepository.loadApps failed for ${ai.packageName}")
            null
        }

    private fun sortApps(
        apps: List<App>,
        field: AppInfoField,
        descending: Boolean,
    ): List<App> {
        val collator = Collator.getInstance()
        val comparator = compareBy<Pair<App, Comparable<*>?>> { it.second }.thenBy(collator) { it.first.name }
        val finalComparator = if (descending) comparator.reversed() else comparator

        return apps
            .map { app -> app to sortKey(app, field) }
            .sortedWith(finalComparator)
            .map { it.first }
    }

    private fun isUserInstalledApp(appInfo: ApplicationInfo): Boolean = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0

    private fun isArchived(applicationInfo: ApplicationInfo): Boolean? {
        if (Build.VERSION.SDK_INT >= 35 && applicationInfo.isArchived) {
            return true
        }
        return applicationInfo.metaData?.containsKey("com.android.vending.archive")
    }

    private fun sortKey(
        app: App,
        field: AppInfoField,
    ): Comparable<*>? =
        when (field) {
            AppInfoField.APK_SIZE -> app.sizes.apkBytes
            AppInfoField.APP_SIZE -> app.sizes.appBytes
            AppInfoField.CACHE_SIZE -> app.sizes.cacheBytes
            AppInfoField.DATA_SIZE -> app.sizes.dataBytes
            AppInfoField.ENABLED -> app.enabled.toString()
            AppInfoField.ARCHIVED -> app.archived ?: false
            AppInfoField.EXISTS_IN_APP_STORE -> app.existsInStore ?: false
            AppInfoField.EXTERNAL_CACHE_SIZE -> app.sizes.externalCacheBytes
            AppInfoField.FIRST_INSTALLED -> app.firstInstalled
            AppInfoField.LAST_UPDATED -> app.lastUpdated
            AppInfoField.LAST_USED -> app.lastUsed
            AppInfoField.MIN_SDK -> app.minSdk ?: 0
            AppInfoField.PACKAGE_MANAGER -> app.installerName ?: ""
            AppInfoField.GRANTED_PERMISSIONS -> app.grantedPermissionsCount ?: 0
            AppInfoField.REQUESTED_PERMISSIONS -> app.requestedPermissionsCount ?: 0
            AppInfoField.TARGET_SDK -> app.targetSdk ?: 0
            AppInfoField.TOTAL_SIZE -> app.sizes.totalBytes
            AppInfoField.VERSION -> app.versionName ?: ""
        }

    // Copy of Android's flag to avoid direct dependency on PackageInfo in signature
    private companion object {
        const val PACKAGEINFO_REQUESTED_PERMISSION_GRANTED: Int = 2
    }
}
