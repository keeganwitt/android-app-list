package com.github.keeganwitt.applist

import android.content.ContentResolver
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowToast
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class AppExporterTest {
    private lateinit var activity: AppCompatActivity
    private lateinit var items: List<AppItemUiModel>
    private lateinit var crashReporter: CrashReporter

    @Before
    fun setUp() {
        activity =
            Robolectric
                .buildActivity(AppCompatActivity::class.java)
                .setup()
                .get()
        items =
            listOf(
                AppItemUiModel("com.example.app1", "App 1", "1.0"),
                AppItemUiModel("com.example.app2", "App 2", "2.0"),
            )
        crashReporter = mockk(relaxed = true)
    }

    @Test
    fun initiateExport_withEmptyItems_showsNoAppsToast() {
        val exporter = AppExporter(activity, { emptyList() }, ExportFormatter(), crashReporter)
        exporter.selectedAppInfoField = AppInfoField.VERSION

        Shadows.shadowOf(activity.mainLooper).runPaused {
            exporter.initiateExport("xml")
        }

        val text = ShadowToast.getTextOfLatestToast()
        assert(text.toString() == activity.getString(R.string.export_no_apps).toString())
    }

    @Test
    fun writeXmlToFile_writesContent_andShowsSuccessToast() {
        val formatter = mockk<ExportFormatter>()
        val xmlOutput = "<?xml version=\"1.0\"?><apps></apps>"
        every { formatter.toXml(any(), any()) } returns xmlOutput

        val exporter = AppExporter(activity, { items }, formatter, crashReporter)
        exporter.selectedAppInfoField = AppInfoField.VERSION
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeXmlToFile(uri)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.toXml(items, AppInfoField.VERSION) }
        val toast = ShadowToast.getTextOfLatestToast()
        assert(toast.toString() == activity.getString(R.string.export_successful))
    }

    @Test
    fun writeXmlToFile_onError_reportsCrash_andShowsFailToast() {
        val failingFormatter = mockk<ExportFormatter>()
        every { failingFormatter.toXml(any(), any()) } throws RuntimeException("boom")
        val exporter = AppExporter(activity, { items }, failingFormatter, crashReporter)
        exporter.selectedAppInfoField = AppInfoField.VERSION
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeXmlToFile(uri)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { crashReporter.record(any(), "Error exporting XML") }
        val toast = ShadowToast.getTextOfLatestToast()
        assert(toast.toString().startsWith(activity.getString(R.string.export_failed)))
    }

    @Test
    fun writeHtmlToFile_writesContent_andShowsSuccessToast() {
        val formatter = mockk<ExportFormatter>()
        val htmlOutput = "<!DOCTYPE html><html></html>"
        every { formatter.toHtml(any()) } returns htmlOutput

        val exporter = AppExporter(activity, { items }, formatter, crashReporter)
        val file = File.createTempFile("apps", ".html").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeHtmlToFile(uri)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.toHtml(items) }
        val toast = ShadowToast.getTextOfLatestToast()
        assert(toast.toString() == activity.getString(R.string.export_successful))
    }

    @Test
    fun writeHtmlToFile_onError_reportsCrash_andShowsFailToast() {
        val failingFormatter = mockk<ExportFormatter>()
        every { failingFormatter.toHtml(any()) } throws RuntimeException("boom")
        val exporter = AppExporter(activity, { items }, failingFormatter, crashReporter)
        val file = File.createTempFile("apps", ".html").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeHtmlToFile(uri)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { crashReporter.record(any(), "Error exporting HTML") }
        val toast = ShadowToast.getTextOfLatestToast()
        assert(toast.toString().startsWith(activity.getString(R.string.export_failed)))
    }
}
