package com.github.keeganwitt.applist.services

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PackageServiceTest {
    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var service: AndroidPackageService

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
        every { context.packageManager } returns packageManager
        service = AndroidPackageService(context)
    }

    @Test
    fun `given installed apps, when getInstalledApplications called, then returns list of apps`() {
        val appInfo1 = ApplicationInfo().apply { packageName = "com.test.app1" }
        val packageInfo1 = PackageInfo().apply { applicationInfo = appInfo1 }
        val appInfo2 = ApplicationInfo().apply { packageName = "com.test.app2" }
        val packageInfo2 = PackageInfo().apply { applicationInfo = appInfo2 }
        val packages = listOf(packageInfo1, packageInfo2)

        every { packageManager.getInstalledPackages(any<PackageManager.PackageInfoFlags>()) } returns packages

        val result = service.getInstalledApplications(PackageManager.GET_META_DATA.toLong())

        assertEquals(2, result.size)
        assertEquals("com.test.app1", result[0].packageName)
        assertEquals("com.test.app2", result[1].packageName)
    }

    @Test
    fun `given launchable app, when getLaunchIntentForPackage called, then returns intent`() {
        val intent = mockk<Intent>()
        every { packageManager.getLaunchIntentForPackage("com.test.app") } returns intent

        val result = service.getLaunchIntentForPackage("com.test.app")

        assertNotNull(result)
    }

    @Test
    fun `given non-launchable app, when getLaunchIntentForPackage called, then returns null`() {
        every { packageManager.getLaunchIntentForPackage("com.test.app") } returns null

        val result = service.getLaunchIntentForPackage("com.test.app")

        assertNull(result)
    }

    @Test
    fun `given application info, when loadLabel called, then returns app label`() {
        val appInfo = mockk<ApplicationInfo>()
        every { appInfo.loadLabel(packageManager) } returns "Test App"

        val result = service.loadLabel(appInfo)

        assertEquals("Test App", result)
    }

    @Test
    fun `given application info, when getPackageInfo called, then returns package info`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val packageInfo = PackageInfo().apply { versionName = "1.0.0" }
        every { packageManager.getPackageInfo(eq("com.test.app"), any<PackageManager.PackageInfoFlags>()) } returns packageInfo

        val result = service.getPackageInfo(appInfo)

        assertEquals("1.0.0", result.versionName)
    }

    @Test(expected = PackageManager.NameNotFoundException::class)
    fun `given invalid package, when getPackageInfo called, then throws exception`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.invalid.app" }
        every { packageManager.getPackageInfo(eq("com.invalid.app"), any<PackageManager.PackageInfoFlags>()) } throws PackageManager.NameNotFoundException()

        service.getPackageInfo(appInfo)
    }

    @Test
    @Config(sdk = [34])
    fun `given valid package, when getApplicationIcon called, then returns drawable`() {
        val drawable = mockk<Drawable>()
        every { packageManager.getApplicationIcon("com.test.app") } returns drawable

        val result = service.getApplicationIcon("com.test.app")

        assertNotNull(result)
    }

    @Test
    @Config(sdk = [34])
    fun `given invalid package, when getApplicationIcon called, then returns null`() {
        every { packageManager.getApplicationIcon("com.invalid.app") } throws PackageManager.NameNotFoundException()

        val result = service.getApplicationIcon("com.invalid.app")

        assertNull(result)
    }

    @Test
    fun `getPackageInfo on O does not include signatures flag`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val packageInfo = PackageInfo()
        val flagsSlot = slot<PackageManager.PackageInfoFlags>()

        every { packageManager.getPackageInfo(eq("com.test.app"), capture(flagsSlot)) } returns packageInfo

        service.getPackageInfo(appInfo)

        val flags = flagsSlot.captured.getValue()
        assertTrue("Expected GET_SIGNATURES flag to be absent", (flags and PackageManager.GET_SIGNATURES.toLong()) == 0L)
    }

    @Test
    @Config(sdk = [34])
    fun `getPackageInfo on P does not include signing certificates flag`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val packageInfo = PackageInfo()
        val flagsSlot = slot<PackageManager.PackageInfoFlags>()

        every { packageManager.getPackageInfo(eq("com.test.app"), capture(flagsSlot)) } returns packageInfo

        service.getPackageInfo(appInfo)

        val flags = flagsSlot.captured.getValue()
        assertTrue("Expected GET_SIGNING_CERTIFICATES flag to be absent", (flags and PackageManager.GET_SIGNING_CERTIFICATES.toLong()) == 0L)
    }

    @Test
    @Config(sdk = [35])
    fun `given SDK 35 and archived app, when getApplicationIcon called, then returns icon from application info`() {
        val packageName = "com.test.archived"
        val drawable = mockk<Drawable>()
        val appInfo = ApplicationInfo().apply { this.packageName = packageName }

        // Mock getApplicationIcon(packageName) to throw NameNotFoundException
        every { packageManager.getApplicationIcon(packageName) } throws PackageManager.NameNotFoundException()

        // Mock getApplicationInfo with flags to return appInfo
        val flags = PackageManager.MATCH_ARCHIVED_PACKAGES or PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()
        val flagsSlot = slot<PackageManager.ApplicationInfoFlags>()
        every { packageManager.getApplicationInfo(packageName, capture(flagsSlot)) } returns appInfo

        // Mock getApplicationIcon(appInfo) to return drawable
        every { packageManager.getApplicationIcon(appInfo) } returns drawable

        val result = service.getApplicationIcon(packageName)

        assertNotNull(result)
        assertEquals(drawable, result)
        assertEquals(flags, flagsSlot.captured.getValue())
    }

    @Test
    @Config(sdk = [35])
    fun `given SDK 35 and missing app even with archived flags, when getApplicationIcon called, then returns null`() {
        val packageName = "com.test.missing"

        every { packageManager.getApplicationIcon(packageName) } throws PackageManager.NameNotFoundException()
        every { packageManager.getApplicationInfo(packageName, any<PackageManager.ApplicationInfoFlags>()) } throws PackageManager.NameNotFoundException()

        val result = service.getApplicationIcon(packageName)

        assertNull(result)
    }
}
