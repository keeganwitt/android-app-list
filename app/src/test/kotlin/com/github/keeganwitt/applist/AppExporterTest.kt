package com.github.keeganwitt.applist

import android.content.ContentResolver
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowToast
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(application = TestAppListApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class AppExporterTest {
    private lateinit var activity: AppCompatActivity
    private lateinit var items: List<AppItemUiModel>
    private lateinit var crashReporter: CrashReporter
    private val testDispatchers =
        object : DispatcherProvider {
            override val io = Dispatchers.Unconfined
            override val main = Dispatchers.Unconfined
            override val default = Dispatchers.Unconfined
        }

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
        val exporter =
            AppExporter(activity, { emptyList() }, ExportFormatter(), crashReporter, testDispatchers)
        exporter.selectedAppInfoField = AppInfoField.VERSION

        Shadows.shadowOf(activity.mainLooper).runPaused {
            exporter.initiateExport(AppExporter.ExportFormat.XML)
        }

        val text = ShadowToast.getTextOfLatestToast()
        assertTrue(text.toString() == activity.getString(R.string.export_no_apps))
    }

    @Test
    fun writeXmlToFile_writesContent_andShowsSuccessToast() {
        val formatter = mockk<ExportFormatter>()
        val xmlOutput = "<?xml version=\"1.0\"?><apps></apps>"
        every { formatter.toXml(any(), any()) } returns xmlOutput

        val exporter = AppExporter(activity, { items }, formatter, crashReporter, testDispatchers)
        exporter.selectedAppInfoField = AppInfoField.VERSION
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeXmlToFile(uri)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.toXml(items, AppInfoField.VERSION) }
        val toast = ShadowToast.getTextOfLatestToast()
        assertTrue(toast.toString() == activity.getString(R.string.export_successful))
    }

    @Test
    fun writeXmlToFile_onIOException_reportsCrash_andShowsFailToast() {
        val exceptionMessage = "boom"
        val failingFormatter = mockk<ExportFormatter>()
        every { failingFormatter.toXml(any(), any()) } throws IOException(exceptionMessage)
        val exporter = AppExporter(activity, { items }, failingFormatter, crashReporter, testDispatchers)
        exporter.selectedAppInfoField = AppInfoField.VERSION
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeXmlToFile(uri)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { crashReporter.recordException(any(), "Error exporting XML") }
        val toast = ShadowToast.getTextOfLatestToast()
        val expected = activity.getString(R.string.export_failed, exceptionMessage)
        assertTrue(toast.toString() == expected)
    }

    @Test
    fun writeXmlToFile_onSecurityException_reportsCrash_andShowsFailToast() {
        val exceptionMessage = "boom"
        val failingFormatter = mockk<ExportFormatter>()
        every { failingFormatter.toXml(any(), any()) } throws SecurityException(exceptionMessage)
        val exporter = AppExporter(activity, { items }, failingFormatter, crashReporter, testDispatchers)
        exporter.selectedAppInfoField = AppInfoField.VERSION
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeXmlToFile(uri)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { crashReporter.recordException(any(), "Error exporting XML") }
        val toast = ShadowToast.getTextOfLatestToast()
        val expected = activity.getString(R.string.export_failed, exceptionMessage)
        assertTrue(toast.toString() == expected)
    }

    @Test
    fun writeXmlToFile_withNullOutputStream_reportsCrash_andShowsFailToast() {
        val formatter = mockk<ExportFormatter>()
        val itemsProvider = mockk<() -> List<AppItemUiModel>>()
        every { itemsProvider() } returns items
        val spyActivity = spyk(activity)
        val mockContentResolver = mockk<ContentResolver>()
        every { spyActivity.contentResolver } returns mockContentResolver
        val exporter = AppExporter(spyActivity, itemsProvider, formatter, crashReporter, testDispatchers)
        exporter.selectedAppInfoField = AppInfoField.VERSION
        val uri = Uri.parse("content://test/uri")
        every { mockContentResolver.openOutputStream(uri) } returns null

        exporter.writeXmlToFile(uri)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify(exactly = 0) { formatter.toXml(any(), any()) }
        // itemsProvider is called to capture items before launching coroutine
        verify(exactly = 1) { itemsProvider() }
        verify { crashReporter.recordException(any(), "Error exporting XML") }
        val toast = ShadowToast.getTextOfLatestToast()
        val expected = activity.getString(R.string.export_failed, "Failed to open output stream")
        assertTrue(toast.toString() == expected)
    }

    @Test
    fun writeHtmlToFile_writesContent_andShowsSuccessToast() {
        val formatter = mockk<ExportFormatter>()
        val htmlOutput = "<!DOCTYPE html><html></html>"
        every { formatter.toHtml(any()) } returns htmlOutput

        val exporter = AppExporter(activity, { items }, formatter, crashReporter, testDispatchers)
        val file = File.createTempFile("apps", ".html").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeHtmlToFile(uri)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.toHtml(items) }
        val toast = ShadowToast.getTextOfLatestToast()
        assertTrue(toast.toString() == activity.getString(R.string.export_successful))
    }

    @Test
    fun writeHtmlToFile_onError_reportsCrash_andShowsFailToast() {
        val exceptionMessage = "boom"
        val failingFormatter = mockk<ExportFormatter>()
        every { failingFormatter.toHtml(any()) } throws IOException(exceptionMessage)
        val exporter = AppExporter(activity, { items }, failingFormatter, crashReporter, testDispatchers)
        val file = File.createTempFile("apps", ".html").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeHtmlToFile(uri)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { crashReporter.recordException(any(), "Error exporting HTML") }
        val toast = ShadowToast.getTextOfLatestToast()
        val expected = activity.getString(R.string.export_failed, exceptionMessage)
        assertTrue(toast.toString() == expected)
    }

    @Test
    fun writeHtmlToFile_withNullOutputStream_reportsCrash_andShowsFailToast() {
        val formatter = mockk<ExportFormatter>()
        val itemsProvider = mockk<() -> List<AppItemUiModel>>()
        every { itemsProvider() } returns items
        val spyActivity = spyk(activity)
        val mockContentResolver = mockk<ContentResolver>()
        every { spyActivity.contentResolver } returns mockContentResolver
        val exporter = AppExporter(spyActivity, itemsProvider, formatter, crashReporter, testDispatchers)
        val uri = Uri.parse("content://test/uri")
        every { mockContentResolver.openOutputStream(uri) } returns null

        exporter.writeHtmlToFile(uri)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify(exactly = 0) { formatter.toHtml(any()) }
        // itemsProvider is called to capture items before launching coroutine
        verify(exactly = 1) { itemsProvider() }
        verify { crashReporter.recordException(any(), "Error exporting HTML") }
        val toast = ShadowToast.getTextOfLatestToast()
        val expected = activity.getString(R.string.export_failed, "Failed to open output stream")
        assertTrue(toast.toString() == expected)
    }

    @Test
    fun writeCsvToFile_writesContent_andShowsSuccessToast() {
        val formatter = mockk<ExportFormatter>()
        val csvOutput = "App Name,Package Name,Info Type,Info Value\n\"App 1\",\"com.example.app1\",\"VERSION\",\"1.0\""
        every { formatter.toCsv(any(), any()) } returns csvOutput

        val exporter = AppExporter(activity, { items }, formatter, crashReporter, testDispatchers)
        exporter.selectedAppInfoField = AppInfoField.VERSION
        val file = File.createTempFile("apps", ".csv").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeCsvToFile(uri)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.toCsv(items, AppInfoField.VERSION) }
        val toast = ShadowToast.getTextOfLatestToast()
        assertTrue(toast.toString() == activity.getString(R.string.export_successful))
    }

    @Test
    fun writeTsvToFile_writesContent_andShowsSuccessToast() {
        val formatter = mockk<ExportFormatter>()
        val tsvOutput = "App Name\tPackage Name\tInfo Type\tInfo Value\nApp 1\tcom.example.app1\tVERSION\t1.0"
        every { formatter.toTsv(any(), any()) } returns tsvOutput

        val exporter = AppExporter(activity, { items }, formatter, crashReporter, testDispatchers)
        exporter.selectedAppInfoField = AppInfoField.VERSION
        val file = File.createTempFile("apps", ".tsv").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeTsvToFile(uri)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.toTsv(items, AppInfoField.VERSION) }
        val toast = ShadowToast.getTextOfLatestToast()
        assertTrue(toast.toString() == activity.getString(R.string.export_successful))
    }
}
