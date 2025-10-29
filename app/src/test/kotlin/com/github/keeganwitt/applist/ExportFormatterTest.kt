package com.github.keeganwitt.applist

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExportFormatterTest {

    private lateinit var formatter: ExportFormatter

    @Before
    fun setup() {
        formatter = ExportFormatter()
    }

    @Test
    fun `given empty list, when toXml called, then returns valid XML with no apps`() {
        val result = formatter.toXml(emptyList(), AppInfoField.VERSION)

        assertTrue(result.contains("<?xml version"))
        assertTrue(result.contains("<apps>"))
        assertTrue(result.contains("</apps>"))
        assertTrue(!result.contains("<app>"))
    }

    @Test
    fun `given single app, when toXml called, then returns valid XML with app data`() {
        val items = listOf(
            AppItemUiModel(
                packageName = "com.test.app",
                appName = "Test App",
                infoText = "1.0.0"
            )
        )

        val result = formatter.toXml(items, AppInfoField.VERSION)

        assertTrue(result.contains("<apps>"))
        assertTrue(result.contains("<app>"))
        assertTrue(result.contains("<appName>Test App</appName>"))
        assertTrue(result.contains("<appPackage>com.test.app</appPackage>"))
        assertTrue(result.contains("<appInfoType>VERSION</appInfoType>"))
        assertTrue(result.contains("<appInfoValue>1.0.0</appInfoValue>"))
        assertTrue(result.contains("</app>"))
        assertTrue(result.contains("</apps>"))
    }

    @Test
    fun `given multiple apps, when toXml called, then returns valid XML with all apps`() {
        val items = listOf(
            AppItemUiModel(
                packageName = "com.test.app1",
                appName = "Test App 1",
                infoText = "1.0.0"
            ),
            AppItemUiModel(
                packageName = "com.test.app2",
                appName = "Test App 2",
                infoText = "2.0.0"
            )
        )

        val result = formatter.toXml(items, AppInfoField.VERSION)

        assertTrue(result.contains("Test App 1"))
        assertTrue(result.contains("com.test.app1"))
        assertTrue(result.contains("Test App 2"))
        assertTrue(result.contains("com.test.app2"))
    }

    @Test
    fun `given app with special XML characters, when toXml called, then characters are escaped`() {
        val items = listOf(
            AppItemUiModel(
                packageName = "com.test.app",
                appName = "Test & App <tag>",
                infoText = "Version \"1.0\" & 'more'"
            )
        )

        val result = formatter.toXml(items, AppInfoField.VERSION)

        assertTrue(result.contains("&amp;"))
        assertTrue(result.contains("&lt;"))
        assertTrue(result.contains("&gt;"))
        assertTrue(result.contains("&quot;"))
        assertTrue(result.contains("&apos;"))
    }

    @Test
    fun `given empty list, when toHtml called, then returns valid HTML with no apps`() {
        val result = formatter.toHtml(emptyList())

        assertTrue(result.contains("<!DOCTYPE html>"))
        assertTrue(result.contains("<html>"))
        assertTrue(result.contains("<title>App List</title>"))
        assertTrue(result.contains("<div class=\"app-grid\">"))
        assertTrue(result.contains("</html>"))
        assertTrue(!result.contains("<div class=\"app-item\">"))
    }

    @Test
    fun `given single app, when toHtml called, then returns valid HTML with app data`() {
        val items = listOf(
            AppItemUiModel(
                packageName = "com.test.app",
                appName = "Test App",
                infoText = "1.0.0"
            )
        )

        val result = formatter.toHtml(items)

        assertTrue(result.contains("<!DOCTYPE html>"))
        assertTrue(result.contains("<div class=\"app-item\">"))
        assertTrue(result.contains("<div class=\"app-name\">Test App</div>"))
        assertTrue(result.contains("<div class=\"package-name\">com.test.app</div>"))
        assertTrue(result.contains("<div class=\"app-info\">1.0.0</div>"))
    }

    @Test
    fun `given multiple apps, when toHtml called, then returns valid HTML with all apps`() {
        val items = listOf(
            AppItemUiModel(
                packageName = "com.test.app1",
                appName = "Test App 1",
                infoText = "1.0.0"
            ),
            AppItemUiModel(
                packageName = "com.test.app2",
                appName = "Test App 2",
                infoText = "2.0.0"
            )
        )

        val result = formatter.toHtml(items)

        assertTrue(result.contains("Test App 1"))
        assertTrue(result.contains("com.test.app1"))
        assertTrue(result.contains("Test App 2"))
        assertTrue(result.contains("com.test.app2"))
    }

    @Test
    fun `given HTML output, when toHtml called, then contains proper styling`() {
        val items = listOf(
            AppItemUiModel(
                packageName = "com.test.app",
                appName = "Test App",
                infoText = "1.0.0"
            )
        )

        val result = formatter.toHtml(items)

        assertTrue(result.contains("<style>"))
        assertTrue(result.contains("font-family"))
        assertTrue(result.contains("app-grid"))
        assertTrue(result.contains("app-item"))
        assertTrue(result.contains("app-name"))
        assertTrue(result.contains("package-name"))
    }

    @Test
    fun `given HTML output, when toHtml called, then contains viewport meta tag`() {
        val result = formatter.toHtml(emptyList())

        assertTrue(result.contains("<meta name=\"viewport\""))
        assertTrue(result.contains("width=device-width"))
    }

    @Test
    fun `given app with long package name, when toHtml called, then includes word-wrap styling`() {
        val result = formatter.toHtml(emptyList())

        assertTrue(result.contains("word-wrap: break-word"))
    }
}
