
package com.github.keeganwitt.applist

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportFormatterTest {
    private val formatter = ExportFormatter()

    @Test
    fun `given empty list, when toXml called, then return empty xml`() {
        // Given
        val items = emptyList<AppItemUiModel>()

        // When
        val result = formatter.toXml(items, AppInfoField.APK_SIZE)

        // Then
        val expected =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<apps>\n" +
                "</apps>\n"
        assertEquals(expected, result)
    }

    @Test
    fun `given list of apps, when toXml called, then return correct xml`() {
        // Given
        val items =
            listOf(
                AppItemUiModel(
                    packageName = "com.example.app1",
                    appName = "App 1",
                    infoText = "1.0",
                ),
                AppItemUiModel(
                    packageName = "com.example.app2",
                    appName = "App 2",
                    infoText = "2.0",
                ),
            )

        // When
        val result = formatter.toXml(items, AppInfoField.VERSION)

        // Then
        val expected =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<apps>\n" +
                "<app>\n" +
                "<appName>App 1</appName>\n" +
                "<appPackage>com.example.app1</appPackage>\n" +
                "<appInfoType>VERSION</appInfoType>\n" +
                "<appInfoValue>1.0</appInfoValue>\n" +
                "</app>\n" +
                "<app>\n" +
                "<appName>App 2</appName>\n" +
                "<appPackage>com.example.app2</appPackage>\n" +
                "<appInfoType>VERSION</appInfoType>\n" +
                "<appInfoValue>2.0</appInfoValue>\n" +
                "</app>\n" +
                "</apps>\n"
        assertEquals(expected, result)
    }

    @Test
    fun `given list of apps with special characters, when toXml called, then return escaped xml`() {
        // Given
        val items =
            listOf(
                AppItemUiModel(
                    packageName = "com.example.app<1>",
                    appName = "App & 1",
                    infoText = "'1.0\"",
                ),
            )

        // When
        val result = formatter.toXml(items, AppInfoField.VERSION)

        // Then
        val expected =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<apps>\n" +
                "<app>\n" +
                "<appName>App &amp; 1</appName>\n" +
                "<appPackage>com.example.app&lt;1&gt;</appPackage>\n" +
                "<appInfoType>VERSION</appInfoType>\n" +
                "<appInfoValue>&apos;1.0&quot;</appInfoValue>\n" +
                "</app>\n" +
                "</apps>\n"
        assertEquals(expected, result)
    }

    @Test
    fun `given empty list, when toHtml called, then return empty html`() {
        // Given
        val items = emptyList<AppItemUiModel>()

        // When
        val result = formatter.toHtml(items)

        // Then
        val expected =
            "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>App List</title>\n" +
                "<style>\n" +
                "body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n" +
                "h1 { text-align: center; color: #333; }\n" +
                ".app-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; padding: 10px; }\n" +
                ".app-item { background: white; border-radius: 8px; padding: 10px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n" +
                ".app-name { font-weight: bold; margin-top: 8px; word-wrap: break-word; }\n" +
                ".package-name { font-style: italic; font-size: 0.9em; color: #666; margin-top: 4px; word-wrap: break-word; }\n" +
                ".app-info { margin-top: 4px; color: #333; word-wrap: break-word; }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<h1>App List</h1>\n" +
                "<div class=\"app-grid\">\n" +
                "</div>\n" +
                "</body>\n" +
                "</html>\n"
        assertEquals(expected, result)
    }

    @Test
    fun `given list of apps, when toHtml called, then return correct html`() {
        // Given
        val items =
            listOf(
                AppItemUiModel(
                    packageName = "com.example.app1",
                    appName = "App 1",
                    infoText = "1.0",
                ),
                AppItemUiModel(
                    packageName = "com.example.app2",
                    appName = "App 2",
                    infoText = "2.0",
                ),
            )

        // When
        val result = formatter.toHtml(items)

        // Then
        val expected =
            "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>App List</title>\n" +
                "<style>\n" +
                "body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n" +
                "h1 { text-align: center; color: #333; }\n" +
                ".app-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; padding: 10px; }\n" +
                ".app-item { background: white; border-radius: 8px; padding: 10px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n" +
                ".app-name { font-weight: bold; margin-top: 8px; word-wrap: break-word; }\n" +
                ".package-name { font-style: italic; font-size: 0.9em; color: #666; margin-top: 4px; word-wrap: break-word; }\n" +
                ".app-info { margin-top: 4px; color: #333; word-wrap: break-word; }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<h1>App List</h1>\n" +
                "<div class=\"app-grid\">\n" +
                "<div class=\"app-item\">\n" +
                "<div class=\"app-name\">App 1</div>\n" +
                "<div class=\"package-name\">com.example.app1</div>\n" +
                "<div class=\"app-info\">1.0</div>\n" +
                "</div>\n" +
                "<div class=\"app-item\">\n" +
                "<div class=\"app-name\">App 2</div>\n" +
                "<div class=\"package-name\">com.example.app2</div>\n" +
                "<div class=\"app-info\">2.0</div>\n" +
                "</div>\n" +
                "</div>\n" +
                "</body>\n" +
                "</html>\n"
        assertEquals(expected, result)
    }

    @Test
    fun `given list of apps with special characters, when toHtml called, then return escaped html`() {
        // Given
        val items =
            listOf(
                AppItemUiModel(
                    packageName = "com.example.app<1>",
                    appName = "App & 1",
                    infoText = "\"1.0\"",
                ),
            )

        // When
        val result = formatter.toHtml(items)

        // Then
        val expected =
            "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>App List</title>\n" +
                "<style>\n" +
                "body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n" +
                "h1 { text-align: center; color: #333; }\n" +
                ".app-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; padding: 10px; }\n" +
                ".app-item { background: white; border-radius: 8px; padding: 10px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n" +
                ".app-name { font-weight: bold; margin-top: 8px; word-wrap: break-word; }\n" +
                ".package-name { font-style: italic; font-size: 0.9em; color: #666; margin-top: 4px; word-wrap: break-word; }\n" +
                ".app-info { margin-top: 4px; color: #333; word-wrap: break-word; }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<h1>App List</h1>\n" +
                "<div class=\"app-grid\">\n" +
                "<div class=\"app-item\">\n" +
                "<div class=\"app-name\">App &amp; 1</div>\n" +
                "<div class=\"package-name\">com.example.app&lt;1&gt;</div>\n" +
                "<div class=\"app-info\">&quot;1.0&quot;</div>\n" +
                "</div>\n" +
                "</div>\n" +
                "</body>\n" +
                "</html>\n"
        assertEquals(expected, result)
    }

    @Test
    fun `given list of apps, when toCsv called, then return correct csv`() {
        // Given
        val items =
            listOf(
                AppItemUiModel(
                    packageName = "com.example.app1",
                    appName = "App 1",
                    infoText = "1.0",
                ),
                AppItemUiModel(
                    packageName = "com.example.app2",
                    appName = "App 2",
                    infoText = "2.0",
                ),
            )

        // When
        val result = formatter.toCsv(items, AppInfoField.VERSION)

        // Then
        val expected =
            "App Name,Package Name,Info Type,Info Value\n" +
                "\"App 1\",\"com.example.app1\",\"VERSION\",\"1.0\"\n" +
                "\"App 2\",\"com.example.app2\",\"VERSION\",\"2.0\"\n"
        assertEquals(expected, result)
    }

    @Test
    fun `given apps with quotes and commas, when toCsv called, then return escaped csv`() {
        // Given
        val items =
            listOf(
                AppItemUiModel(
                    packageName = "com.example.app,1",
                    appName = "App \"1\"",
                    infoText = "1.0",
                ),
            )

        // When
        val result = formatter.toCsv(items, AppInfoField.VERSION)

        // Then
        val expected =
            "App Name,Package Name,Info Type,Info Value\n" +
                "\"App \"\"1\"\"\",\"com.example.app,1\",\"VERSION\",\"1.0\"\n"
        assertEquals(expected, result)
    }

    @Test
    fun `given list of apps, when toTsv called, then return correct tsv`() {
        // Given
        val items =
            listOf(
                AppItemUiModel(
                    packageName = "com.example.app1",
                    appName = "App 1",
                    infoText = "1.0",
                ),
                AppItemUiModel(
                    packageName = "com.example.app2",
                    appName = "App 2",
                    infoText = "2.0",
                ),
            )

        // When
        val result = formatter.toTsv(items, AppInfoField.VERSION)

        // Then
        val expected =
            "App Name\tPackage Name\tInfo Type\tInfo Value\n" +
                "App 1\tcom.example.app1\tVERSION\t1.0\n" +
                "App 2\tcom.example.app2\tVERSION\t2.0\n"
        assertEquals(expected, result)
    }
}
