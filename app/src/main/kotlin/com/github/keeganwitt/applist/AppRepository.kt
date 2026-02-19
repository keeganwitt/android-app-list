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
            val filteredWithBasic = getFilteredApps(showSystemApps)
            val basicApps = filteredWithBasic.map { it.second }
            emit(sortApps(basicApps, field, descending))

            val lastUsedEpochs = usageStatsService.getLastUsedEpochs(reload)
            val detailedApps =
                coroutineScope {
                    val appsDeferred =
                        filteredWithBasic.map { (ai, basicApp) ->
                            async {
                                mapToAppDetailed(ai, basicApp, lastUsedEpochs)
                            }
                        }
                    appsDeferred.awaitAll()
                }
            emit(sortApps(detailedApps, field, descending))
        }

    private fun getFilteredApps(showSystemApps: Boolean): List<Pair<ApplicationInfo, App>> {
        var flags =
            (PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS)
                .toLong()
        if (Build.VERSION.SDK_INT >= 35) {
            flags = flags or PackageManager.MATCH_ARCHIVED_PACKAGES
        }
        val allInstalled = packageService.getInstalledApplications(flags)
        val launchablePackages = packageService.getLaunchablePackages()

        return allInstalled.mapNotNull { ai ->
            val archived = ai.isArchivedApp
            val isUserInstalled = ai.isUserInstalled
            val hasLaunch = launchablePackages.contains(ai.packageName)
            if ((showSystemApps || isUserInstalled) && (archived || hasLaunch)) {
                ai to mapToAppBasic(ai, archived)
            } else {
                null
            }
        }
    }

    private fun mapToAppBasic(
        ai: ApplicationInfo,
        archived: Boolean,
    ): App {
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
            isDetailed = false,
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
                isDetailed = true,
            )
        } catch (e: Exception) {
            crashReporter?.recordException(e, "AndroidAppRepository.loadApps failed for ${ai.packageName}")
            basicApp.copy(isDetailed = true)
        }

    private fun sortApps(
        apps: List<App>,
        field: AppInfoField,
        descending: Boolean,
    ): List<App> {
        val collator = Collator.getInstance()
        val comparator = compareBy<App> { sortKey(it, field) }.thenBy(collator) { it.name }
        val finalComparator = if (descending) comparator.reversed() else comparator

        return apps.sortedWith(finalComparator)
    }

    private fun sortKey(
        app: App,
        field: AppInfoField,
    ): Comparable<*>? = field.getValue(app)

    // Copy of Android's flag to avoid direct dependency on PackageInfo in signature
    private companion object {
        const val PACKAGEINFO_REQUESTED_PERMISSION_GRANTED: Int = 2
    }
}
