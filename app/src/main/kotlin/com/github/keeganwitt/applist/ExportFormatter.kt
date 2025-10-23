package com.github.keeganwitt.applist

import android.text.Html
import java.lang.StringBuilder

class ExportFormatter {
    fun toXml(items: List<AppItemUiModel>, selectedField: AppInfoField): String {
        val sb = StringBuilder()
        sb.append("""<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n""")
        sb.append("<apps>\n")
        items.forEach { item ->
            sb.append("  <app>\n")
            sb.append("    <appName>").append(escapeXml(item.appName)).append("</appName>\n")
            sb.append("    <appPackage>").append(escapeXml(item.packageName)).append("</appPackage>\n")
            sb.append("    <appInfoType>").append(selectedField.name).append("</appInfoType>\n")
            sb.append("    <appInfoValue>").append(escapeXml(item.infoText)).append("</appInfoValue>\n")
            sb.append("  </app>\n")
        }
        sb.append("</apps>\n")
        return sb.toString()
    }

    fun toHtml(items: List<AppItemUiModel>): String {
        val sb = StringBuilder()
        sb.append("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>App List</title>
              <style>
                body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }
                h1 { text-align: center; color: #333; }
                .app-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; padding: 10px; }
                .app-item { background: white; border-radius: 8px; padding: 10px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .app-name { font-weight: bold; margin-top: 8px; word-wrap: break-word; }
                .package-name { font-style: italic; font-size: 0.9em; color: #666; margin-top: 4px; word-wrap: break-word; }
                .app-info { margin-top: 4px; color: #333; word-wrap: break-word; }
              </style>
            </head>
            <body>
              <h1>App List</h1>
              <div class="app-grid">
        """.trimIndent())
        items.forEach { item ->
            sb.append("<div class=\"app-item\">\n")
            sb.append("<div class=\"app-name\">").append(escapeHtml(item.appName)).append("</div>\n")
            sb.append("<div class=\"package-name\">").append(escapeHtml(item.packageName)).append("</div>\n")
            sb.append("<div class=\"app-info\">").append(escapeHtml(item.infoText)).append("</div>\n")
            sb.append("</div>\n")
        }
        sb.append("""
              </div>
            </body>
            </html>
        """.trimIndent())
        return sb.toString()
    }

    private fun escapeXml(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

    private fun escapeHtml(s: String): String = Html.escapeHtml(s).toString()
}
