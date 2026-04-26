package com.github.keeganwitt.applist

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.github.keeganwitt.applist.services.AppStoreService
import com.github.keeganwitt.applist.services.PackageService
import com.github.keeganwitt.applist.services.StorageService
import com.github.keeganwitt.applist.services.UsageStatsService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AppRepositoryTest {
    private lateinit var packageService: PackageService
    private lateinit var usageStatsService: UsageStatsService
    private lateinit var storageService: StorageService
    private lateinit var appStoreService: AppStoreService
    private lateinit var crashReporter: CrashReporter
    private lateinit var repository: AndroidAppRepository

    @Before
    fun setup() {
        packageService = mockk(relaxed = true)
        usageStatsService = mockk(relaxed = true)
        storageService = mockk(relaxed = true)
        appStoreService = mockk(relaxed = true)
        crashReporter = mockk(relaxed = true)
        repository =
            AndroidAppRepository(
                packageService,
                usageStatsService,
                storageService,
                appStoreService,
                crashReporter,
            )
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
            every { packageService.getInstallerPackageName(any()) } returns "com.android.vending"
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()
            every { storageService.getStorageUsage(any()) } returns StorageUsage()
            every { appStoreService.installerDisplayName(any()) } returns "Google Play"
            coEvery { appStoreService.existsInAppStore(any(), any()) } returns true

            val flow =
                repository.loadApps(
                    field = AppInfoField.VERSION,
                    showSystemApps = false,
                    descending = false,
                    reload = false,
                )

            val results = flow.toList()
            val finalDocs = results.last()

            assertEquals(1, finalDocs.size)
            assertEquals("com.test.app", finalDocs[0].packageName)
            assertEquals("Test App", finalDocs[0].name)
            assertEquals("1.0.0", finalDocs[0].versionName)
        }

    @Test
    fun `given system apps, when loadApps called with showSystemApps false, filtered list is returned`() =
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
                        showSystemApps = false,
                        descending = false,
                        reload = false,
                    ).toList()
                    .last()

            assertEquals(1, result.size)
            assertEquals("com.test.userapp", result[0].packageName)
        }

    @Test
    fun `given system apps, when loadApps called with showSystemApps true, system apps are included`() =
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
                        showSystemApps = true,
                        descending = false,
                        reload = false,
                    ).toList()
                    .last()

            assertEquals(2, result.size)
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
                        showSystemApps = false,
                        descending = true,
                        reload = false,
                    ).toList()
                    .last()

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
                        showSystemApps = false,
                        descending = false,
                        reload = false,
                    ).toList()
                    .last()

            assertEquals(1, result.size)
            verify(exactly = 0) { crashReporter.log(any()) }
            verify(exactly = 0) { crashReporter.recordException(any(), any()) }
        }

    @Test
    fun `given apps, when loadApps called, then emits basic info first then detailed info`() =
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
                    showSystemApps = false,
                    descending = false,
                    reload = false,
                )

            val results = flow.toList()

            assertEquals(2, results.size)

            // First emission: Basic info
            val firstEmission = results[0]
            assertEquals(1, firstEmission.size)
            assertNull(firstEmission[0].versionName)

            // Second emission: Detailed info
            val secondEmission = results[1]
            assertEquals(1, secondEmission.size)
            assertEquals("1.0.0", secondEmission[0].versionName)
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
            every { packageService.getInstallerPackageName(any()) } returns "com.android.vending"
            every { appStoreService.installerDisplayName(any()) } returns "Google Play"

            val result =
                repository
                    .loadApps(
                        field = AppInfoField.VERSION,
                        showSystemApps = false,
                        descending = false,
                        reload = false,
                    ).toList()
                    .last()

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
                        showSystemApps = false,
                        descending = false,
                        reload = false,
                    ).toList()
                    .last()

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
                        showSystemApps = false,
                        descending = false,
                        reload = false,
                    ).toList()
                    .last()

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
                createApplicationInfo(null).apply {
                    metaData = Bundle().apply { putBoolean("com.android.vending.archive", true) }
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
                        showSystemApps = true,
                        descending = false,
                        reload = false,
                    ).toList()
                    .last()

            assertEquals(1, result.size)
            assertEquals("", result[0].packageName)
        }

    @Test
    @Config(sdk = [34])
    fun `given SDK 34, when loadApps called, then it uses isArchivedApp utility`() =
        runTest {
            val appInfo =
                createApplicationInfo("com.test.archived").apply {
                    metaData = Bundle().apply { putBoolean("com.android.vending.archive", true) }
                }
            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getLaunchablePackages() } returns emptySet()
            every { packageService.getPackageInfo(any()) } returns createPackageInfo("1.0.0")

            val result = repository.loadApps(AppInfoField.VERSION, true, false, false).toList().last()

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
                    null,
                )

            val result =
                nullReporterRepository
                    .loadApps(
                        field = AppInfoField.TARGET_SDK,
                        showSystemApps = false,
                        descending = false,
                        reload = true,
                    ).toList()
                    .last()

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

            val result = repository.loadApps(AppInfoField.VERSION, false, false, false).toList().last()

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

            repository.loadApps(AppInfoField.VERSION, false, false, false).toList()

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

            val result = repository.loadApps(AppInfoField.VERSION, false, false, false).toList().last()

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

            val result = repository.loadApps(AppInfoField.VERSION, false, false, false).toList().last()

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

            val result = repository.loadApps(AppInfoField.VERSION, false, false, false).toList().last()

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

            val result = repository.loadApps(AppInfoField.VERSION, false, false, false).toList().last()

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
}
