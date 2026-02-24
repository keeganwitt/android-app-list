package com.github.keeganwitt.applist

import android.app.Activity
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
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
    private lateinit var apps: List<App>
    private lateinit var crashReporter: CrashReporter
    private lateinit var appSettings: AppSettings
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
        apps =
            listOf(
                createTestApp("com.example.app1", "App 1"),
                createTestApp("com.example.app2", "App 2"),
            )
        crashReporter = mockk(relaxed = true)
        appSettings = mockk(relaxed = true)
        every { appSettings.isIncludeUsageStatsInExportEnabled() } returns true
    }

    @Test
    fun initiateExport_withEmptyItems_showsNoAppsToast() {
        val exporter =
            AppExporter(activity, { emptyList() }, ExportFormatter(), appSettings, crashReporter, testDispatchers)

        Shadows.shadowOf(activity.mainLooper).runPaused {
            exporter.initiateExport(ExportFormat.XML)
        }

        val text = ShadowToast.getTextOfLatestToast()
        assertTrue(text.toString() == activity.getString(R.string.export_no_apps))
    }

    @Test
    fun writeToFile_withXml_writesContent_andShowsSuccessToast() {
        val formatter = mockk<ExportFormatter>()
        val xmlOutput = "<?xml version=\"1.0\"?><apps></apps>"
        every { formatter.write(ExportFormat.XML, any(), any(), any()) } answers {
            (args[1] as java.io.Writer).write(xmlOutput)
        }

        val exporter = AppExporter(activity, { apps }, formatter, appSettings, crashReporter, testDispatchers)
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeToFile(uri, ExportFormat.XML)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.write(ExportFormat.XML, any(), any(), any()) }
        val toast = ShadowToast.getTextOfLatestToast()
        assertTrue(toast.toString() == activity.getString(R.string.export_successful))
        assertEquals(xmlOutput, file.readText())
    }

    @Test
    fun `writeToFile when setting disabled then includeUsageStats is false`() {
        val formatter = mockk<ExportFormatter>(relaxed = true)
        every { appSettings.isIncludeUsageStatsInExportEnabled() } returns false
        val exporter = AppExporter(activity, { apps }, formatter, appSettings, crashReporter, testDispatchers)
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeToFile(uri, ExportFormat.XML)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.write(ExportFormat.XML, any(), apps, false) }
    }

    @Test
    fun onActivityResult_withXml_exportsXml() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val callbackSlot = slot<ActivityResultCallback<ActivityResult>>()
        every {
            registry.register(any(), any<ActivityResultContract<Intent, ActivityResult>>(), capture(callbackSlot))
        } returns mockk(relaxed = true)

        val formatter = mockk<ExportFormatter>(relaxed = true)
        val xmlOutput = "xml content"
        every { formatter.write(ExportFormat.XML, any(), any(), any()) } answers {
            (args[1] as java.io.Writer).write(xmlOutput)
        }

        val exporter = AppExporter(activity, { apps }, formatter, appSettings, crashReporter, testDispatchers, registry)
        exporter.initiateExport(ExportFormat.XML)

        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)
        val result = ActivityResult(Activity.RESULT_OK, Intent().apply { data = uri })
        callbackSlot.captured.onActivityResult(result)

        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.write(ExportFormat.XML, any(), any(), any()) }
        assertEquals(xmlOutput, file.readText())
        val toast = ShadowToast.getTextOfLatestToast()
        assertTrue("Expected successful toast but was: $toast", toast?.toString() == activity.getString(R.string.export_successful))
    }

    @Test
    fun onActivityResult_withCsv_exportsCsv() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val callbackSlot = slot<ActivityResultCallback<ActivityResult>>()
        every {
            registry.register(any(), any<ActivityResultContract<Intent, ActivityResult>>(), capture(callbackSlot))
        } returns mockk(relaxed = true)

        val formatter = mockk<ExportFormatter>(relaxed = true)
        val csvOutput = "csv content"
        every { formatter.write(ExportFormat.CSV, any(), any(), any()) } answers {
            (args[1] as java.io.Writer).write(csvOutput)
        }

        val exporter = AppExporter(activity, { apps }, formatter, appSettings, crashReporter, testDispatchers, registry)
        exporter.initiateExport(ExportFormat.CSV)

        val file = File.createTempFile("apps", ".csv").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)
        val result = ActivityResult(Activity.RESULT_OK, Intent().apply { data = uri })
        callbackSlot.captured.onActivityResult(result)

        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.write(ExportFormat.CSV, any(), any(), any()) }
        assertEquals(csvOutput, file.readText())
    }

    @Test
    fun writeToFile_whenFormatterThrowsIOException_handlesError() {
        val formatter = mockk<ExportFormatter>()
        val exception = IOException("Disk full")
        every { formatter.write(ExportFormat.XML, any(), any(), any()) } throws exception

        val exporter = AppExporter(activity, { apps }, formatter, appSettings, crashReporter, testDispatchers)
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeToFile(uri, ExportFormat.XML)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { crashReporter.recordException(exception, "Error exporting XML") }
        val toast = ShadowToast.getTextOfLatestToast()
        assertEquals(activity.getString(R.string.export_failed, exception.message), toast.toString())
    }

    @Test
    fun writeToFile_whenOpenOutputStreamReturnsNull_handlesError() {
        val authority = "com.github.keeganwitt.applist.test.null"
        Robolectric.setupContentProvider(NullContentProvider::class.java, authority)
        val exporter = AppExporter(activity, { apps }, ExportFormatter(), appSettings, crashReporter, testDispatchers)
        val uri = Uri.parse("content://$authority/file")

        exporter.writeToFile(uri, ExportFormat.XML)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { crashReporter.recordException(any<IOException>(), "Error exporting XML") }
        val toast = ShadowToast.getTextOfLatestToast()
        assertEquals(activity.getString(R.string.export_failed, "Failed to open output stream"), toast.toString())
    }

    class NullContentProvider : ContentProvider() {
        override fun onCreate(): Boolean = true
        override fun query(uri: Uri, p1: Array<out String>?, p2: String?, p3: Array<out String>?, p4: String?): Cursor? = null
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, p1: ContentValues?): Uri? = null
        override fun delete(uri: Uri, p1: String?, p2: Array<out String>?): Int = 0
        override fun update(uri: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int = 0
        override fun openFile(uri: Uri, mode: String): android.os.ParcelFileDescriptor? = null
    }

    @Test
    fun writeToFile_whenFormatterThrowsSecurityException_handlesError() {
        val formatter = mockk<ExportFormatter>()
        val exception = SecurityException("Permission denied")
        every { formatter.write(ExportFormat.XML, any(), any(), any()) } throws exception

        val exporter = AppExporter(activity, { apps }, formatter, appSettings, crashReporter, testDispatchers)
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeToFile(uri, ExportFormat.XML)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { crashReporter.recordException(exception, "Error exporting XML") }
        val toast = ShadowToast.getTextOfLatestToast()
        assertEquals(activity.getString(R.string.export_failed, exception.message), toast.toString())
    }

    @Test
    fun writeToFile_whenSecurityException_handlesError() {
        val formatter = mockk<ExportFormatter>()
        every { formatter.write(ExportFormat.XML, any(), any(), any()) } throws SecurityException("Mocked SecurityException")

        val exporter = AppExporter(activity, { apps }, formatter, appSettings, crashReporter, testDispatchers)
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeToFile(uri, ExportFormat.XML)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { crashReporter.recordException(any<SecurityException>(), any()) }
        val toast = ShadowToast.getTextOfLatestToast()
        assertTrue("Toast was: $toast", toast?.toString()?.contains("Mocked SecurityException") == true)
    }

    @Test
    fun writeToFile_whenIOException_handlesError() {
        val formatter = mockk<ExportFormatter>()
        every { formatter.write(ExportFormat.XML, any(), any(), any()) } throws IOException("Mocked IOException")

        val exporter = AppExporter(activity, { apps }, formatter, appSettings, crashReporter, testDispatchers)
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeToFile(uri, ExportFormat.XML)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { crashReporter.recordException(any<IOException>(), any()) }
        val toast = ShadowToast.getTextOfLatestToast()
        assertTrue("Toast was: $toast", toast?.toString()?.contains("Mocked IOException") == true)
    }

    private fun createTestApp(
        packageName: String,
        name: String,
    ): App =
        App(
            packageName = packageName,
            name = name,
            versionName = "1.0.0",
            archived = false,
            minSdk = 24,
            targetSdk = 33,
            firstInstalled = 123456789L,
            lastUpdated = 123456789L,
            lastUsed = 123456789L,
            sizes = StorageUsage(),
            installerName = "Google Play",
            existsInStore = true,
            grantedPermissionsCount = 5,
            requestedPermissionsCount = 10,
            enabled = true,
            isDetailed = true,
        )
}
