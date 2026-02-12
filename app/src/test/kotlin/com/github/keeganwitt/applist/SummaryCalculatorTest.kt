package com.github.keeganwitt.applist

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryCalculatorTest {
    private val context = mockk<Context>()
    private val calculator = SummaryCalculator(context)

    @Test
    fun `calculate returns correct summaries`() {
        mockStrings()

        val app1 = createApp(enabled = true, archived = false, apkSize = 5 * 1024 * 1024) // Small
        val app2 = createApp(enabled = false, archived = true, apkSize = 20 * 1024 * 1024) // Medium
        val app3 = createApp(enabled = true, archived = false, apkSize = 150 * 1024 * 1024) // Huge

        val result = calculator.calculate(listOf(app1, app2, app3))

        val enabledSummary = result.find { it.field == AppInfoField.ENABLED }
        assertEquals(2, enabledSummary?.buckets?.get("Enabled"))
        assertEquals(1, enabledSummary?.buckets?.get("Disabled"))

        val archivedSummary = result.find { it.field == AppInfoField.ARCHIVED }
        assertEquals(1, archivedSummary?.buckets?.get("Archived"))
        assertEquals(2, archivedSummary?.buckets?.get("Installed"))

        val sizeSummary = result.find { it.field == AppInfoField.APK_SIZE }
        assertEquals(1, sizeSummary?.buckets?.get("Small"))
        assertEquals(1, sizeSummary?.buckets?.get("Medium"))
        assertEquals(0, sizeSummary?.buckets?.get("Large"))
        assertEquals(1, sizeSummary?.buckets?.get("Huge"))
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
    }

    private fun createApp(enabled: Boolean, archived: Boolean, apkSize: Long): App {
        val sizes = StorageUsage()
        sizes.apkBytes = apkSize
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
            enabled = enabled
        )
    }
}
