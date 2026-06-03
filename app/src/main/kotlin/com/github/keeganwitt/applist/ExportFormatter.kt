package com.github.keeganwitt.applist

import android.util.Xml
import java.io.Writer

class ExportFormatter {
    fun write(
        format: ExportFormat,
        writer: Writer,
        apps: List<App>,
        includeUsageStats: Boolean,
    ) {
        when (format) {
            ExportFormat.XML -> writeXml(writer, apps, includeUsageStats)
            ExportFormat.HTML -> writeHtml(writer, apps, includeUsageStats)
            ExportFormat.CSV -> writeCsv(writer, apps, includeUsageStats)
            ExportFormat.TSV -> writeTsv(writer, apps, includeUsageStats)
        }
    }

    fun writeXml(
        writer: Writer,
        apps: List<App>,
        includeUsageStats: Boolean,
    ) {
        val fields = if (includeUsageStats) fieldsWithUsageStats else fieldsWithoutUsageStats
        val serializer = Xml.newSerializer()
        serializer.setOutput(writer)
        serializer.startDocument("UTF-8", null)
        serializer.text("\n")
        serializer.startTag(null, "apps")
        serializer.text("\n")
        apps.forEach { app ->
            serializer.startTag(null, "app")
            serializer.text("\n")
            serializer.startTag(null, "appName")
            serializer.text(app.name)
            serializer.endTag(null, "appName")
            serializer.text("\n")
            serializer.startTag(null, "packageName")
            serializer.text(app.packageName)
            serializer.endTag(null, "packageName")
            serializer.text("\n")
            fields.forEach { field ->
                serializer.startTag(null, field.name)
                serializer.text(field.getFormattedValue(app))
                serializer.endTag(null, field.name)
                serializer.text("\n")
            }
            serializer.endTag(null, "app")
            serializer.text("\n")
        }
        serializer.endTag(null, "apps")
        serializer.text("\n")
        serializer.endDocument()
    }

    fun writeHtml(
        writer: Writer,
        apps: List<App>,
        includeUsageStats: Boolean,
    ) {
        val fields = if (includeUsageStats) fieldsWithUsageStats else fieldsWithoutUsageStats
        writer.write(HTML_HEADER)

        val sb = StringBuilder()
        apps.forEach { app ->
            sb.setLength(0)
            sb.append("<div class=\"app-item\">\n")
            sb.append("<div class=\"app-name\">")
            sb.appendHtmlEncoded(app.name)
            sb.append("</div>\n")
            sb.append("<div class=\"package-name\">")
            sb.appendHtmlEncoded(app.packageName)
            sb.append("</div>\n")
            fields.forEach { field ->
                sb.append("<div class=\"app-info\"><b>")
                sb.append(field.name)
                sb.append(":</b> ")
                sb.appendHtmlEncoded(field.getFormattedValue(app))
                sb.append("</div>\n")
            }
            sb.append("</div>\n")
            writer.write(sb.toString())
        }

        writer.write(HTML_FOOTER)
    }

    fun writeCsv(
        writer: Writer,
        apps: List<App>,
        includeUsageStats: Boolean,
    ) {
        val fields = if (includeUsageStats) fieldsWithUsageStats else fieldsWithoutUsageStats
        writer.append("App Name,Package Name")
        fields.forEach { field ->
            writer.append(",").append(field.name)
        }
        writer.append("\n")

        val sb = StringBuilder()
        apps.forEach { app ->
            sb.setLength(0)
            sb.append("\"")
            sb.appendCsvEscaped(app.name)
            sb.append("\",\"")
            sb.appendCsvEscaped(app.packageName)
            sb.append("\"")
            fields.forEach { field ->
                sb.append(",\"")
                sb.appendCsvEscaped(field.getFormattedValue(app))
                sb.append("\"")
            }
            sb.append("\n")
            writer.write(sb.toString())
        }
    }

    fun writeTsv(
        writer: Writer,
        apps: List<App>,
        includeUsageStats: Boolean,
    ) {
        val fields = if (includeUsageStats) fieldsWithUsageStats else fieldsWithoutUsageStats
        writer.append("App Name\tPackage Name")
        fields.forEach { field ->
            writer.append("\t").append(field.name)
        }
        writer.append("\n")

        val sb = StringBuilder()
        apps.forEach { app ->
            sb.setLength(0)
            sb.appendTsvEscaped(app.name)
            sb.append("\t")
            sb.appendTsvEscaped(app.packageName)
            fields.forEach { field ->
                sb.append("\t")
                sb.appendTsvEscaped(field.getFormattedValue(app))
            }
            sb.append("\n")
            writer.write(sb.toString())
        }
    }

    private fun StringBuilder.appendHtmlEncoded(s: String) {
        for (element in s) {
            when (element) {
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '&' -> append("&amp;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(element)
            }
        }
    }

    private fun StringBuilder.appendCsvEscaped(s: String) {
        for (element in s) {
            if (element == '"') {
                append("\"\"")
            } else {
                append(element)
            }
        }
    }

    private fun StringBuilder.appendTsvEscaped(s: String) {
        for (element in s) {
            when (element) {
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(element)
            }
        }
    }

    companion object {
        private val fieldsWithUsageStats by lazy { AppInfoField.entries }
        private val fieldsWithoutUsageStats by lazy { AppInfoField.entries.filter { !it.requiresUsageStats } }

        private const val HTML_HEADER = """<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>App List</title>
<style>
body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }
h1 { text-align: center; color: #333; }
.app-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 10px; padding: 10px; }
.app-item { background: white; border-radius: 8px; padding: 10px; text-align: left; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.app-name { font-weight: bold; font-size: 1.2em; margin-bottom: 4px; word-wrap: break-word; text-align: center; }
.package-name { font-style: italic; font-size: 0.9em; color: #666; margin-bottom: 8px; word-wrap: break-word; text-align: center; }
.app-info { margin-top: 2px; font-size: 0.85em; color: #333; word-wrap: break-word; }
</style>
</head>
<body>
<h1>App List</h1>
<div class="app-grid">
"""

        private const val HTML_FOOTER = """</div>
</body>
</html>
"""
    }
}
