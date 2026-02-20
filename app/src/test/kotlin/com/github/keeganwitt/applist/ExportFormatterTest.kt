package com.github.keeganwitt.applist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportFormatterTest {
    private val formatter = ExportFormatter()

    @Test
    fun `given empty list, when toXml called, then return empty xml`() {
        // Given
        val apps = emptyList<App>()

        // When
        val result = formatter.toXml(apps, includeUsageStats = false)

        // Then
        val expected =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<apps>\n" +
                "</apps>\n"
        assertEquals(expected, result)
    }

    @Test
    fun `given list of apps, when toXml called, then return correct xml with all fields`() {
        // Given
        val apps =
            listOf(
                createTestApp(
                    packageName = "com.example.app1",
                    name = "App 1",
                    versionName = "1.0",
                ),
            )

        // When
        val result = formatter.toXml(apps, includeUsageStats = false)

        // Then
        assertTrue(result.contains("<appName>App 1</appName>"))
        assertTrue(result.contains("<packageName>com.example.app1</packageName>"))
        assertTrue(result.contains("<VERSION>1.0</VERSION>"))
        assertTrue(result.contains("<APK_SIZE>"))
        // Check that usage stats field is NOT there when includeUsageStats is false
        assertTrue(!result.contains("<APP_SIZE>"))
    }

    @Test
    fun `given list of apps, when writeCsv called, then return wide format csv`() {
        // Given
        val apps =
            listOf(
                createTestApp(
                    packageName = "com.example.app1",
                    name = "App 1",
                    versionName = "1.0",
                ),
            )

        // When
        val result = formatter.toCsv(apps, includeUsageStats = true)

        // Then
        val lines = result.trim().split("\n")
        val header = lines[0]
        assertTrue(header.startsWith("App Name,Package Name,"))
        assertTrue(header.contains("VERSION"))
        assertTrue(header.contains("APK_SIZE"))
        assertTrue(header.contains("APP_SIZE")) // Should be there because includeUsageStats = true

        val data = lines[1]
        assertTrue(data.startsWith("\"App 1\",\"com.example.app1\""))
    }

    @Test
    fun `given list of apps, when writeCsv called with includeUsageStats false, then omit those columns`() {
        // Given
        val apps = listOf(createTestApp("com.example.app1", "App 1"))

        // When
        val result = formatter.toCsv(apps, includeUsageStats = false)

        // Then
        val header = result.trim().split("\n")[0]
        assertTrue(header.contains("VERSION"))
        assertTrue(!header.contains("APP_SIZE"))
    }

    @Test
    fun `given list of apps, when toTsv called, then return wide format tsv`() {
        // Given
        val apps =
            listOf(
                createTestApp(
                    packageName = "com.example.app1",
                    name = "App 1",
                    versionName = "1.0",
                ),
            )

        // When
        val result = formatter.toTsv(apps, includeUsageStats = false)

        // Then
        val lines = result.trim().split("\n")
        val header = lines[0]
        assertTrue(header.startsWith("App Name\tPackage Name\t"))
        assertTrue(header.contains("VERSION"))

        val data = lines[1]
        assertTrue(data.startsWith("App 1\tcom.example.app1\t"))
    }

    @Test
    fun `given list of apps, when toHtml called, then return html with all fields`() {
        // Given
        val apps = listOf(createTestApp("com.example.app1", "App 1"))

        // When
        val result = formatter.toHtml(apps, includeUsageStats = false)

        // Then
        assertTrue(result.contains("App 1"))
        assertTrue(result.contains("com.example.app1"))
        assertTrue(result.contains("<b>VERSION:</b>"))
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
