package com.github.keeganwitt.applist

import android.content.Context
import com.github.keeganwitt.applist.services.AppStoreService
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SummaryCalculatorTest {
    private val context = mockk<Context>()
    private val store = mockk<AppStoreService>()
    private val calculator = SummaryCalculator(context, store)

    @Test
    fun `calculate returns correct enabled summary`() {
        mockStrings()
        val app1 = createApp(enabled = true, archived = false, apkSize = 0)
        val app2 = createApp(enabled = false, archived = false, apkSize = 0)
        val app3 = createApp(enabled = true, archived = false, apkSize = 0)

        val result = calculator.calculate(listOf(app1, app2, app3), AppInfoField.ENABLED)

        assertEquals(AppInfoField.ENABLED, result?.field)
        assertEquals(2, result?.buckets?.get("Enabled"))
        assertEquals(1, result?.buckets?.get("Disabled"))
    }

    @Test
    fun `calculate returns correct archived summary`() {
        mockStrings()
        val app1 = createApp(enabled = true, archived = false, apkSize = 0)
        val app2 = createApp(enabled = true, archived = true, apkSize = 0)

        val result = calculator.calculate(listOf(app1, app2), AppInfoField.ARCHIVED)

        assertEquals(AppInfoField.ARCHIVED, result?.field)
        assertEquals(1, result?.buckets?.get("Archived"))
        assertEquals(1, result?.buckets?.get("Installed"))
    }

    @Test
    fun `calculate returns correct size summary`() {
        mockStrings()
        val app1 = createApp(enabled = true, archived = false, apkSize = 5 * 1024 * 1024) // Small
        val app2 = createApp(enabled = true, archived = false, apkSize = 20 * 1024 * 1024) // Medium
        val app3 = createApp(enabled = true, archived = false, apkSize = 75 * 1024 * 1024) // Large
        val app4 = createApp(enabled = true, archived = false, apkSize = 150 * 1024 * 1024) // Huge

        val result = calculator.calculate(listOf(app1, app2, app3, app4), AppInfoField.APK_SIZE)

        assertEquals(AppInfoField.APK_SIZE, result?.field)
        assertEquals(1, result?.buckets?.get("Small"))
        assertEquals(1, result?.buckets?.get("Medium"))
        assertEquals(1, result?.buckets?.get("Large"))
        assertEquals(1, result?.buckets?.get("Huge"))
    }

    @Test
    fun `calculateSdkSummary groups by sdk version`() {
        mockStrings()
        val app1 = createApp(enabled = true, archived = false, apkSize = 0).copy(targetSdk = 33, minSdk = 24)
        val app2 = createApp(enabled = true, archived = false, apkSize = 0).copy(targetSdk = 33, minSdk = 26)
        val app3 = createApp(enabled = true, archived = false, apkSize = 0).copy(targetSdk = 34, minSdk = 24)

        val targetResult = calculator.calculate(listOf(app1, app2, app3), AppInfoField.TARGET_SDK)
        assertEquals(2, targetResult?.buckets?.get("33"))
        assertEquals(1, targetResult?.buckets?.get("34"))

        val minResult = calculator.calculate(listOf(app1, app2, app3), AppInfoField.MIN_SDK)
        assertEquals(2, minResult?.buckets?.get("24"))
        assertEquals(1, minResult?.buckets?.get("26"))
    }

    @Test
    fun `calculatePermissionSummary buckets correctly`() {
        mockStrings()
        val app0 = createApp(enabled = true, archived = false, apkSize = 0).copy(grantedPermissionsCount = 0)
        val app3 = createApp(enabled = true, archived = false, apkSize = 0).copy(grantedPermissionsCount = 3)
        val app8 = createApp(enabled = true, archived = false, apkSize = 0).copy(grantedPermissionsCount = 8)
        val app15 = createApp(enabled = true, archived = false, apkSize = 0).copy(grantedPermissionsCount = 15)
        val app25 = createApp(enabled = true, archived = false, apkSize = 0).copy(grantedPermissionsCount = 25)

        val result = calculator.calculate(listOf(app0, app3, app8, app15, app25), AppInfoField.GRANTED_PERMISSIONS)

        assertEquals(1, result?.buckets?.get("None")) // 0
        assertEquals(1, result?.buckets?.get("Few")) // 1-5
        assertEquals(1, result?.buckets?.get("Some")) // 6-10
        assertEquals(1, result?.buckets?.get("Many")) // 11-20
        assertEquals(1, result?.buckets?.get("Lots")) // 20+
    }

    @Test
    fun `calculateDateSummary buckets correctly`() {
        mockStrings()
        val now = System.currentTimeMillis()
        val oneMonthAgo = now - 15L * 24 * 60 * 60 * 1000
        val threeMonthsAgo = now - 45L * 24 * 60 * 60 * 1000
        val sixMonthsAgo = now - 120L * 24 * 60 * 60 * 1000
        val older = now - 200L * 24 * 60 * 60 * 1000

        val app1 = createApp(enabled = true, archived = false, apkSize = 0).copy(firstInstalled = oneMonthAgo)
        val app2 = createApp(enabled = true, archived = false, apkSize = 0).copy(firstInstalled = threeMonthsAgo)
        val app3 = createApp(enabled = true, archived = false, apkSize = 0).copy(firstInstalled = sixMonthsAgo)
        val app4 = createApp(enabled = true, archived = false, apkSize = 0).copy(firstInstalled = older)

        val result = calculator.calculate(listOf(app1, app2, app3, app4), AppInfoField.FIRST_INSTALLED)

        assertEquals(1, result?.buckets?.get("Last month"))
        assertEquals(1, result?.buckets?.get("Last 3 months"))
        assertEquals(1, result?.buckets?.get("Last 6 months"))
        assertEquals(1, result?.buckets?.get("Older"))
    }

    @Test
    fun `calculatePackageManagerSummary groups correctly`() {
        mockStrings()
        every { store.installerDisplayName("store1") } returns "Store 1"
        every { store.installerDisplayName("store2") } returns "Store 2"

        val app1 = createApp(enabled = true, archived = false, apkSize = 0).copy(installerName = "store1")
        val app2 = createApp(enabled = true, archived = false, apkSize = 0).copy(installerName = "store1")
        val app3 = createApp(enabled = true, archived = false, apkSize = 0).copy(installerName = "store2")

        val result = calculator.calculate(listOf(app1, app2, app3), AppInfoField.PACKAGE_MANAGER)

        assertEquals(2, result?.buckets?.get("Store 1"))
        assertEquals(1, result?.buckets?.get("Store 2"))
    }

    @Test
    fun `calculate returns null for version field`() {
        val app = createApp(enabled = true, archived = false, apkSize = 0)
        val result = calculator.calculate(listOf(app), AppInfoField.VERSION)
        assertNull(result)
    }

    private fun mockStrings() {
        every { context.getString(any()) } returns "String"
        every { context.getString(R.string.enabled) } returns "Enabled"
        every { context.getString(R.string.disabled) } returns "Disabled"
        every { context.getString(R.string.archived) } returns "Archived"
        every { context.getString(R.string.installed) } returns "Installed"
        every { context.getString(R.string.boolean_true) } returns "True"
        every { context.getString(R.string.boolean_false) } returns "False"

        every { context.getString(R.string.size_bucket_small) } returns "Small"
        every { context.getString(R.string.size_bucket_medium) } returns "Medium"
        every { context.getString(R.string.size_bucket_large) } returns "Large"
        every { context.getString(R.string.size_bucket_huge) } returns "Huge"

        every { context.getString(R.string.perm_bucket_none) } returns "None"
        every { context.getString(R.string.perm_bucket_few) } returns "Few"
        every { context.getString(R.string.perm_bucket_some) } returns "Some"
        every { context.getString(R.string.perm_bucket_many) } returns "Many"
        every { context.getString(R.string.perm_bucket_lots) } returns "Lots"

        every { context.getString(R.string.date_bucket_last_month) } returns "Last month"
        every { context.getString(R.string.date_bucket_last_three_months) } returns "Last 3 months"
        every { context.getString(R.string.date_bucket_last_six_months) } returns "Last 6 months"
        every { context.getString(R.string.date_bucket_older) } returns "Older"
    }

    private fun createApp(
        enabled: Boolean,
        archived: Boolean,
        apkSize: Long,
    ): App {
        val sizes = StorageUsage(apkBytes = apkSize)
        return App(
            packageName = "pkg",
            name = "name",
            versionName = "1.0",
            archived = archived,
            minSdk = 21,
            targetSdk = 31,
            firstInstalled = 0,
            lastUpdated = 0,
            lastUsed = 0,
            sizes = sizes,
            installerName = "installer",
            existsInStore = true,
            grantedPermissionsCount = 0,
            requestedPermissionsCount = 0,
            enabled = enabled,
        )
    }
}
