package com.github.keeganwitt.applist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.StringWriter

@RunWith(RobolectricTestRunner::class)
class ExportFormatterTest {
    private val formatter = ExportFormatter()

    @Test
    fun `given empty list, when writeXml called, then return empty xml`() {
        // Given
        val apps = emptyList<App>()

        // When
        val sw = StringWriter()
        formatter.writeXml(sw, apps, includeUsageStats = false)
        val result = sw.toString()

        // Then
        assertTrue(result.contains("<?xml version='1.0' encoding='UTF-8' ?>"))
        assertTrue(result.contains("<apps>\n</apps>\n"))
    }

    @Test
    fun `given list of apps, when writeXml called, then return correct xml with all fields`() {
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
        val sw = StringWriter()
        formatter.writeXml(sw, apps, includeUsageStats = false)
        val result = sw.toString()

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
        val sw = StringWriter()
        formatter.writeCsv(sw, apps, includeUsageStats = true)
        val result = sw.toString()

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
        val sw = StringWriter()
        formatter.writeCsv(sw, apps, includeUsageStats = false)
        val result = sw.toString()

        // Then
        val header = result.trim().split("\n")[0]
        assertTrue(header.contains("VERSION"))
        assertTrue(!header.contains("APP_SIZE"))
    }

    @Test
    fun `given list of apps, when writeTsv called, then return wide format tsv`() {
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
        val sw = StringWriter()
        formatter.writeTsv(sw, apps, includeUsageStats = false)
        val result = sw.toString()

        // Then
        val lines = result.trim().split("\n")
        val header = lines[0]
        assertTrue(header.startsWith("App Name\tPackage Name\t"))
        assertTrue(header.contains("VERSION"))

        val data = lines[1]
        assertTrue(data.startsWith("App 1\tcom.example.app1\t"))
    }

    @Test
    fun `given list of apps, when writeHtml called, then return html with all fields`() {
        // Given
        val apps = listOf(createTestApp("com.example.app1", "App 1"))

        // When
        val sw = StringWriter()
        formatter.writeHtml(sw, apps, includeUsageStats = false)
        val result = sw.toString()

        // Then
        assertTrue(result.contains("App 1"))
        assertTrue(result.contains("com.example.app1"))
        assertTrue(result.contains("<b>VERSION:</b>"))
    }

    @Test
    fun `given apps with special characters, when writeCsv called, then return escaped csv`() {
        // Given
        val apps = listOf(createTestApp("com.example,pkg", "App \"Name\""))

        // When
        val sw = StringWriter()
        formatter.writeCsv(sw, apps, includeUsageStats = false)
        val result = sw.toString()

        // Then
        assertTrue(result.contains("\"App \"\"Name\"\"\""))
        assertTrue(result.contains("\"com.example,pkg\""))
    }

    @Test
    fun `given apps with special characters, when writeXml called, then return escaped xml`() {
        // Given
        val apps = listOf(createTestApp(packageName = "com.example.pkg", name = "App <Name> & \"More\""))

        // When
        val sw = StringWriter()
        formatter.writeXml(sw, apps, includeUsageStats = false)
        val result = sw.toString()

        // Then
        // XmlSerializer doesn't escape quotes in text content
        assertTrue(result.contains("<appName>App &lt;Name&gt; &amp; \"More\"</appName>"))
    }

    @Test
    fun `given apps with special characters, when writeHtml called, then return escaped html`() {
        // Given
        val apps = listOf(createTestApp(packageName = "com.example.pkg", name = "App <Name> & \"More\" '"))

        // When
        val sw = StringWriter()
        formatter.writeHtml(sw, apps, includeUsageStats = false)
        val result = sw.toString()

        // Then
        // TextUtils.htmlEncode escapes quotes and apostrophes
        assertTrue(result.contains("App &lt;Name&gt; &amp; &quot;More&quot; &#39;"))
    }

    @Test
    fun `given apps with tabs and newlines, when writeTsv called, then return escaped tsv`() {
        // Given
        val apps =
            listOf(
                createTestApp(
                    packageName = "com.example.pkg",
                    name = "App\tName\nWith\rNewlines and \\ backslash",
                ),
            )

        // When
        val sw = StringWriter()
        formatter.writeTsv(sw, apps, includeUsageStats = false)
        val result = sw.toString()

        // Then
        val lines = result.trim().split("\n")
        val dataParts = lines[1].split("\t")

        // App Name should be escaped
        assertEquals("App\\tName\\nWith\\rNewlines and \\\\ backslash", dataParts[0])
    }

    @Test
    fun `when write called with XML, then delegate to writeXml`() {
        val apps = listOf(createTestApp("com.example.app", "App"))
        val writer = StringWriter()
        formatter.write(ExportFormat.XML, writer, apps, false, "")
        assertTrue(writer.toString().contains("<apps>"))
    }

    @Test
    fun `when write called with CSV, then delegate to writeCsv`() {
        val apps = listOf(createTestApp("com.example.app", "App"))
        val writer = StringWriter()
        formatter.write(ExportFormat.CSV, writer, apps, false, "")
        assertTrue(writer.toString().contains("App Name,Package Name"))
    }

