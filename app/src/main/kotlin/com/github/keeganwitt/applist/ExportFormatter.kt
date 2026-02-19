package com.github.keeganwitt.applist

import java.io.StringWriter
import java.io.Writer

class ExportFormatter {
    fun toXml(
        items: List<AppItemUiModel>,
        selectedField: AppInfoField,
    ): String {
        val sw = StringWriter()
        writeXml(sw, items, selectedField)
        return sw.toString()
    }

    fun writeXml(
        writer: Writer,
        items: List<AppItemUiModel>,
        selectedField: AppInfoField,
    ) {
        writer
            .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            .append("<apps>\n")
        items.forEach { item ->
            writer.append("<app>\n")
            writer.append("<appName>").append(escapeMarkup(item.appName)).append("</appName>\n")
            writer.append("<appPackage>").append(escapeMarkup(item.packageName)).append("</appPackage>\n")
            writer.append("<appInfoType>").append(selectedField.name).append("</appInfoType>\n")
            writer.append("<appInfoValue>").append(escapeMarkup(item.infoText)).append("</appInfoValue>\n")
            writer.append("</app>\n")
        }
        writer.append("</apps>\n")
    }

    fun toHtml(items: List<AppItemUiModel>): String {
        val sw = StringWriter()
        writeHtml(sw, items)
        return sw.toString()
    }

    fun writeHtml(
        writer: Writer,
        items: List<AppItemUiModel>,
    ) {
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
            .append(".app-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; padding: 10px; }\n")
            .append(
                ".app-item { background: white; border-radius: 8px; padding: 10px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n",
            ).append(".app-name { font-weight: bold; margin-top: 8px; word-wrap: break-word; }\n")
            .append(".package-name { font-style: italic; font-size: 0.9em; color: #666; margin-top: 4px; word-wrap: break-word; }\n")
            .append(".app-info { margin-top: 4px; color: #333; word-wrap: break-word; }\n")
            .append("</style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("<h1>App List</h1>\n")
            .append("<div class=\"app-grid\">\n")
        items.forEach { item ->
            writer.append("<div class=\"app-item\">\n")
            writer.append("<div class=\"app-name\">").append(escapeMarkup(item.appName)).append("</div>\n")
            writer.append("<div class=\"package-name\">").append(escapeMarkup(item.packageName)).append("</div>\n")
            writer.append("<div class=\"app-info\">").append(escapeMarkup(item.infoText)).append("</div>\n")
            writer.append("</div>\n")
        }
        writer
            .append("</div>\n")
            .append("</body>\n")
            .append("</html>\n")
    }

    fun toCsv(
        items: List<AppItemUiModel>,
        selectedField: AppInfoField,
    ): String {
        val sw = StringWriter()
        writeCsv(sw, items, selectedField)
        return sw.toString()
    }

    fun writeCsv(
        writer: Writer,
        items: List<AppItemUiModel>,
        selectedField: AppInfoField,
    ) {
        writer.append("App Name,Package Name,Info Type,Info Value\n")
        items.forEach { item ->
            writer.append("\"").append(item.appName.replace("\"", "\"\"")).append("\",")
            writer.append("\"").append(item.packageName.replace("\"", "\"\"")).append("\",")
            writer.append("\"").append(selectedField.name.replace("\"", "\"\"")).append("\",")
            writer.append("\"").append(item.infoText.replace("\"", "\"\"")).append("\"\n")
        }
    }

    fun toTsv(
        items: List<AppItemUiModel>,
        selectedField: AppInfoField,
    ): String {
        val sw = StringWriter()
        writeTsv(sw, items, selectedField)
        return sw.toString()
    }

    fun writeTsv(
        writer: Writer,
        items: List<AppItemUiModel>,
        selectedField: AppInfoField,
    ) {
        writer.append("App Name\tPackage Name\tInfo Type\tInfo Value\n")
        items.forEach { item ->
            writer.append(item.appName).append("\t")
            writer.append(item.packageName).append("\t")
            writer.append(selectedField.name).append("\t")
            writer.append(item.infoText).append("\n")
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
