package com.github.keeganwitt.applist

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportFormatTest {
    @Test
    fun `given ExportFormat enum, when entries accessed, then all formats are present`() {
        val entries = ExportFormat.entries

        assertEquals(4, entries.size)
    }

    @Test
    fun `given XML format, when properties accessed, then they are correct`() {
        val format = ExportFormat.XML

        assertEquals("xml", format.extension)
        assertEquals("text/xml", format.mimeType)
        assertEquals("XML", format.displayName)
    }

    @Test
    fun `given HTML format, when properties accessed, then they are correct`() {
        val format = ExportFormat.HTML

        assertEquals("html", format.extension)
        assertEquals("text/html", format.mimeType)
        assertEquals("HTML", format.displayName)
    }

    @Test
    fun `given CSV format, when properties accessed, then they are correct`() {
        val format = ExportFormat.CSV

        assertEquals("csv", format.extension)
        assertEquals("text/csv", format.mimeType)
        assertEquals("CSV", format.displayName)
    }

    @Test
    fun `given TSV format, when properties accessed, then they are correct`() {
        val format = ExportFormat.TSV

        assertEquals("tsv", format.extension)
        assertEquals("text/tab-separated-values", format.mimeType)
        assertEquals("TSV", format.displayName)
    }
}
