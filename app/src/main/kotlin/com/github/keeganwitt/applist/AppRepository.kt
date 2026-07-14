package com.github.keeganwitt.applist

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.github.keeganwitt.applist.db.AppCacheEntity
import com.github.keeganwitt.applist.db.AppDao
import com.github.keeganwitt.applist.db.toCacheEntity
import com.github.keeganwitt.applist.db.toDomainModel
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.Collator
import java.util.concurrent.TimeUnit

sealed class SyncState {
    data object Idle : SyncState()

    data class BuildingInitial(
        val progress: Int,
        val total: Int,
    ) : SyncState()

    data object SyncingBackground : SyncState()
}

interface AppRepository {
    fun loadApps(
        field: AppInfoField,
        systemAppsOnly: Boolean,
        showArchivedApps: Boolean,
        descending: Boolean,
        reload: Boolean,
    ): Flow<List<App>>

    fun getSyncState(): Flow<SyncState>

    suspend fun refreshCache(force: Boolean = false)

    suspend fun getCachedApps(): List<App>
}

class AndroidAppRepository(
    private val packageService: PackageService,
    private val usageStatsService: UsageStatsService,
    private val storageService: StorageService,
    private val appStoreService: AppStoreService,
    private val appDao: AppDao,
    private val crashReporter: CrashReporter? = null,
) : AppRepository {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    private val syncMutex = Mutex()

    override fun getSyncState(): Flow<SyncState> = _syncState.asStateFlow()

    override fun loadApps(
        field: AppInfoField,
        systemAppsOnly: Boolean,
        showArchivedApps: Boolean,
        descending: Boolean,
        reload: Boolean,
    ): Flow<List<App>> =
        channelFlow {
            // First emit from DB
            val initialEntities = appDao.getAllApps()
            if (initialEntities.isNotEmpty()) {
                val apps = initialEntities.map { it.toDomainModel() }
                send(sortAndFilter(apps, field, systemAppsOnly, showArchivedApps, descending))
            }

            // Trigger sync in background if needed or if reload is true
            launch {
                refreshCache(force = reload)
            }

            // Now observe DB for updates
            appDao.getAllAppsFlow().collect { entities ->
                val apps = entities.map { it.toDomainModel() }
                send(sortAndFilter(apps, field, systemAppsOnly, showArchivedApps, descending))
            }
        }

    private fun sortAndFilter(
        apps: List<App>,
        field: AppInfoField,
        systemAppsOnly: Boolean,
        showArchivedApps: Boolean,
        descending: Boolean,
    ): List<App> {
        val filtered =
            apps.filter { app ->
                val archived = app.archived == true
                val isUserInstalled = app.isUserInstalled
                val hasLaunch = app.hasLaunchIntent
                val isVisible = if (archived) showArchivedApps else hasLaunch
                (if (systemAppsOnly) !isUserInstalled else isUserInstalled) && isVisible
            }
        return sortApps(filtered, field, descending)
    }

    override suspend fun refreshCache(force: Boolean) {
        if (!force && _syncState.value != SyncState.Idle) return

        val cachedApps: List<AppCacheEntity>
        val isInitial: Boolean

        syncMutex.withLock {
            if (!force && _syncState.value != SyncState.Idle) return

            cachedApps = appDao.getAllApps()
            isInitial = cachedApps.isEmpty()

            if (isInitial) {
                _syncState.value = SyncState.BuildingInitial(0, 0)
            } else {
                _syncState.value = SyncState.SyncingBackground
            }
        }

        try {
            var flags =
                (
                    PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES or
                        PackageManager.MATCH_DISABLED_COMPONENTS
                ).toLong()
            if (Build.VERSION.SDK_INT >= 35) {
                flags = flags or PackageManager.MATCH_ARCHIVED_PACKAGES
            }
            val allInstalled = packageService.getInstalledApplications(flags)
            val launchablePackages = packageService.getLaunchablePackages().toSet()

            // Reconciliation
            val cachedMap = cachedApps.associateBy { it.packageName }
            val installedPackageNames = allInstalled.map { it.packageName }.toSet()

            // Remove uninstalled
            val toRemove = cachedApps.filter { it.packageName !in installedPackageNames }.map { it.packageName }
            if (toRemove.isNotEmpty()) appDao.deleteApps(toRemove)

            // Identify to sync
            val toSync =
                allInstalled.filter { ai ->
                    val cached = cachedMap[ai.packageName]
                    if (force || cached == null) return@filter true

                    // Check last update time
                    val pkgInfo =
                        try {
                            packageService.getPackageInfo(ai)
                        } catch (e: Exception) {
                            return@filter false
                        }
                    if (pkgInfo.lastUpdateTime > (cached.lastUpdated ?: 0L)) return@filter true

                    // Check staleness (e.g. 24 hours)
                    if (System.currentTimeMillis() - cached.lastCachedAt > STALENESS_THRESHOLD) return@filter true

                    false
                }

            if (toSync.isNotEmpty()) {
                if (isInitial) _syncState.value = SyncState.BuildingInitial(0, toSync.size)

                val lastUsedEpochs = usageStatsService.getLastUsedEpochs(force)

                var processedCount = 0
                toSync.chunked(10).forEach { chunk ->
                    coroutineScope {
                        val apps =
                            chunk
                                .map { ai ->
                                    async {
                                        val basic = mapToAppBasic(ai, launchablePackages.contains(ai.packageName))
                                        mapToAppDetailed(ai, basic, lastUsedEpochs)
                                    }
                                }.awaitAll()
                        appDao.insertApps(apps.map { it.toCacheEntity(System.currentTimeMillis()) })
                    }
                    processedCount += chunk.size

                    if (isInitial) {
                        _syncState.value = SyncState.BuildingInitial(processedCount, toSync.size)
                    }
                }
            }
        } finally {
            _syncState.value = SyncState.Idle
        }
    }

    override suspend fun getCachedApps(): List<App> = appDao.getAllApps().map { it.toDomainModel() }

    private fun mapToAppBasic(
        ai: ApplicationInfo,
        hasLaunchIntent: Boolean,
    ): App {
        val archived = ai.isArchivedApp
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
            storeUrl = null,
            grantedPermissionsCount = null,
            requestedPermissionsCount = null,
            enabled = ai.enabled,
            isUserInstalled = ai.isUserInstalled,
            hasLaunchIntent = hasLaunchIntent,
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
        } catch (e: PackageManager.NameNotFoundException) {
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
            val storeUrl = appStoreService.appStoreLink(ai.packageName ?: "", installerPackage)
            app =
                app.copy(
                    installerName = installerName,
                    existsInStore = existsInStore,
                    storeUrl = storeUrl,
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
        val STALENESS_THRESHOLD = TimeUnit.DAYS.toMillis(1)
    }
}
