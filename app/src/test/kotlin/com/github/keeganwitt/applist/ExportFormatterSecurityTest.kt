package com.github.keeganwitt.applist

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExportFormatterSecurityTest {
    private val formatter = ExportFormatter()

    @Test
    fun `given apps with formula-triggering characters, when writeCsv called, then return sanitized csv`() {
        // Given
        val apps = listOf(
            createTestApp(packageName = "com.example.pkg", name = "=SUM(1,2)"),
            createTestApp(packageName = "+formula", name = "Normal"),
            createTestApp(packageName = "com.normal", name = "-something"),
            createTestApp(packageName = "@at", name = "Normal"),
            createTestApp(packageName = "com.newline", name = "\n=SUM(1,2)")
        )

        // When
        val result = formatter.toCsv(apps, includeUsageStats = false)

        // Then
        assertTrue("CSV should sanitize name starting with =", result.contains("\"'=SUM(1,2)\""))
        assertTrue("CSV should sanitize packageName starting with +", result.contains("\"'+formula\""))
        assertTrue("CSV should sanitize name starting with -", result.contains("\"'-something\""))
        assertTrue("CSV should sanitize packageName starting with @", result.contains("\"'@at\""))
        assertTrue("CSV should sanitize name starting with \\n", result.contains("\"'\n=SUM(1,2)\""))
    }

    @Test
    fun `given apps with formula-triggering characters, when writeTsv called, then return sanitized tsv`() {
        // Given
        val apps = listOf(
            createTestApp(packageName = "com.example.pkg", name = "=SUM(1,2)"),
            createTestApp(packageName = "+formula", name = "Normal"),
            createTestApp(packageName = "com.normal", name = "-something"),
            createTestApp(packageName = "@at", name = "Normal"),
            createTestApp(packageName = "com.newline", name = "\n=SUM(1,2)")
        )

        // When
        val result = formatter.toTsv(apps, includeUsageStats = false)

        // Then
        assertTrue("TSV should sanitize name starting with =", result.contains("'=SUM(1,2)\t"))
        assertTrue("TSV should sanitize packageName starting with +", result.contains("\t'+formula\t"))
        assertTrue("TSV should sanitize name starting with -", result.contains("'-something\t"))
        assertTrue("TSV should sanitize packageName starting with @", result.contains("\t'@at\t"))
        assertTrue("TSV should sanitize name starting with \\n", result.contains("'\n=SUM(1,2)\t"))
    }

    private fun createTestApp(
        packageName: String,
        name: String,
        versionName: String = "1.0.0",
    ): App =
        App(
            packageName = packageName,
            name = name,
            versionName = versionName,
            archived = false,
            minSdk = 24,
            targetSdk = 33,
            firstInstalled = 123456789L,
            lastUpdated = 123456789L,
            lastUsed = 123456789L,
            sizes = StorageUsage(apkBytes = 1000, appBytes = 2000),
            installerName = "Google Play",
            existsInStore = true,
            grantedPermissionsCount = 5,
            requestedPermissionsCount = 10,
            enabled = true,
            isDetailed = true,
        )
}
