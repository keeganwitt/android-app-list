package com.github.keeganwitt.applist

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.github.keeganwitt.applist.db.AppDao
import com.github.keeganwitt.applist.db.toCacheEntity
import com.github.keeganwitt.applist.services.AppStoreService
import com.github.keeganwitt.applist.services.PackageService
import com.github.keeganwitt.applist.services.StorageService
import com.github.keeganwitt.applist.services.UsageStatsService
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppRepositoryTest {
    private lateinit var packageService: PackageService
    private lateinit var usageStatsService: UsageStatsService
    private lateinit var storageService: StorageService
    private lateinit var appStoreService: AppStoreService
    private lateinit var appDao: AppDao
    private lateinit var crashReporter: CrashReporter
    private lateinit var repository: AndroidAppRepository
    private val dbFlow = MutableStateFlow<List<com.github.keeganwitt.applist.db.AppCacheEntity>>(emptyList())

    @Before
    fun setup() {
        packageService = mockk(relaxed = true)
        usageStatsService = mockk(relaxed = true)
        storageService = mockk(relaxed = true)
        appStoreService = mockk(relaxed = true)
        appDao = mockk(relaxed = true)
        crashReporter = mockk(relaxed = true)
        dbFlow.value = emptyList()
        every { appDao.getAllAppsFlow() } returns dbFlow
        coEvery { appDao.getAllApps() } answers { dbFlow.value }
        coEvery { appDao.insertApps(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val apps = it.invocation.args[0] as List<com.github.keeganwitt.applist.db.AppCacheEntity>
            dbFlow.value = apps
        }
        coEvery { appDao.deleteApps(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val names = it.invocation.args[0] as List<String>
            dbFlow.value = dbFlow.value.filter { it.packageName !in names }
        }
        repository =
            AndroidAppRepository(
                packageService,
                usageStatsService,
                storageService,
                appStoreService,
                appDao,
                crashReporter,
            )
    }

    @Test
    fun `given empty cache, when loadApps called, then it triggers sync and returns from DB when sync completes`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.app")
            val packageInfo = createPackageInfo("1.0.0")

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getPackageInfo(any<ApplicationInfo>()) } returns packageInfo
            every { packageService.loadLabel(any<ApplicationInfo>()) } returns "Test App"
            every { packageService.getLaunchablePackages() } returns setOf("com.test.app")
            every { usageStatsService.getLastUsedEpochs(any<Boolean>()) } returns emptyMap()
            every { storageService.getStorageUsage(any<ApplicationInfo>()) } returns StorageUsage()
            coEvery { appStoreService.existsInAppStore(any<String>(), any<String>()) } returns true

            val flow = repository.loadApps(AppInfoField.VERSION, false, false, false, false)
            val result = flow.first { it.isNotEmpty() }

            assertEquals(1, result.size)
            assertEquals("com.test.app", result[0].packageName)
            coVerify { appDao.insertApps(any()) }
        }

    @Test
    fun `given cached apps, when refreshCache called with uninstall, then it deletes from DB`() =
        runTest {
            val cachedApp = createAppEntity("com.uninstalled.app")
            dbFlow.value = listOf<com.github.keeganwitt.applist.db.AppCacheEntity>(cachedApp)

            every { packageService.getInstalledApplications(any<Long>()) } returns emptyList()
            every { packageService.getLaunchablePackages() } returns emptySet()

            repository.refreshCache()

            coVerify { appDao.deleteApps(listOf("com.uninstalled.app")) }
            assertTrue(dbFlow.value.isEmpty())
        }

    @Test
    fun `given stale cache, when refreshCache called, then it updates DB`() =
        runTest {
            val staleTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)
            val cachedApp = createAppEntity("com.test.app").copy(lastCachedAt = staleTime)
            dbFlow.value = listOf<com.github.keeganwitt.applist.db.AppCacheEntity>(cachedApp)

            val appInfo = createApplicationInfo("com.test.app")
            val packageInfo = createPackageInfo("1.0.0")
            packageInfo.lastUpdateTime = System.currentTimeMillis()

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getPackageInfo(any<ApplicationInfo>()) } returns packageInfo
            every { packageService.loadLabel(any<ApplicationInfo>()) } returns "Updated App"

            repository.refreshCache()

            coVerify { appDao.insertApps(any<List<com.github.keeganwitt.applist.db.AppCacheEntity>>()) }
            assertEquals("Updated App", dbFlow.value[0].name)
        }

    @Test
    fun `given sync in progress, then syncState reflects progress`() =
        runTest {
            val appInfo1 = createApplicationInfo("com.app1")
            val appInfo2 = createApplicationInfo("com.app2")
            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo1, appInfo2)
            every { packageService.getPackageInfo(any<ApplicationInfo>()) } returns createPackageInfo("1.0")

            val states = mutableListOf<SyncState>()
            val job =
                launch {
                    repository.getSyncState().collect { states.add(it) }
                }
            yield()
            runCurrent()

            repository.refreshCache()
            yield()

            assertTrue(states.any { it is SyncState.BuildingInitial })
            assertTrue(states.last() is SyncState.Idle)
            job.cancel()
        }

    @Test
    fun `given cached apps, then getCachedApps returns them`() =
        runTest {
            val entity1 = createAppEntity("pkg1")
            val entity2 = createAppEntity("pkg2")
            coEvery { appDao.getAllApps() } returns listOf(entity1, entity2)

            val result = repository.getCachedApps()

            assertEquals(2, result.size)
            assertEquals("pkg1", result[0].packageName)
            assertEquals("pkg2", result[1].packageName)
        }

    @Test
    fun `given installed apps, when loadApps called, then apps are returned`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.app")
            val packageInfo = createPackageInfo("1.0.0")

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getLaunchablePackages() } returns setOf("com.test.app")
            every { packageService.getPackageInfo(any()) } returns packageInfo
            every { packageService.loadLabel(any()) } returns "Test App"
            every { packageService.getInstallerPackageName(any()) } returns AppStoreService.GOOGLE_PLAY
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()
            every { storageService.getStorageUsage(any()) } returns StorageUsage()
            every { appStoreService.installerDisplayName(any()) } returns "Google Play"
            every { appStoreService.appStoreLink("com.test.app", AppStoreService.GOOGLE_PLAY) } returns
                "https://play.google.com/store/apps/details?id=com.test.app"
            coEvery { appStoreService.existsInAppStore(any(), any()) } returns true

            val flow =
                repository.loadApps(
                    field = AppInfoField.VERSION,
                    systemAppsOnly = false,
                    showArchivedApps = false,
                    descending = false,
                    reload = false,
                )

            val result = flow.first { it.isNotEmpty() }

            assertEquals(1, result.size)
            assertEquals("com.test.app", result[0].packageName)
            assertEquals("Test App", result[0].name)
            assertEquals("1.0.0", result[0].versionName)
            assertEquals("https://play.google.com/store/apps/details?id=com.test.app", result[0].storeUrl)
        }

    @Test
    fun `given system apps, when loadApps called with systemAppsOnly false, filtered list is returned`() =
        runTest {
            val userApp = createApplicationInfo("com.test.userapp", isSystemApp = false)
            val systemApp = createApplicationInfo("com.android.system", isSystemApp = true)

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(userApp, systemApp)
            every { packageService.getLaunchablePackages() } returns setOf("com.test.userapp", "com.android.system")
            every { packageService.getPackageInfo(any()) } returns createPackageInfo("1.0.0")
            every { packageService.loadLabel(any()) } returns "App"
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()

            val result =
                repository
                    .loadApps(
                        field = AppInfoField.VERSION,
                        systemAppsOnly = false,
                        showArchivedApps = false,
                        descending = false,
                        reload = false,
                    ).first { it.isNotEmpty() }

            assertEquals(1, result.size)
            assertEquals("com.test.userapp", result[0].packageName)
        }

    @Test
    fun `given system apps, when loadApps called with systemAppsOnly true, only system apps are returned`() =
        runTest {
            val userApp = createApplicationInfo("com.test.userapp", isSystemApp = false)
            val systemApp = createApplicationInfo("com.android.system", isSystemApp = true)

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(userApp, systemApp)
            every { packageService.getLaunchablePackages() } returns setOf("com.test.userapp", "com.android.system")
            every { packageService.getPackageInfo(any()) } returns createPackageInfo("1.0.0")
            every { packageService.loadLabel(any()) } returns "App"
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()

            val result =
                repository
                    .loadApps(
                        field = AppInfoField.VERSION,
                        systemAppsOnly = true,
                        showArchivedApps = false,
                        descending = false,
                        reload = false,
                    ).first { it.isNotEmpty() }

            assertEquals(1, result.size)
            assertEquals("com.android.system", result[0].packageName)
        }

    @Test
    fun `given non-launchable user app, when user apps are selected, then it is filtered out`() =
        runTest {
            val app = createApp(packageName = "com.test.nonlaunchable").copy(hasLaunchIntent = false)
            dbFlow.value = listOf(app.toCacheEntity(System.currentTimeMillis()))

            val result =
                repository
                    .loadApps(
                        field = AppInfoField.VERSION,
                        systemAppsOnly = false,
                        showArchivedApps = false,
                        descending = false,
                        reload = false,
                    ).first()

            assertTrue(result.isEmpty())
        }

    @Test
    fun `given apps, when loadApps called with descending true, apps are sorted descending`() =
        runTest {
            val app1 = createApplicationInfo("com.test.app1")
            val app2 = createApplicationInfo("com.test.app2")
            val app3 = createApplicationInfo("com.test.app3")

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(app1, app2, app3)
            every { packageService.getLaunchablePackages() } returns setOf("com.test.app1", "com.test.app2", "com.test.app3")
            every { packageService.getPackageInfo(any()) } returns createPackageInfo("1.0.0")
            every { packageService.loadLabel(app1) } returns "App A"
            every { packageService.loadLabel(app2) } returns "App B"
            every { packageService.loadLabel(app3) } returns "App C"
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()

            val result =
                repository
                    .loadApps(
                        field = AppInfoField.VERSION,
                        systemAppsOnly = false,
                        showArchivedApps = false,
                        descending = true,
                        reload = false,
                    ).first { it.isNotEmpty() }

            assertEquals(3, result.size)
            assertEquals("App C", result[0].name)
            assertEquals("App B", result[1].name)
            assertEquals("App A", result[2].name)
        }

    @Test
    fun `given app throws NameNotFoundException, when loadApps called, then it is not logged to crash reporter`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.app")

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getLaunchablePackages() } returns setOf("com.test.app")
            every { packageService.getPackageInfo(any()) } throws PackageManager.NameNotFoundException()
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()

            val result =
                repository
                    .loadApps(
                        field = AppInfoField.VERSION,
                        systemAppsOnly = false,
                        showArchivedApps = false,
                        descending = false,
                        reload = false,
                    ).first { it.isNotEmpty() }

            assertEquals(1, result.size)
            verify(exactly = 0) { crashReporter.log(any()) }
            verify(exactly = 0) { crashReporter.recordException(any(), any()) }
        }

    @Test
    fun `given apps, when loadApps called, then emits synced info`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.app")
            val packageInfo = createPackageInfo("1.0.0")

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getLaunchablePackages() } returns setOf("com.test.app")
            every { packageService.getPackageInfo(any()) } returns packageInfo
            every { packageService.loadLabel(any()) } returns "Test App"
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()

            val flow =
                repository.loadApps(
                    field = AppInfoField.VERSION,
                    systemAppsOnly = false,
                    showArchivedApps = false,
                    descending = false,
                    reload = false,
                )

            val result = flow.first { it.isNotEmpty() }

            assertEquals(1, result.size)
            assertEquals("1.0.0", result[0].versionName)
        }

    @Test
    fun `given getPackageInfo fails, when loadApps called, then other detailed info is still populated`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.pkg.error")
            val storageUsage = StorageUsage(apkBytes = 100, appBytes = 200)
            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getLaunchablePackages() } returns setOf("com.test.pkg.error")
            every { packageService.getPackageInfo(any()) } throws RuntimeException("Package Info Error")
            every { storageService.getStorageUsage(any()) } returns storageUsage
            every { packageService.getInstallerPackageName(any()) } returns AppStoreService.GOOGLE_PLAY
            every { appStoreService.installerDisplayName(any()) } returns "Google Play"

            val result =
                repository
                    .loadApps(
                        field = AppInfoField.VERSION,
                        systemAppsOnly = false,
                        showArchivedApps = false,
                        descending = false,
                        reload = false,
                    ).first { it.isNotEmpty() }

            assertEquals(1, result.size)
            assertNull(result[0].versionName)
            assertEquals(storageUsage, result[0].sizes)
            assertEquals("Google Play", result[0].installerName)
            assertTrue(result[0].isDetailed)
            verify { crashReporter.recordException(any(), match { it.contains("failed to getPackageInfo") }) }
        }

    @Test
    fun `given storage usage fetch fails, when loadApps called, then app remains in list with other info`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.storage.error")
            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getLaunchablePackages() } returns setOf("com.test.storage.error")
            every { packageService.getPackageInfo(any()) } returns createPackageInfo("1.0.0")
            every { storageService.getStorageUsage(any()) } throws RuntimeException("Storage Error")

            val result =
                repository
                    .loadApps(
                        field = AppInfoField.VERSION,
                        systemAppsOnly = false,
                        showArchivedApps = false,
                        descending = false,
                        reload = false,
                    ).first { it.isNotEmpty() }

            assertEquals(1, result.size)
            assertEquals("1.0.0", result[0].versionName)
            assertTrue(result[0].isDetailed)
            verify { crashReporter.recordException(any(), match { it.contains("failed to getStorageUsage") }) }
        }

    @Test
    fun `given installer info retrieval fails, when loadApps called, then other detailed info is still populated`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.installer.error")
            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getLaunchablePackages() } returns setOf("com.test.installer.error")
            every { packageService.getPackageInfo(any()) } returns createPackageInfo("1.0.0")
            every { storageService.getStorageUsage(any()) } returns StorageUsage(apkBytes = 100)
            every { packageService.getInstallerPackageName(any()) } throws RuntimeException("Installer Error")

            val result =
                repository
                    .loadApps(
                        field = AppInfoField.VERSION,
                        systemAppsOnly = false,
                        showArchivedApps = false,
                        descending = false,
                        reload = false,
                    ).first { it.isNotEmpty() }

            assertEquals(1, result.size)
            assertEquals("1.0.0", result[0].versionName)
            assertEquals(100L, result[0].sizes.apkBytes)
            assertNull(result[0].installerName)
            assertTrue(result[0].isDetailed)
            verify { crashReporter.recordException(any(), match { it.contains("failed to get installer info") }) }
        }

    @Test
    fun `given app with null package name, when loadApps called, then it handles it safely`() =
        runTest {
            val appInfo =
                createApplicationInfo(null, isSystemApp = true).apply {
                    metaData = Bundle().apply { putBoolean(AppStoreService.GOOGLE_PLAY_ARCHIVE_KEY, true) }
                }
            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getLaunchablePackages() } returns emptySet()
            every { packageService.getPackageInfo(any()) } returns createPackageInfo("1.0.0")
            every { packageService.loadLabel(any()) } returns "Test App"
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()

            val result =
                repository
                    .loadApps(
                        field = AppInfoField.VERSION,
                        systemAppsOnly = true,
                        showArchivedApps = true,
                        descending = false,
                        reload = false,
                    ).first { it.isNotEmpty() }

            assertEquals(1, result.size)
            assertEquals("", result[0].packageName)
        }

    @Test
    @Config(sdk = [34])
    fun `given SDK 34, when loadApps called, then it uses isArchivedApp utility`() =
        runTest {
            val appInfo =
                createApplicationInfo("com.test.archived", isSystemApp = true).apply {
                    metaData = Bundle().apply { putBoolean(AppStoreService.GOOGLE_PLAY_ARCHIVE_KEY, true) }
                }
            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getLaunchablePackages() } returns emptySet()
            every { packageService.getPackageInfo(any()) } returns createPackageInfo("1.0.0")

            val result =
                repository
                    .loadApps(
                        AppInfoField.VERSION,
                        systemAppsOnly = true,
                        showArchivedApps = true,
                        descending = false,
                        reload = false,
                    ).first { it.isNotEmpty() }

            assertEquals(1, result.size)
            assertTrue(result[0].archived == true)
        }

    @Test
    fun `given null crashReporter and failing loads, when loadApps called, then it handles it safely`() =
        runTest {
            val app1 = createApplicationInfo("com.test.app1")

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(app1)
            every { packageService.getLaunchablePackages() } returns setOf("com.test.app1")
            every { packageService.loadLabel(app1) } returns "App A"
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()

            // Trigger 143/145 branch
            every { packageService.getPackageInfo(app1) } throws
                PackageManager
                    .NameNotFoundException("Not found")

            // Trigger 152 branch
            every { storageService.getStorageUsage(app1) } throws RuntimeException("Storage fail")

            // Trigger 165 branch
            every { packageService.getInstallerPackageName(app1) } throws RuntimeException("Installer fail")

            val nullReporterRepository =
                AndroidAppRepository(
                    packageService,
                    usageStatsService,
                    storageService,
                    appStoreService,
                    appDao,
                    null,
                )

            val result =
                nullReporterRepository
                    .loadApps(
                        field = AppInfoField.TARGET_SDK,
                        systemAppsOnly = false,
                        showArchivedApps = false,
                        descending = false,
                        reload = true,
                    ).first { it.isNotEmpty() }

            assertEquals(1, result.size)
        }

    @Test
    fun `given usageStatsService fails, when loadApps called, then LAST_USED is in failedFields`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.usage.fail")
            every { packageService.getInstalledApplications(any()) } returns listOf(appInfo)
            every { packageService.getLaunchablePackages() } returns setOf("com.test.usage.fail")
            every { packageService.getPackageInfo(any()) } returns createPackageInfo("1.0.0")
            every { usageStatsService.getLastUsedEpochs(any()) } returns null

            val result =
                repository
                    .loadApps(
                        AppInfoField.VERSION,
                        systemAppsOnly = false,
                        showArchivedApps = false,
                        descending = false,
                        reload = false,
                    ).first { it.isNotEmpty() }

            assertEquals(1, result.size)
            assertTrue(result[0].failedFields.contains(AppInfoField.LAST_USED))
        }

    private fun createApplicationInfo(
        packageName: String?,
        isSystemApp: Boolean = false,
    ): ApplicationInfo =
        ApplicationInfo().apply {
            this.packageName = packageName
            this.flags = if (isSystemApp) ApplicationInfo.FLAG_SYSTEM else 0
            this.enabled = true
            this.minSdkVersion = 24
            this.targetSdkVersion = 33
        }

    private fun createPackageInfo(versionName: String): PackageInfo =
        PackageInfo().apply {
            this.versionName = versionName
            this.firstInstallTime = System.currentTimeMillis()
            this.lastUpdateTime = System.currentTimeMillis()
        }

    private fun createApp(
        packageName: String = "com.test.app",
        name: String = "Test",
        isDetailed: Boolean = false,
        failedFields: Set<AppInfoField> = emptySet(),
    ): App =
        App(
            packageName = packageName,
            name = name,
            versionName = null,
            archived = null,
            minSdk = null,
            targetSdk = null,
            firstInstalled = null,
            lastUpdated = null,
            lastUsed = null,
            sizes = StorageUsage(),
            installerName = null,
            existsInStore = null,
            grantedPermissionsCount = null,
            requestedPermissionsCount = null,
            enabled = true,
            isDetailed = isDetailed,
            failedFields = failedFields,
        )

    @Test
    @Config(sdk = [34])
    fun `given SDK less than 35, when loadApps called, flags do not include MATCH_ARCHIVED_PACKAGES`() =
        runTest {
            val flagsSlot = slot<Long>()
            every { packageService.getInstalledApplications(capture(flagsSlot)) } returns emptyList()

            repository
                .loadApps(
                    AppInfoField.VERSION,
                    systemAppsOnly = false,
                    showArchivedApps = false,
                    descending = false,
                    reload = false,
                ).first()

            val flags = flagsSlot.captured
            assertTrue((flags and PackageManager.MATCH_ARCHIVED_PACKAGES) == 0L)
        }

    @Test
    fun `given storage usage with null bytes, when loadApps called, then size fields are in failedFields`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.nullbytes")
            every { packageService.getInstalledApplications(any()) } returns listOf(appInfo)
            every { packageService.getLaunchablePackages() } returns setOf("com.test.nullbytes")
            every { packageService.getPackageInfo(any()) } returns createPackageInfo("1.0.0")
            every { storageService.getStorageUsage(any()) } returns StorageUsage(apkBytes = null, appBytes = 100)

            val result =
                repository
                    .loadApps(
                        AppInfoField.VERSION,
                        systemAppsOnly = false,
                        showArchivedApps = false,
                        descending = false,
                        reload = false,
                    ).first { it.isNotEmpty() }

            assertEquals(1, result.size)
            assertTrue(result[0].failedFields.contains(AppInfoField.APK_SIZE))
            org.junit.Assert.assertFalse(result[0].failedFields.contains(AppInfoField.APP_SIZE))
        }

    @Test
    fun `given storage usage with null appBytes, when loadApps called, then size fields are in failedFields`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.nullappbytes")
            every { packageService.getInstalledApplications(any()) } returns listOf(appInfo)
            every { packageService.getLaunchablePackages() } returns setOf("com.test.nullappbytes")
            every { packageService.getPackageInfo(any()) } returns createPackageInfo("1.0.0")
            every { storageService.getStorageUsage(any()) } returns StorageUsage(apkBytes = 100, appBytes = null)

            val result =
                repository
                    .loadApps(
                        AppInfoField.VERSION,
                        systemAppsOnly = false,
                        showArchivedApps = false,
                        descending = false,
                        reload = false,
                    ).first { it.isNotEmpty() }

            assertEquals(1, result.size)
            org.junit.Assert.assertFalse(result[0].failedFields.contains(AppInfoField.APK_SIZE))
            assertTrue(result[0].failedFields.contains(AppInfoField.APP_SIZE))
        }

    @Test
    fun `given appStoreService fails, when loadApps called, then installer fields are in failedFields`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.store.fail")
            every { packageService.getInstalledApplications(any()) } returns listOf(appInfo)
            every { packageService.getLaunchablePackages() } returns setOf("com.test.store.fail")
            every { packageService.getPackageInfo(any()) } returns createPackageInfo("1.0.0")
            coEvery { appStoreService.existsInAppStore(any(), any()) } throws RuntimeException("Store fail")

            val result =
                repository
                    .loadApps(
                        AppInfoField.VERSION,
                        systemAppsOnly = false,
                        showArchivedApps = false,
                        descending = false,
                        reload = false,
                    ).first { it.isNotEmpty() }

            assertEquals(1, result.size)
            assertTrue(result[0].failedFields.containsAll(listOf(AppInfoField.PACKAGE_MANAGER, AppInfoField.EXISTS_IN_APP_STORE)))
        }

    @Test
    fun `given multiple failures in mapToAppDetailed, when loadApps called, then all affected fields are in failedFields`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.multi.fail")
            every { packageService.getInstalledApplications(any()) } returns listOf(appInfo)
            every { packageService.getLaunchablePackages() } returns setOf("com.test.multi.fail")

            every { packageService.getPackageInfo(any()) } throws RuntimeException("PC fail")
            every { storageService.getStorageUsage(any()) } throws RuntimeException("Storage fail")
            every { packageService.getInstallerPackageName(any()) } throws RuntimeException("Installer fail")
            coEvery { appStoreService.existsInAppStore(any(), any()) } throws RuntimeException("Store fail")

            val result =
                repository
                    .loadApps(
                        AppInfoField.VERSION,
                        systemAppsOnly = false,
                        showArchivedApps = false,
                        descending = false,
                        reload = false,
                    ).first { it.isNotEmpty() }

            assertEquals(1, result.size)
            assertTrue(
                result[0].failedFields.containsAll(
                    listOf(
                        AppInfoField.VERSION,
                        AppInfoField.APK_SIZE,
                        AppInfoField.PACKAGE_MANAGER,
                    ),
                ),
            )
        }

    @Test
    fun `given archived app, when loadApps called with showArchivedApps false, it is filtered out`() =
        runTest {
            val archivedApp =
                createApplicationInfo("com.test.archived").apply {
                    metaData = Bundle().apply { putBoolean(AppStoreService.GOOGLE_PLAY_ARCHIVE_KEY, true) }
                }
            every { packageService.getInstalledApplications(any()) } returns listOf(archivedApp)
            every { packageService.getLaunchablePackages() } returns emptySet()

            val result =
                repository
                    .loadApps(
                        AppInfoField.VERSION,
                        systemAppsOnly = false,
                        showArchivedApps = false,
                        descending = false,
                        reload = false,
                    ).first()

            assertEquals(0, result.size)
        }

    @Test
    fun `given archived app, when loadApps called with showArchivedApps true, it is included`() =
        runTest {
            val archivedApp =
                createApplicationInfo("com.test.archived").apply {
                    metaData = Bundle().apply { putBoolean(AppStoreService.GOOGLE_PLAY_ARCHIVE_KEY, true) }
                }
            every { packageService.getInstalledApplications(any()) } returns listOf(archivedApp)
            every { packageService.getLaunchablePackages() } returns emptySet()
            every { packageService.getPackageInfo(any()) } returns createPackageInfo("1.0.0")

            val result =
                repository
                    .loadApps(
                        AppInfoField.VERSION,
                        systemAppsOnly = false,
                        showArchivedApps = true,
                        descending = false,
                        reload = false,
                    ).first { it.isNotEmpty() }

            assertEquals(1, result.size)
            assertEquals("com.test.archived", result[0].packageName)
        }

    @Test
    fun `when app uninstalled, refreshCache removes it from DB`() =
        runTest {
            // Initial state with 1 app in DB
            val entity = createApp("com.old").toCacheEntity(System.currentTimeMillis())
            dbFlow.value = listOf(entity)

            // PackageManager returns empty
            every { packageService.getInstalledApplications(any()) } returns emptyList()
            every { packageService.getLaunchablePackages() } returns emptySet()

            repository.refreshCache(force = false)

            assertTrue(dbFlow.value.isEmpty())
        }

    @Test
    fun `when cache is stale, refreshCache syncs it`() =
        runTest {
            // Cache is 2 days old
            val oldTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)
            val entity = createApp("com.test").toCacheEntity(oldTime)
            dbFlow.value = listOf(entity)

            val appInfo = createApplicationInfo("com.test")
            every { packageService.getInstalledApplications(any()) } returns listOf(appInfo)
            every { packageService.getLaunchablePackages() } returns setOf("com.test")
            every { packageService.getPackageInfo(any()) } returns createPackageInfo("1.1.0")

            repository.refreshCache(force = false)

            // Verify it was updated (lastCachedAt should be recent)
            assertTrue(dbFlow.value[0].lastCachedAt > oldTime)
            assertEquals("1.1.0", dbFlow.value[0].versionName)
        }

    private fun createAppEntity(packageName: String): com.github.keeganwitt.applist.db.AppCacheEntity =
        com.github.keeganwitt.applist.db.AppCacheEntity(
            packageName = packageName,
            name = "Name",
            versionName = "1.0",
            archived = false,
            minSdk = 24,
            targetSdk = 33,
            firstInstalled = 1000L,
            lastUpdated = 1000L,
            lastUsed = 1000L,
            sizes = StorageUsage(100, 200, 300, 400, 500),
            installerName = AppStoreService.GOOGLE_PLAY,
            existsInStore = true,
            grantedPermissionsCount = 5,
            requestedPermissionsCount = 10,
            enabled = true,
            isUserInstalled = true,
            hasLaunchIntent = true,
            isDetailed = true,
            failedFields = emptySet(),
            lastCachedAt = System.currentTimeMillis(),
        )

    @Test
    fun `given non-empty cache, when loadApps called, then it emits from DB immediately`() =
        runTest {
            val cachedApp = createAppEntity("com.cached.app")
            dbFlow.value = listOf(cachedApp)

            val flow = repository.loadApps(AppInfoField.VERSION, false, false, false, false)
            val result = flow.first()

            assertEquals(1, result.size)
            assertEquals("com.cached.app", result[0].packageName)
        }

    @Test
    fun `given package info retrieval fails during reconciliation, when refreshCache called, then it skips the app`() =
        runTest {
            val cachedApp = createAppEntity("com.test.app")
            dbFlow.value = listOf(cachedApp)

            val appInfo = createApplicationInfo("com.test.app")
            every { packageService.getInstalledApplications(any()) } returns listOf(appInfo)
            every { packageService.getPackageInfo(appInfo) } throws RuntimeException("Package Info Error")

            repository.refreshCache()

            coVerify(exactly = 0) { appDao.insertApps(any()) }
        }

    @Test
    fun `given app is up to date and not stale, when refreshCache called, then it does not sync the app`() =
        runTest {
            val now = System.currentTimeMillis()
            val cachedApp =
                createAppEntity("com.test.app").copy(
                    lastUpdated = now,
                    lastCachedAt = now,
                )
            dbFlow.value = listOf(cachedApp)

            val appInfo = createApplicationInfo("com.test.app")
            val packageInfo = createPackageInfo("1.0").apply { lastUpdateTime = now }
            every { packageService.getInstalledApplications(any()) } returns listOf(appInfo)
            every { packageService.getPackageInfo(appInfo) } returns packageInfo

            repository.refreshCache()

            coVerify(exactly = 0) { appDao.insertApps(any()) }
        }

    @Test
    fun `given sync already in progress, when refreshCache called without force, then it returns early`() =
        runTest {
            val appInfo = createApplicationInfo("com.app1")
            every { packageService.getInstalledApplications(any()) } returns listOf(appInfo)
            coEvery { appDao.getAllApps() } coAnswers {
                kotlinx.coroutines.delay(1000)
                emptyList()
            }

            // Trigger first sync in background
            val syncJob =
                launch {
                    repository.refreshCache()
                }

            // Wait for it to start and hit the delayed DAO call
            yield()
            runCurrent()

            // Now try another sync. The first is still in withLock or has already set _syncState.
            repository.refreshCache(force = false)

            // Verify only 1 call was made to getInstalledApplications (from the first sync)
            verify(exactly = 1) { packageService.getInstalledApplications(any()) }

            syncJob.join()
        }

    @Test
    @Config(sdk = [35])
    fun `given SDK 35, when loadApps called, flags include MATCH_ARCHIVED_PACKAGES`() =
        runTest {
            val flagsSlot = slot<Long>()
            every { packageService.getInstalledApplications(capture(flagsSlot)) } returns emptyList()

            repository
                .loadApps(
                    AppInfoField.VERSION,
                    systemAppsOnly = false,
                    showArchivedApps = false,
                    descending = false,
                    reload = false,
                ).first()

            val flags = flagsSlot.captured
            assertTrue((flags and PackageManager.MATCH_ARCHIVED_PACKAGES) != 0L)
        }
}
