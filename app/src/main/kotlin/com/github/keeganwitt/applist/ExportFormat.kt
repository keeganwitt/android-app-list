package com.github.keeganwitt.applist

enum class ExportFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String,
) {
    XML("xml", "text/xml", "XML"),
    HTML("html", "text/html", "HTML"),
    CSV("csv", "text/csv", "CSV"),
    TSV("tsv", "text/tab-separated-values", "TSV"),
}
