package com.github.keeganwitt.applist

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.github.keeganwitt.applist.services.AppStoreService
import com.github.keeganwitt.applist.services.PackageService
import com.github.keeganwitt.applist.services.StorageService
import com.github.keeganwitt.applist.services.UsageStatsService
import com.github.keeganwitt.applist.utils.isArchivedApp
import com.github.keeganwitt.applist.utils.isUserInstalled
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
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
    private val cacheMutex = Mutex()
    private var cachedRawApps: List<ApplicationInfo>? = null
    private var cachedLaunchablePackages: Set<String>? = null
    private var cachedShowSystemApps: Boolean? = null
    private var cachedApps: List<App>? = null

    override fun loadApps(
        field: AppInfoField,
        showSystemApps: Boolean,
        descending: Boolean,
        reload: Boolean,
    ): Flow<List<App>> =
        flow {
            val (allInstalled, launchablePackages, cachedFullApps) =
                cacheMutex.withLock {
                    if (!reload && cachedShowSystemApps == showSystemApps && cachedRawApps != null && cachedLaunchablePackages != null) {
                        Triple(cachedRawApps!!, cachedLaunchablePackages!!, cachedApps)
                    } else {
                        var flags =
                            (PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS)
                                .toLong()
                        if (Build.VERSION.SDK_INT >= 35) {
                            flags = flags or PackageManager.MATCH_ARCHIVED_PACKAGES
                        }
                        val installed = packageService.getInstalledApplications(flags)
                        val launchable = packageService.getLaunchablePackages()
                        cachedRawApps = installed
                        cachedLaunchablePackages = launchable
                        cachedShowSystemApps = showSystemApps
                        cachedApps = null
                        Triple(installed, launchable, null)
                    }
                }

            // Phase 1: Filter and map basic info in one pass (fast)
            val filteredWithBasic =
                allInstalled.mapNotNull { ai ->
                    val archived = ai.isArchivedApp
                    val isUserInstalled = ai.isUserInstalled
                    val hasLaunch = launchablePackages.contains(ai.packageName)
                    if ((showSystemApps || isUserInstalled) && (archived || hasLaunch)) {
                        ai to mapToAppBasic(ai, archived)
                    } else {
                        null
                    }
                }
            val basicApps = filteredWithBasic.map { it.second }
            val sortedBasicApps = sortApps(basicApps, field, descending)
            emit(sortedBasicApps)

            // Phase 2: Fetch heavy data and emit updated list
            val sortedDetailedApps =
                if (cachedFullApps != null) {
                    sortApps(cachedFullApps, field, descending)
                } else {
                    val lastUsedEpochs = usageStatsService.getLastUsedEpochs(reload)
                    val semaphore = Semaphore(MAX_CONCURRENCY)

                    val detailedApps =
                        coroutineScope {
                            val appsDeferred =
                                filteredWithBasic.map { (ai, basicApp) ->
                                    async {
                                        semaphore.withPermit {
                                            mapToAppDetailed(ai, basicApp, lastUsedEpochs)
                                        }
                                    }
                                }
                            appsDeferred.awaitAll()
                        }
                    cacheMutex.withLock {
                        cachedApps = detailedApps
                    }
                    sortApps(detailedApps, field, descending)
                }

            emit(sortedDetailedApps)
        }

    private fun mapToAppBasic(ai: ApplicationInfo, archived: Boolean): App {
        val packageName = ai.packageName ?: ""
        return App(
            packageName = packageName,
            name = packageService.loadLabel(ai),
            versionName = "", // Load lazily/later if slow, or now if fast. PackageInfo might be slow?
            archived = archived,
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
    ): App =
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

            basicApp.copy(
                versionName = pkgInfo.versionName,
                firstInstalled = pkgInfo.firstInstallTime,
                lastUpdated = pkgInfo.lastUpdateTime,
                lastUsed = lastUsedEpochs[ai.packageName] ?: 0L,
                sizes = storage,
                installerName = installerName,
                existsInStore = existsInStore,
                grantedPermissionsCount = grantedCount,
                requestedPermissionsCount = requestedCount,
            )
        } catch (e: Exception) {
            crashReporter?.recordException(e, "AndroidAppRepository.loadApps failed for ${ai.packageName}")
            basicApp
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
