package com.github.keeganwitt.applist

import android.content.Context
import com.github.keeganwitt.applist.services.AppStoreService
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class SummaryCalculatorTest {
    private val context = mockk<Context>()
    private val store = mockk<AppStoreService>()
    private val calculator = SummaryCalculator(context, store)

    @Test
    fun `calculate returns correct enabled summary`() {
        mockStrings()
        val app1 = createApp(enabled = true, archived = false, apkSize = 0)
        val app2 = createApp(enabled = false, archived = false, apkSize = 0)

        val result = calculator.calculate(listOf(app1, app2), AppInfoField.ENABLED)

        assertEquals(2, result?.buckets?.size)
        assertEquals(1, result?.buckets?.get("Enabled"))
        assertEquals(1, result?.buckets?.get("Disabled"))
    }

    @Test
    fun `calculate returns correct archived summary with Unknown`() {
        mockStrings()
        val app1 = createApp(enabled = true, archived = false, apkSize = 0)
        val app2 = createApp(enabled = true, archived = true, apkSize = 0)
        val app3 = createApp(enabled = true, archived = null, apkSize = 0)

        val result = calculator.calculate(listOf(app1, app2, app3), AppInfoField.ARCHIVED)

        assertEquals(3, result?.buckets?.size)
        assertEquals(1, result?.buckets?.get("Installed"))
        assertEquals(1, result?.buckets?.get("Archived"))
        assertEquals(1, result?.buckets?.get("Unknown"))
    }

    @Test
    fun `calculate returns correct exists in app store summary with Unknown`() {
        mockStrings()
        val app1 = createApp(enabled = true, archived = false, apkSize = 0).copy(existsInStore = true)
        val app2 = createApp(enabled = true, archived = false, apkSize = 0).copy(existsInStore = false)
        val app3 = createApp(enabled = true, archived = false, apkSize = 0).copy(existsInStore = null)

        val result = calculator.calculate(listOf(app1, app2, app3), AppInfoField.EXISTS_IN_APP_STORE)

        assertEquals(3, result?.buckets?.size)
        assertEquals(1, result?.buckets?.get("True"))
        assertEquals(1, result?.buckets?.get("False"))
        assertEquals(1, result?.buckets?.get("Unknown"))
    }

    @Test
    fun `calculateSdkSummary groups by sdk version including Unknown`() {
        mockStrings()
        val app1 = createApp(enabled = true, archived = false, apkSize = 0).copy(targetSdk = 33)
        val app2 = createApp(enabled = true, archived = false, apkSize = 0).copy(targetSdk = null)

        val result = calculator.calculate(listOf(app1, app2), AppInfoField.TARGET_SDK)

        println("Buckets: ${result?.buckets}")
        // groupingBy { it?.toString() ?: unknown }.eachCount() only includes present buckets
        assertEquals(2, result?.buckets?.size)
        assertEquals(1, result?.buckets?.get("33"))
        assertEquals(1, result?.buckets?.get("Unknown"))
    }

    @Test
    fun `calculatePermissionSummary buckets correctly including Unknown`() {
        mockStrings()
        val app0 = createApp(enabled = true, archived = false, apkSize = 0).copy(grantedPermissionsCount = 0)
        val appNull = createApp(enabled = true, archived = false, apkSize = 0).copy(grantedPermissionsCount = null)

        val result = calculator.calculate(listOf(app0, appNull), AppInfoField.GRANTED_PERMISSIONS)

        // orderedBuckets initializes 5 default buckets + Unknown (if not empty)
        assertEquals(6, result?.buckets?.size)
        assertEquals(1, result?.buckets?.get("None"))
        assertEquals(1, result?.buckets?.get("Unknown"))
        assertEquals(0, result?.buckets?.get("Few"))
    }

    @Test
    fun `calculateDateSummary buckets correctly including Unknown`() {
        mockStrings()
        val now = System.currentTimeMillis()
        val app1 = createApp(enabled = true, archived = false, apkSize = 0).copy(firstInstalled = now)
        val appNull = createApp(enabled = true, archived = false, apkSize = 0).copy(firstInstalled = null)
        val appZero = createApp(enabled = true, archived = false, apkSize = 0).copy(firstInstalled = 0L)

        val result = calculator.calculate(listOf(app1, appNull, appZero), AppInfoField.FIRST_INSTALLED)

        // orderedBuckets initializes 4 default buckets + Unknown (if not empty)
        assertEquals(5, result?.buckets?.size)
        assertEquals(1, result?.buckets?.get("Last month"))
        assertEquals(2, result?.buckets?.get("Unknown"))
        assertEquals(0, result?.buckets?.get("Older"))
    }

    @Test
    fun `calculateSizeSummary includes 0 in Small bucket`() {
        mockStrings()
        val app0 = createApp(enabled = true, archived = false, apkSize = 0)
        val result = calculator.calculate(listOf(app0), AppInfoField.APK_SIZE)

        assertEquals(1, result?.buckets?.get("Small"))
    }

    @Test
    fun `calculatePackageManagerSummary groups correctly`() {
        mockStrings()
        every { store.installerDisplayName("store1") } returns "Store 1"
        val app1 = createApp(enabled = true, archived = false, apkSize = 0).copy(installerName = "store1")
        val result = calculator.calculate(listOf(app1), AppInfoField.PACKAGE_MANAGER)
        assertEquals(1, result?.buckets?.get("Store 1"))
    }

    @Test
    fun `calculate covers all fields`() {
        mockStrings()
        val app = createApp(enabled = true, archived = true, apkSize = 100)
        AppInfoField.entries.forEach { field ->
            val result = calculator.calculate(listOf(app), field)
            if (field == AppInfoField.VERSION) {
                assertNull(result)
            } else {
                assertTrue("Result for $field should not be empty", result?.buckets?.isNotEmpty() ?: false)
            }
        }
    }

    @Test
    fun `calculateSdkSummary sorting works as expected`() {
        mockStrings()
        val app1 = createApp(enabled = true, archived = false, apkSize = 0).copy(targetSdk = 33)
        val app2 = createApp(enabled = true, archived = false, apkSize = 0).copy(targetSdk = 21)
        val app3 = createApp(enabled = true, archived = false, apkSize = 0).copy(targetSdk = null)

        val result = calculator.calculate(listOf(app1, app2, app3), AppInfoField.TARGET_SDK)

        val keys = result?.buckets?.keys?.toList()
        assertEquals(3, keys?.size)
        // Order should be descending: 33, 21, Unknown
        assertEquals("33", keys?.get(0))
        assertEquals("21", keys?.get(1))
        assertEquals("Unknown", keys?.get(2))
    }

    @Test
    fun `calculatePermissionSummary removes Unknown if empty`() {
        mockStrings()
        val app = createApp(enabled = true, archived = false, apkSize = 0).copy(grantedPermissionsCount = 1)
        val result = calculator.calculate(listOf(app), AppInfoField.GRANTED_PERMISSIONS)
        assertFalse(result?.buckets?.containsKey("Unknown") ?: true)
    }

    @Test
    fun `calculateDateSummary handled Older bucket and removes Unknown if empty`() {
        mockStrings()
        val app = createApp(enabled = true, archived = false, apkSize = 0).copy(firstInstalled = 1L) // Very old
        val result = calculator.calculate(listOf(app), AppInfoField.FIRST_INSTALLED)
        assertEquals(1, result?.buckets?.get("Older"))
        assertFalse(result?.buckets?.containsKey("Unknown") ?: true)
    }

    @Test
    fun `calculateDateSummary covers 3 and 6 month buckets and unknown bucket retains`() {
        mockStrings()
        val now = Instant.now()
        val app3m =
            createApp(
                enabled = true,
                archived = false,
                apkSize = 0,
            ).copy(firstInstalled = now.minus(40, ChronoUnit.DAYS).toEpochMilli())
        val app6m =
            createApp(
                enabled = true,
                archived = false,
                apkSize = 0,
            ).copy(firstInstalled = now.minus(120, ChronoUnit.DAYS).toEpochMilli())
        val appUnk = createApp(enabled = true, archived = false, apkSize = 0).copy(firstInstalled = 0L)
        val result = calculator.calculate(listOf(app3m, app6m, appUnk), AppInfoField.FIRST_INSTALLED)
        assertEquals(1, result?.buckets?.get("Last 3 months"))
        assertEquals(1, result?.buckets?.get("Last 6 months"))
        assertEquals(1, result?.buckets?.get("Unknown"))
    }

    @Test
    fun `calculatePermissionSummary covers some many lots buckets and keeps unknown`() {
        mockStrings()
        val appSome = createApp(enabled = true, archived = false, apkSize = 0).copy(grantedPermissionsCount = 7)
        val appMany = createApp(enabled = true, archived = false, apkSize = 0).copy(grantedPermissionsCount = 15)
        val appLots = createApp(enabled = true, archived = false, apkSize = 0).copy(grantedPermissionsCount = 25)
        val appUnk = createApp(enabled = true, archived = false, apkSize = 0).copy(grantedPermissionsCount = null)
        val result = calculator.calculate(listOf(appSome, appMany, appLots, appUnk), AppInfoField.GRANTED_PERMISSIONS)
        assertEquals(1, result?.buckets?.get("Some"))
        assertEquals(1, result?.buckets?.get("Many"))
        assertEquals(1, result?.buckets?.get("Lots"))
        assertEquals(1, result?.buckets?.get("Unknown"))
    }

    @Test
    fun `calculateSizeSummary covers medium large huge buckets`() {
        mockStrings()
        val appMedium = createApp(enabled = true, archived = false, apkSize = 20L * 1024 * 1024)
        val appLarge = createApp(enabled = true, archived = false, apkSize = 70L * 1024 * 1024)
        val appHuge = createApp(enabled = true, archived = false, apkSize = 150L * 1024 * 1024)
        val result = calculator.calculate(listOf(appMedium, appLarge, appHuge), AppInfoField.TOTAL_SIZE)
        assertEquals(1, result?.buckets?.get("Medium"))
        assertEquals(1, result?.buckets?.get("Large"))
        assertEquals(1, result?.buckets?.get("Huge"))
    }

    private fun mockStrings() {
        every { store.installerDisplayName(any()) } returns "Store"
        every { context.getString(any()) } returns "String"
        every { context.getString(R.string.enabled) } returns "Enabled"
        every { context.getString(R.string.disabled) } returns "Disabled"
        every { context.getString(R.string.archived) } returns "Archived"
        every { context.getString(R.string.installed) } returns "Installed"
        every { context.getString(R.string.boolean_true) } returns "True"
        every { context.getString(R.string.boolean_false) } returns "False"
        every { context.getString(R.string.unknown) } returns "Unknown"

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
        archived: Boolean?,
        apkSize: Long,
    ): App {
        val sizes = StorageUsage(appBytes = apkSize, apkBytes = apkSize)
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
