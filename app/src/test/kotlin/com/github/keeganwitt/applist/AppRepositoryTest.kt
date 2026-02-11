package com.github.keeganwitt.applist

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.github.keeganwitt.applist.services.AppStoreService
import com.github.keeganwitt.applist.services.PackageService
import com.github.keeganwitt.applist.services.StorageService
import com.github.keeganwitt.applist.services.UsageStatsService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
            every { packageService.getLaunchIntentForPackage(any()) } returns mockk()
            every { packageService.getPackageInfo(any()) } returns packageInfo
            every { packageService.loadLabel(any()) } returns "Test App"
            every { packageService.getInstallerPackageName(any()) } returns "com.android.vending"
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()
            every { storageService.getStorageUsage(any()) } returns StorageUsage()
            every { appStoreService.installerDisplayName(any()) } returns "Google Play"
            every { appStoreService.existsInAppStore(any(), any()) } returns true

            val flow =
                repository.loadApps(
                    field = AppInfoField.VERSION,
                    showSystemApps = false,
                    descending = false,
                    reload = false,
                )

            val result = flow.toList()
            val finalDocs = result.last()

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
            val packageInfo = createPackageInfo("1.0.0")

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(userApp, systemApp)
            every { packageService.getLaunchIntentForPackage(any()) } returns mockk()
            every { packageService.getPackageInfo(any()) } returns packageInfo
            every { packageService.loadLabel(any()) } returns "App"
            every { packageService.getInstallerPackageName(any()) } returns null
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()
            every { storageService.getStorageUsage(any()) } returns StorageUsage()
            every { appStoreService.installerDisplayName(any()) } returns "Unknown"
            every { appStoreService.existsInAppStore(any(), any()) } returns null

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
            val packageInfo = createPackageInfo("1.0.0")

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(userApp, systemApp)
            every { packageService.getLaunchIntentForPackage(any()) } returns mockk()
            every { packageService.getPackageInfo(any()) } returns packageInfo
            every { packageService.loadLabel(any()) } returns "App"
            every { packageService.getInstallerPackageName(any()) } returns null
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()
            every { storageService.getStorageUsage(any()) } returns StorageUsage()
            every { appStoreService.installerDisplayName(any()) } returns "Unknown"
            every { appStoreService.existsInAppStore(any(), any()) } returns null

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
    fun `given apps without launch intent, when loadApps called, apps are filtered out`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.app")

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getLaunchIntentForPackage(any()) } returns null
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

            assertTrue(result.isEmpty())
        }

    @Test
    fun `given apps, when loadApps called with descending true, apps are sorted descending`() =
        runTest {
            val app1 = createApplicationInfo("com.test.app1")
            val app2 = createApplicationInfo("com.test.app2")
            val app3 = createApplicationInfo("com.test.app3")
            val packageInfo = createPackageInfo("1.0.0")

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(app1, app2, app3)
            every { packageService.getLaunchIntentForPackage(any()) } returns mockk()
            every { packageService.getPackageInfo(any()) } returns packageInfo
            every { packageService.loadLabel(app1) } returns "App A"
            every { packageService.loadLabel(app2) } returns "App B"
            every { packageService.loadLabel(app3) } returns "App C"
            every { packageService.getInstallerPackageName(any()) } returns null
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()
            every { storageService.getStorageUsage(any()) } returns StorageUsage()
            every { appStoreService.installerDisplayName(any()) } returns "Unknown"
            every { appStoreService.existsInAppStore(any(), any()) } returns null

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
    fun `given app throws exception, when loadApps called, exception is caught`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.app")

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getLaunchIntentForPackage(any()) } returns mockk()
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
            // Crash reporter might be called during detailed mapping, which happens in the flow
            // Since we collect the flow, it should trigger
            verify(atLeast = 1) { crashReporter.recordException(any(), any()) }
        }

    @Test
    fun `given apps with usage stats, when loadApps called, last used times are populated`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.app")
            val packageInfo = createPackageInfo("1.0.0")
            val lastUsedTime = 1234567890L

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getLaunchIntentForPackage(any()) } returns mockk()
            every { packageService.getPackageInfo(any()) } returns packageInfo
            every { packageService.loadLabel(any()) } returns "Test App"
            every { packageService.getInstallerPackageName(any()) } returns null
            every { usageStatsService.getLastUsedEpochs(any()) } returns mapOf("com.test.app" to lastUsedTime)
            every { storageService.getStorageUsage(any()) } returns StorageUsage()
            every { appStoreService.installerDisplayName(any()) } returns "Unknown"
            every { appStoreService.existsInAppStore(any(), any()) } returns null

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
            assertEquals(lastUsedTime, result[0].lastUsed)
        }

    @Test
    fun `given apps with permissions, when loadApps called, permission counts are populated`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.app")
            val packageInfo =
                createPackageInfo("1.0.0").apply {
                    requestedPermissions = arrayOf("android.permission.INTERNET", "android.permission.CAMERA")
                    requestedPermissionsFlags = intArrayOf(2, 0)
                }

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getLaunchIntentForPackage(any()) } returns mockk()
            every { packageService.getPackageInfo(any()) } returns packageInfo
            every { packageService.loadLabel(any()) } returns "Test App"
            every { packageService.getInstallerPackageName(any()) } returns null
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()
            every { storageService.getStorageUsage(any()) } returns StorageUsage()
            every { appStoreService.installerDisplayName(any()) } returns "Unknown"
            every { appStoreService.existsInAppStore(any(), any()) } returns null

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
            assertEquals(2, result[0].requestedPermissionsCount)
            assertEquals(1, result[0].grantedPermissionsCount)
        }

    @Test
    fun `given reload true, when loadApps called, then usage stats are reloaded`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.app")
            val packageInfo = createPackageInfo("1.0.0")

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getLaunchIntentForPackage(any()) } returns mockk()
            every { packageService.getPackageInfo(any()) } returns packageInfo
            every { packageService.loadLabel(any()) } returns "Test App"
            every { packageService.getInstallerPackageName(any()) } returns null
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()
            every { storageService.getStorageUsage(any()) } returns StorageUsage()
            every { appStoreService.installerDisplayName(any()) } returns "Unknown"
            every { appStoreService.existsInAppStore(any(), any()) } returns null

            repository
                .loadApps(
                    field = AppInfoField.VERSION,
                    showSystemApps = false,
                    descending = false,
                    reload = true,
                ).toList()

            verify { usageStatsService.getLastUsedEpochs(true) }
        }

    @Test
    fun `given apps, when loadApps called, then emits basic info first then detailed info`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.app")
            val packageInfo = createPackageInfo("1.0.0")

            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            every { packageService.getLaunchIntentForPackage(any()) } returns mockk()
            every { packageService.getPackageInfo(any()) } returns packageInfo
            every { packageService.loadLabel(any()) } returns "Test App"
            every { packageService.getInstallerPackageName(any()) } returns null
            every { usageStatsService.getLastUsedEpochs(any()) } returns emptyMap()
            every { storageService.getStorageUsage(any()) } returns StorageUsage()
            every { appStoreService.installerDisplayName(any()) } returns "Unknown"
            every { appStoreService.existsInAppStore(any(), any()) } returns null

            val flow =
                repository.loadApps(
                    field = AppInfoField.VERSION,
                    showSystemApps = false,
                    descending = false,
                    reload = false,
                )

            val results = flow.toList()

            assertEquals(2, results.size)

            // First emission: Basic info (versionName should be empty as defined in mapToAppBasic)
            val firstEmission = results[0]
            assertEquals(1, firstEmission.size)
            assertEquals("", firstEmission[0].versionName)

            // Second emission: Detailed info
            val secondEmission = results[1]
            assertEquals(1, secondEmission.size)
            assertEquals("1.0.0", secondEmission[0].versionName)
        }

    @Test
    fun `given detailed info fetch fails, when loadApps called, then app remains in list with basic info`() =
        runTest {
            val appInfo = createApplicationInfo("com.test.error")
            every { packageService.getInstalledApplications(any<Long>()) } returns listOf(appInfo)
            // returning valid basic info
            every { packageService.loadLabel(any()) } returns "Error App"
            every { packageService.getLaunchIntentForPackage(any()) } returns mockk()
            // failing detailed info
            every { packageService.getPackageInfo(any()) } throws RuntimeException("Package Error")

            val flow = repository.loadApps(
                field = AppInfoField.VERSION,
                showSystemApps = false, 
                descending = false,
                reload = false
            )
            
            val result = flow.toList()
            val finalDocs = result.last()

            assertEquals(1, finalDocs.size)
            assertEquals("com.test.error", finalDocs[0].packageName)
            // Verify we got the basic info back
            assertEquals("Error App", finalDocs[0].name)
        }

    private fun createApplicationInfo(
        packageName: String,
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
}
