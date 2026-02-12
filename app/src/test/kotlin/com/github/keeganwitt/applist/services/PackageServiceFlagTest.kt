package com.github.keeganwitt.applist.services

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PackageServiceFlagTest {
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
    @Config(sdk = [Build.VERSION_CODES.O])
    fun `getPackageInfo on O does not include signatures flag`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val packageInfo = PackageInfo()
        val flagsSlot = slot<Int>()

        every { packageManager.getPackageInfo(eq("com.test.app"), capture(flagsSlot)) } returns packageInfo

        service.getPackageInfo(appInfo)

        val flags = flagsSlot.captured.toLong()
        assertTrue("Expected GET_SIGNATURES flag to be absent", (flags and PackageManager.GET_SIGNATURES.toLong()) == 0L)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `getPackageInfo on P does not include signing certificates flag`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val packageInfo = PackageInfo()
        val flagsSlot = slot<Int>()

        every { packageManager.getPackageInfo(eq("com.test.app"), capture(flagsSlot)) } returns packageInfo

        service.getPackageInfo(appInfo)

        val flags = flagsSlot.captured.toLong()
        assertTrue("Expected GET_SIGNING_CERTIFICATES flag to be absent", (flags and PackageManager.GET_SIGNING_CERTIFICATES.toLong()) == 0L)
    }
}
