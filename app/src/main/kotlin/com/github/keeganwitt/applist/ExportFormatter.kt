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
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        writer.write("<apps>\n")
        items.forEach { item ->
            writer.write("<app>\n")
            writer.write("<appName>")
            writer.write(escapeMarkup(item.appName))
            writer.write("</appName>\n")
            writer.write("<appPackage>")
            writer.write(escapeMarkup(item.packageName))
            writer.write("</appPackage>\n")
            writer.write("<appInfoType>")
            writer.write(selectedField.name)
            writer.write("</appInfoType>\n")
            writer.write("<appInfoValue>")
            writer.write(escapeMarkup(item.infoText))
            writer.write("</appInfoValue>\n")
            writer.write("</app>\n")
        }
        writer.write("</apps>\n")
        writer.flush()
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
        writer.write("<!DOCTYPE html>\n")
        writer.write("<html>\n")
        writer.write("<head>\n")
        writer.write("<meta charset=\"UTF-8\">\n")
        writer.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
        writer.write("<title>App List</title>\n")
        writer.write("<style>\n")
        writer.write("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n")
        writer.write("h1 { text-align: center; color: #333; }\n")
        writer.write(".app-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; padding: 10px; }\n")
        writer.write(".app-item { background: white; border-radius: 8px; padding: 10px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n")
        writer.write(".app-name { font-weight: bold; margin-top: 8px; word-wrap: break-word; }\n")
        writer.write(".package-name { font-style: italic; font-size: 0.9em; color: #666; margin-top: 4px; word-wrap: break-word; }\n")
        writer.write(".app-info { margin-top: 4px; color: #333; word-wrap: break-word; }\n")
        writer.write("</style>\n")
        writer.write("</head>\n")
        writer.write("<body>\n")
        writer.write("<h1>App List</h1>\n")
        writer.write("<div class=\"app-grid\">\n")
        items.forEach { item ->
            writer.write("<div class=\"app-item\">\n")
            writer.write("<div class=\"app-name\">")
            writer.write(escapeMarkup(item.appName))
            writer.write("</div>\n")
            writer.write("<div class=\"package-name\">")
            writer.write(escapeMarkup(item.packageName))
            writer.write("</div>\n")
            writer.write("<div class=\"app-info\">")
            writer.write(escapeMarkup(item.infoText))
            writer.write("</div>\n")
            writer.write("</div>\n")
        }
        writer.write("</div>\n")
        writer.write("</body>\n")
        writer.write("</html>\n")
        writer.flush()
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
        writer.write("App Name,Package Name,Info Type,Info Value\n")
        items.forEach { item ->
            writer.write("\"")
            writer.write(item.appName.replace("\"", "\"\""))
            writer.write("\",")
            writer.write("\"")
            writer.write(item.packageName.replace("\"", "\"\""))
            writer.write("\",")
            writer.write("\"")
            writer.write(selectedField.name.replace("\"", "\"\""))
            writer.write("\",")
            writer.write("\"")
            writer.write(item.infoText.replace("\"", "\"\""))
            writer.write("\"\n")
        }
        writer.flush()
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
        writer.write("App Name\tPackage Name\tInfo Type\tInfo Value\n")
        items.forEach { item ->
            writer.write(item.appName)
            writer.write("\t")
            writer.write(item.packageName)
            writer.write("\t")
            writer.write(selectedField.name)
            writer.write("\t")
            writer.write(item.infoText)
            writer.write("\n")
        }
        writer.flush()
    }

    private fun escapeMarkup(s: String): String =
        s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
