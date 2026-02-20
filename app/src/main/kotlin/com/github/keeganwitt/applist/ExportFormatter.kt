package com.github.keeganwitt.applist

import java.io.StringWriter
import java.io.Writer

class ExportFormatter {
    fun toXml(
        apps: List<App>,
        includeUsageStats: Boolean,
    ): String {
        val sw = StringWriter()
        writeXml(sw, apps, includeUsageStats)
        return sw.toString()
    }

    fun writeXml(
        writer: Writer,
        apps: List<App>,
        includeUsageStats: Boolean,
    ) {
        val fields = AppInfoField.entries.filter { includeUsageStats || !it.requiresUsageStats }
        writer
            .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            .append("<apps>\n")
        apps.forEach { app ->
            writer.append("<app>\n")
            writer.append("<appName>").append(escapeMarkup(app.name)).append("</appName>\n")
            writer.append("<packageName>").append(escapeMarkup(app.packageName)).append("</packageName>\n")
            fields.forEach { field ->
                writer.append("<").append(field.name).append(">")
                    .append(escapeMarkup(field.getFormattedValue(app)))
                    .append("</").append(field.name).append(">\n")
            }
            writer.append("</app>\n")
        }
        writer.append("</apps>\n")
    }

    fun toHtml(
        apps: List<App>,
        includeUsageStats: Boolean,
    ): String {
        val sw = StringWriter()
        writeHtml(sw, apps, includeUsageStats)
        return sw.toString()
    }

    fun writeHtml(
        writer: Writer,
        apps: List<App>,
        includeUsageStats: Boolean,
    ) {
        val fields = AppInfoField.entries.filter { includeUsageStats || !it.requiresUsageStats }
        writer
            .append("<!DOCTYPE html>\n")
            .append("<html>\n")
            .append("<head>\n")
            .append("<meta charset=\"UTF-8\">\n")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
            .append("<title>App List</title>\n")
            .append("<style>\n")
            .append("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n")
            .append("h1 { text-align: center; color: #333; }\n")
            .append(".app-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 10px; padding: 10px; }\n")
            .append(
                ".app-item { background: white; border-radius: 8px; padding: 10px; text-align: left; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n",
            ).append(".app-name { font-weight: bold; font-size: 1.2em; margin-bottom: 4px; word-wrap: break-word; text-align: center; }\n")
            .append(".package-name { font-style: italic; font-size: 0.9em; color: #666; margin-bottom: 8px; word-wrap: break-word; text-align: center; }\n")
            .append(".app-info { margin-top: 2px; font-size: 0.85em; color: #333; word-wrap: break-word; }\n")
            .append("</style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("<h1>App List</h1>\n")
            .append("<div class=\"app-grid\">\n")
        apps.forEach { app ->
            writer.append("<div class=\"app-item\">\n")
            writer.append("<div class=\"app-name\">").append(escapeMarkup(app.name)).append("</div>\n")
            writer.append("<div class=\"package-name\">").append(escapeMarkup(app.packageName)).append("</div>\n")
            fields.forEach { field ->
                writer.append("<div class=\"app-info\"><b>").append(field.name).append(":</b> ")
                    .append(escapeMarkup(field.getFormattedValue(app))).append("</div>\n")
            }
            writer.append("</div>\n")
        }
        writer
            .append("</div>\n")
            .append("</body>\n")
            .append("</html>\n")
    }

    fun toCsv(
        apps: List<App>,
        includeUsageStats: Boolean,
    ): String {
        val sw = StringWriter()
        writeCsv(sw, apps, includeUsageStats)
        return sw.toString()
    }

    fun writeCsv(
        writer: Writer,
        apps: List<App>,
        includeUsageStats: Boolean,
    ) {
        val fields = AppInfoField.entries.filter { includeUsageStats || !it.requiresUsageStats }
        writer.append("App Name,Package Name")
        fields.forEach { field ->
            writer.append(",").append(field.name)
        }
        writer.append("\n")

        apps.forEach { app ->
            writer.append("\"").append(app.name.replace("\"", "\"\"")).append("\",")
            writer.append("\"").append(app.packageName.replace("\"", "\"\"")).append("\"")
            fields.forEach { field ->
                writer.append(",\"").append(field.getFormattedValue(app).replace("\"", "\"\"")).append("\"")
            }
            writer.append("\n")
        }
    }

    fun toTsv(
        apps: List<App>,
        includeUsageStats: Boolean,
    ): String {
        val sw = StringWriter()
        writeTsv(sw, apps, includeUsageStats)
        return sw.toString()
    }

    fun writeTsv(
        writer: Writer,
        apps: List<App>,
        includeUsageStats: Boolean,
    ) {
        val fields = AppInfoField.entries.filter { includeUsageStats || !it.requiresUsageStats }
        writer.append("App Name\tPackage Name")
        fields.forEach { field ->
            writer.append("\t").append(field.name)
        }
        writer.append("\n")

        apps.forEach { app ->
            writer.append(app.name).append("\t")
            writer.append(app.packageName)
            fields.forEach { field ->
                writer.append("\t").append(field.getFormattedValue(app))
            }
            writer.append("\n")
        }
    }

    // This is not using Html.escapeHtml for unit tests (since it's not implemented by Robolectric)
    private fun escapeMarkup(s: String): String =
        s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
