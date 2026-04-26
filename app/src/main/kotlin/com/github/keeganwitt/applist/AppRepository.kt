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
            val filteredInfos = getFilteredAppInfos(showSystemApps)
            val filteredWithBasic =
                coroutineScope {
                    filteredInfos
                        .map { (ai, archived) ->
                            async {
                                ai to mapToAppBasic(ai)
                            }
                        }.awaitAll()
                }
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

    private fun getFilteredAppInfos(showSystemApps: Boolean): List<Pair<ApplicationInfo, Boolean>> {
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
                ai to archived
            } else {
                null
            }
        }
    }

    private fun mapToAppBasic(ai: ApplicationInfo): App {
        val archived =
            if (Build.VERSION.SDK_INT >= 35) {
                ai.isArchived
            } else {
                ai.isArchivedApp
            }
        val packageName = ai.packageName ?: ""
        return App(
            packageName = packageName,
            name = packageService.loadLabel(ai),
            versionName = null,
            archived = archived,
            minSdk = ai.minSdkVersion,
            targetSdk = ai.targetSdkVersion,
            firstInstalled = null,
            lastUpdated = null,
            lastUsed = null,
            sizes = StorageUsage(),
            installerName = null,
            existsInStore = null,
            grantedPermissionsCount = null,
            requestedPermissionsCount = null,
            enabled = ai.enabled,
            isDetailed = false,
        )
    }

    private suspend fun mapToAppDetailed(
        ai: ApplicationInfo,
        basicApp: App,
        lastUsedEpochs: Map<String, Long>?,
    ): App {
        var app = basicApp
        val failedFields = mutableSetOf<AppInfoField>()

        if (lastUsedEpochs == null) {
            failedFields.add(AppInfoField.LAST_USED)
        }

        try {
            val pkgInfo = packageService.getPackageInfo(ai)
            val flagsArr = pkgInfo.requestedPermissionsFlags
            val grantedCount =
                flagsArr?.count { flags -> (flags and PACKAGEINFO_REQUESTED_PERMISSION_GRANTED) != 0 }
                    ?: 0
            val requestedCount = pkgInfo.requestedPermissions?.size ?: 0

            app =
                app.copy(
                    versionName = pkgInfo.versionName,
                    firstInstalled = pkgInfo.firstInstallTime,
                    lastUpdated = pkgInfo.lastUpdateTime,
                    grantedPermissionsCount = grantedCount,
                    requestedPermissionsCount = requestedCount,
                )
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            android.util.Log.w("AndroidAppRepository", "App uninstalled during load: ${ai.packageName}")
        } catch (e: Exception) {
            crashReporter?.recordException(e, "AndroidAppRepository.loadApps failed to getPackageInfo for ${ai.packageName}")
            failedFields.addAll(
                listOf(
                    AppInfoField.VERSION,
                    AppInfoField.FIRST_INSTALLED,
                    AppInfoField.LAST_UPDATED,
                    AppInfoField.GRANTED_PERMISSIONS,
                    AppInfoField.REQUESTED_PERMISSIONS,
                ),
            )
        }

        try {
            val storage = storageService.getStorageUsage(ai)
            app = app.copy(sizes = storage)
            if (storage.apkBytes == null) failedFields.add(AppInfoField.APK_SIZE)
            if (storage.appBytes == null) failedFields.add(AppInfoField.APP_SIZE)
            if (storage.cacheBytes == null) failedFields.add(AppInfoField.CACHE_SIZE)
            if (storage.dataBytes == null) failedFields.add(AppInfoField.DATA_SIZE)
            if (storage.externalCacheBytes == null) failedFields.add(AppInfoField.EXTERNAL_CACHE_SIZE)
            if (storage.totalBytes == null) failedFields.add(AppInfoField.TOTAL_SIZE)
        } catch (e: Exception) {
            crashReporter?.recordException(e, "AndroidAppRepository.loadApps failed to getStorageUsage for ${ai.packageName}")
            failedFields.addAll(
                listOf(
                    AppInfoField.APK_SIZE,
                    AppInfoField.APP_SIZE,
                    AppInfoField.CACHE_SIZE,
                    AppInfoField.DATA_SIZE,
                    AppInfoField.EXTERNAL_CACHE_SIZE,
                    AppInfoField.TOTAL_SIZE,
                ),
            )
        }

        try {
            val installerPackage = packageService.getInstallerPackageName(ai)
            val installerName = appStoreService.installerDisplayName(installerPackage)
            val existsInStore = appStoreService.existsInAppStore(ai.packageName ?: "", installerPackage)
            app =
                app.copy(
                    installerName = installerName,
                    existsInStore = existsInStore,
                )
        } catch (e: Exception) {
            crashReporter?.recordException(e, "AndroidAppRepository.loadApps failed to get installer info for ${ai.packageName}")
            failedFields.addAll(
                listOf(
                    AppInfoField.PACKAGE_MANAGER,
                    AppInfoField.EXISTS_IN_APP_STORE,
                ),
            )
        }

        return app.copy(
            lastUsed = lastUsedEpochs?.get(ai.packageName) ?: 0L,
            isDetailed = true,
            failedFields = basicApp.failedFields + failedFields,
        )
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