    @Test
    fun `when write called with TSV, then delegate to writeTsv`() {
        val apps = listOf(createTestApp("com.example.app", "App"))
        val writer = StringWriter()
        formatter.write(ExportFormat.TSV, writer, apps, false, "")
        assertTrue(writer.toString().contains("App Name\tPackage Name"))
    }

    @Test
    fun `when write called with HTML, then delegate to writeHtml`() {
        val apps = listOf(createTestApp("com.example.app", "App"))
        val writer = StringWriter()
        formatter.write(ExportFormat.HTML, writer, apps, false, "")
        assertTrue(writer.toString().contains("<!DOCTYPE html>"))
    }

    @Test
    fun `given empty list, when writeHtml called, then return html with no apps`() {
        val sw = StringWriter()
        formatter.writeHtml(sw, emptyList(), includeUsageStats = false)
        val result = sw.toString()
        assertTrue(result.contains("<div class=\"app-grid\">\n</div>"))
    }

    @Test
    fun `given empty list, when writeCsv called, then return header only`() {
        val sw = StringWriter()
        formatter.writeCsv(sw, emptyList(), includeUsageStats = false)
        val result = sw.toString()
        assertTrue(result.startsWith("App Name,Package Name"))
        assertEquals(1, result.trim().split("\n").size)
    }

    @Test
    fun `given empty list, when writeTsv called, then return header only`() {
        val sw = StringWriter()
        formatter.writeTsv(sw, emptyList(), includeUsageStats = false)
        val result = sw.toString()
        assertTrue(result.startsWith("App Name\tPackage Name"))
        assertEquals(1, result.trim().split("\n").size)
    }

    @Test
    fun `given app with failed fields, when writeXml called, then return loadingFailedValue`() {
        // Given
        val app = createTestApp("com.pkg", "App").copy(failedFields = setOf(AppInfoField.APK_SIZE))
        val loadingFailedValue = "FAIL"

        // When
        val sw = StringWriter()
        formatter.writeXml(sw, listOf(app), includeUsageStats = false, loadingFailedValue = loadingFailedValue)
        val result = sw.toString()

        // Then
        assertTrue(result.contains("<APK_SIZE>$loadingFailedValue</APK_SIZE>"))
    }

    @Test
    fun `given includeUsageStats true, when writeXml called, then include usage fields`() {
        val apps = listOf(createTestApp("com.pkg", "App"))
        val sw = StringWriter()
        formatter.writeXml(sw, apps, includeUsageStats = true)
        val result = sw.toString()
        assertTrue(result.contains("<APP_SIZE>"))
    }

    @Test
    fun `given includeUsageStats true, when writeHtml called, then include usage fields`() {
        val apps = listOf(createTestApp("com.pkg", "App"))
        val sw = StringWriter()
        formatter.writeHtml(sw, apps, includeUsageStats = true)
        val result = sw.toString()
        assertTrue(result.contains("<b>APP_SIZE:</b>"))
    }

    @Test
    fun `given includeUsageStats true, when writeTsv called, then include usage fields`() {
        val apps = listOf(createTestApp("com.pkg", "App"))
        val sw = StringWriter()
        formatter.writeTsv(sw, apps, includeUsageStats = true)
        val result = sw.toString()
        assertTrue(result.contains("\tAPP_SIZE"))
    }

    @Test
    fun `given app with failed fields, when writeHtml called, then return loadingFailedValue`() {
        val app = createTestApp("com.pkg", "App").copy(failedFields = setOf(AppInfoField.APK_SIZE))
        val loadingFailedValue = "FAIL"
        val sw = StringWriter()
        formatter.writeHtml(sw, listOf(app), includeUsageStats = false, loadingFailedValue = loadingFailedValue)
        val result = sw.toString()
        assertTrue(result.contains("<b>APK_SIZE:</b> FAIL"))
    }

    @Test
    fun `given app with failed fields, when writeCsv called, then return loadingFailedValue`() {
        val app = createTestApp("com.pkg", "App").copy(failedFields = setOf(AppInfoField.APK_SIZE))
        val loadingFailedValue = "FAIL"
        val sw = StringWriter()
        formatter.writeCsv(sw, listOf(app), includeUsageStats = false, loadingFailedValue = loadingFailedValue)
        val result = sw.toString()
        assertTrue(result.contains(",\"FAIL\""))
    }

    @Test
    fun `given app with failed fields, when writeTsv called, then return loadingFailedValue`() {
        val app = createTestApp("com.pkg", "App").copy(failedFields = setOf(AppInfoField.APK_SIZE))
        val loadingFailedValue = "FAIL"
        val sw = StringWriter()
        formatter.writeTsv(sw, listOf(app), includeUsageStats = false, loadingFailedValue = loadingFailedValue)
        val result = sw.toString()
        assertTrue(result.contains("\tFAIL"))
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
