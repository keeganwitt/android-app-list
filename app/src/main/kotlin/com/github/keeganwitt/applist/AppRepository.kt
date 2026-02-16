package com.github.keeganwitt.applist

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.github.keeganwitt.applist.services.AppStoreService
import com.github.keeganwitt.applist.services.PackageService
import com.github.keeganwitt.applist.services.StorageService
import com.github.keeganwitt.applist.services.UsageStatsService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.text.Collator

interface AppRepository {
    fun loadApps(
        field: AppInfoField,
        showSystemApps: Boolean,
        descending: Boolean,
        reload: Boolean,
    ): Flow<List<App>>
}

class AndroidAppRepository(
    private val packageService: PackageService,
    private val usageStatsService: UsageStatsService,
    private val storageService: StorageService,
    private val appStoreService: AppStoreService,
    private val crashReporter: CrashReporter? = null,
) : AppRepository {
    override fun loadApps(
        field: AppInfoField,
        showSystemApps: Boolean,
        descending: Boolean,
        reload: Boolean,
    ): Flow<List<App>> =
        flow {
            var flags =
                (PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS)
                    .toLong()
            if (Build.VERSION.SDK_INT >= 35) {
                flags = flags or PackageManager.MATCH_ARCHIVED_PACKAGES
            }
            val allInstalled = packageService.getInstalledApplications(flags)
            val launchablePackages = packageService.getLaunchablePackages()
            val filtered = filterApplications(allInstalled, launchablePackages, showSystemApps)

            // Phase 1: Emit apps with basic info only (fast)
            val basicApps = filtered.map { ai -> mapToAppBasic(ai) }
            val sortedBasicApps = sortApps(basicApps, field, descending)
            emit(sortedBasicApps)

            // Phase 2: Fetch heavy data and emit updated list
            val lastUsedEpochs = usageStatsService.getLastUsedEpochs(reload)
            val semaphore = Semaphore(MAX_CONCURRENCY)

            val detailedApps =
                coroutineScope {
                    val appsDeferred =
                        filtered.zip(basicApps).map { (ai, basicApp) ->
                            async {
                                semaphore.withPermit {
                                    mapToAppDetailed(ai, basicApp, lastUsedEpochs)
                                }
                            }
                        }
                    appsDeferred.awaitAll().filterNotNull()
                }

            val sortedDetailedApps = sortApps(detailedApps, field, descending)
            emit(sortedDetailedApps)
        }

    private fun mapToAppBasic(ai: ApplicationInfo): App {
        val packageName = ai.packageName ?: ""
        return App(
            packageName = packageName,
            name = packageService.loadLabel(ai),
            versionName = "", // Load lazily/later if slow, or now if fast. PackageInfo might be slow?
            archived = isArchived(ai) ?: false,
            minSdk = ai.minSdkVersion,
            targetSdk = ai.targetSdkVersion,
            firstInstalled = 0,
            lastUpdated = 0,
            lastUsed = 0,
            sizes = StorageUsage(), // Empty for now
            installerName = "",
            existsInStore = false,
            grantedPermissionsCount = 0,
            requestedPermissionsCount = 0,
            enabled = ai.enabled,
        )
    }

    private fun mapToAppDetailed(
        ai: ApplicationInfo,
        basicApp: App,
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
                name = basicApp.name,
                versionName = pkgInfo.versionName,
                archived = basicApp.archived,
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
            mapToAppBasic(ai)
        }

    private fun filterApplications(
        apps: List<ApplicationInfo>,
        launchablePackages: Set<String>,
        showSystemApps: Boolean,
    ): List<ApplicationInfo> =
        apps.filter { ai ->
            val include = showSystemApps || isUserInstalledApp(ai)
            val archived = isArchived(ai) ?: false
            val hasLaunch = launchablePackages.contains(ai.packageName)
            include && (archived || hasLaunch)
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
    ): Comparable<*>? = field.getValue(app)

    // Copy of Android's flag to avoid direct dependency on PackageInfo in signature
    private companion object {
        const val PACKAGEINFO_REQUESTED_PERMISSION_GRANTED: Int = 2
        const val MAX_CONCURRENCY = 4
    }
}
