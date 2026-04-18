package com.github.keeganwitt.applist

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    private lateinit var formatter: ExportFormatter
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
        formatter = mockk(relaxed = true)
    }

    @Test
    fun writeToFile_withXml_writesContent_andShowsSuccessToast() {
        val formatter = mockk<ExportFormatter>()
        val xmlOutput = "<?xml version=\"1.0\"?><apps></apps>"
        every { formatter.write(ExportFormat.XML, any(), any(), any(), any()) } answers {
            (args[1] as java.io.Writer).write(xmlOutput)
        }

        val exporter =
            AppExporter(activity, {
                apps
            }, formatter, appSettings, crashReporter, testDispatchers, mockk<ActivityResultRegistry>(relaxed = true))
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeToFile(uri, ExportFormat.XML)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.write(ExportFormat.XML, any(), any(), any(), any()) }
        val toast = ShadowToast.getTextOfLatestToast()
        assertTrue(toast == activity.getString(R.string.export_successful))
        assertEquals(xmlOutput, file.readText())
    }

    @Test
    fun `writeToFile when setting disabled then includeUsageStats is false`() {
        val formatter = mockk<ExportFormatter>(relaxed = true)
        every { appSettings.isIncludeUsageStatsInExportEnabled() } returns false
        val exporter =
            AppExporter(activity, {
                apps
            }, formatter, appSettings, crashReporter, testDispatchers, mockk<ActivityResultRegistry>(relaxed = true))
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeToFile(uri, ExportFormat.XML)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.write(ExportFormat.XML, any(), apps, false, any()) }
    }

    @Test
    fun onActivityResult_withXml_exportsXml() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val callbackSlot = slot<ActivityResultCallback<android.net.Uri?>>()
        every {
            registry.register(
                any(),
                any(),
                any<androidx.activity.result.contract.ActivityResultContract<String, android.net.Uri?>>(),
                capture(callbackSlot),
            )
        } returns mockk(relaxed = true)

        val formatter = mockk<ExportFormatter>(relaxed = true)
        val xmlOutput = "xml content"
        every { formatter.write(ExportFormat.XML, any(), any(), any(), any()) } answers {
            (args[1] as java.io.Writer).write(xmlOutput)
        }

        val exporter = AppExporter(activity, { apps }, formatter, appSettings, crashReporter, testDispatchers, registry)
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)
        callbackSlot.captured.onActivityResult(uri)

        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.write(ExportFormat.XML, any(), any(), any(), any()) }
        assertEquals(xmlOutput, file.readText())
        val toast = ShadowToast.getTextOfLatestToast()
        assertTrue("Expected successful toast but was: $toast", toast == activity.getString(R.string.export_successful))
    }

    @Test
    fun onActivityResult_withCsv_exportsCsv() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val callbackSlot = slot<ActivityResultCallback<android.net.Uri?>>()
        every {
            registry.register(
                any(),
                any(),
                any<androidx.activity.result.contract.ActivityResultContract<String, android.net.Uri?>>(),
                capture(callbackSlot),
            )
        } returns mockk(relaxed = true)

        val formatter = mockk<ExportFormatter>(relaxed = true)
        val csvOutput = "csv content"
        every { formatter.write(ExportFormat.CSV, any(), any(), any(), any()) } answers {
            (args[1] as java.io.Writer).write(csvOutput)
        }

        val exporter = AppExporter(activity, { apps }, formatter, appSettings, crashReporter, testDispatchers, registry)
        val file = File.createTempFile("apps", ".csv").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.export()
        Shadows.shadowOf(activity.mainLooper).idle()
        val dialog =
            org.robolectric.shadows.ShadowDialog
                .getLatestDialog() as androidx.appcompat.app.AlertDialog
        dialog.findViewById<android.widget.RadioButton>(R.id.radio_csv)?.isChecked = true
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
        Shadows.shadowOf(activity.mainLooper).idle()

        callbackSlot.captured.onActivityResult(uri)

        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.write(ExportFormat.CSV, any(), any(), any(), any()) }
        assertEquals(csvOutput, file.readText())
    }

    @Test
    fun writeToFile_whenFormatterThrowsIOException_handlesError() {
        val formatter = mockk<ExportFormatter>()
        val exception = IOException("Disk full")
        every { formatter.write(ExportFormat.XML, any(), any(), any(), any()) } throws exception

        val exporter =
            AppExporter(activity, {
                apps
            }, formatter, appSettings, crashReporter, testDispatchers, mockk<ActivityResultRegistry>(relaxed = true))
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeToFile(uri, ExportFormat.XML)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify(exactly = 0) { crashReporter.recordException(any(), any()) }
        val toast = ShadowToast.getTextOfLatestToast()
        assertEquals(activity.getString(R.string.export_failed, exception.message), toast)
    }

    @Test
    fun writeToFile_whenOpenOutputStreamReturnsNull_handlesError() {
        val authority = "com.github.keeganwitt.applist.test.null"
        Robolectric.setupContentProvider(NullContentProvider::class.java, authority)
        val exporter =
            AppExporter(activity, {
                apps
            }, ExportFormatter(), appSettings, crashReporter, testDispatchers, mockk<ActivityResultRegistry>(relaxed = true))
        val uri = Uri.parse("content://$authority/file")

        exporter.writeToFile(uri, ExportFormat.XML)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify(exactly = 0) { crashReporter.recordException(any(), any()) }
        val toast = ShadowToast.getTextOfLatestToast()
        assertEquals(activity.getString(R.string.export_failed, "Failed to open output stream"), toast)
    }

    class NullContentProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            p1: Array<out String>?,
            p2: String?,
            p3: Array<out String>?,
            p4: String?,
        ): Cursor? = null

        override fun getType(uri: Uri): String? = null

        override fun insert(
            uri: Uri,
            p1: ContentValues?,
        ): Uri? = null

        override fun delete(
            uri: Uri,
            p1: String?,
            p2: Array<out String>?,
        ): Int = 0

        override fun update(
            uri: Uri,
            p1: ContentValues?,
            p2: String?,
            p3: Array<out String>?,
        ): Int = 0

        override fun openFile(
            uri: Uri,
            mode: String,
        ): android.os.ParcelFileDescriptor? = null
    }

    @Test
    fun writeToFile_whenFormatterThrowsSecurityException_handlesError() {
        val formatter = mockk<ExportFormatter>()
        val exception = SecurityException("Permission denied")
        every { formatter.write(ExportFormat.XML, any(), any(), any(), any()) } throws exception

        val exporter =
            AppExporter(activity, {
                apps
            }, formatter, appSettings, crashReporter, testDispatchers, mockk<ActivityResultRegistry>(relaxed = true))
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeToFile(uri, ExportFormat.XML)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify(exactly = 0) { crashReporter.recordException(any(), any()) }
        val toast = ShadowToast.getTextOfLatestToast()
        assertEquals(activity.getString(R.string.export_failed, exception.message), toast)
    }

    @Test
    fun writeToFile_whenSecurityException_handlesError() {
        val formatter = mockk<ExportFormatter>()
        every { formatter.write(ExportFormat.XML, any(), any(), any(), any()) } throws SecurityException("Mocked SecurityException")

        val exporter =
            AppExporter(activity, {
                apps
            }, formatter, appSettings, crashReporter, testDispatchers, mockk<ActivityResultRegistry>(relaxed = true))
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeToFile(uri, ExportFormat.XML)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify(exactly = 0) { crashReporter.recordException(any(), any()) }
        val toast = ShadowToast.getTextOfLatestToast()
        assertTrue("Toast was: $toast", toast?.contains("Mocked SecurityException") == true)
    }

    @Test
    fun writeToFile_whenIOException_handlesError() {
        val formatter = mockk<ExportFormatter>()
        every { formatter.write(ExportFormat.XML, any(), any(), any(), any()) } throws IOException("Mocked IOException")

        val exporter =
            AppExporter(activity, {
                apps
            }, formatter, appSettings, crashReporter, testDispatchers, mockk<ActivityResultRegistry>(relaxed = true))
        val file = File.createTempFile("apps", ".xml").apply { deleteOnExit() }
        val uri = Uri.fromFile(file)

        exporter.writeToFile(uri, ExportFormat.XML)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify(exactly = 0) { crashReporter.recordException(any(), any()) }
        val toast = ShadowToast.getTextOfLatestToast()
        assertTrue("Toast was: $toast", toast?.contains("Mocked IOException") == true)
    }

    @Test
    fun writeToFile_whenOpenOutputStreamThrowsSecurityException_handlesError() {
        val authority = "com.github.keeganwitt.applist.test.security"
        Robolectric.setupContentProvider(SecurityExceptionContentProvider::class.java, authority)
        val exporter =
            AppExporter(activity, {
                apps
            }, ExportFormatter(), appSettings, crashReporter, testDispatchers, mockk<ActivityResultRegistry>(relaxed = true))
        val uri = Uri.parse("content://$authority/file")

        exporter.writeToFile(uri, ExportFormat.XML)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify(exactly = 0) { crashReporter.recordException(any(), any()) }
        val toast = ShadowToast.getTextOfLatestToast()
        assertEquals(activity.getString(R.string.export_failed, "Permission denied"), toast)
    }

    class SecurityExceptionContentProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            p1: Array<out String>?,
            p2: String?,
            p3: Array<out String>?,
            p4: String?,
        ): Cursor? = null

        override fun getType(uri: Uri): String? = null

        override fun insert(
            uri: Uri,
            p1: ContentValues?,
        ): Uri? = null

        override fun delete(
            uri: Uri,
            p1: String?,
            p2: Array<out String>?,
        ): Int = 0

        override fun update(
            uri: Uri,
            p1: ContentValues?,
            p2: String?,
            p3: Array<out String>?,
        ): Int = 0

        override fun openFile(
            uri: Uri,
            mode: String,
        ): android.os.ParcelFileDescriptor? = throw SecurityException("Permission denied")
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

    @Test
    fun onActivityResult_withNullUri_doesNothing() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val callbackSlot = slot<ActivityResultCallback<android.net.Uri?>>()
        every {
            registry.register(
                any(),
                any(),
                any<androidx.activity.result.contract.ActivityResultContract<String, android.net.Uri?>>(),
                capture(callbackSlot),
            )
        } returns mockk(relaxed = true)

        val formatter = mockk<ExportFormatter>(relaxed = true)
        val exporter = AppExporter(activity, { apps }, formatter, appSettings, crashReporter, testDispatchers, registry)

        callbackSlot.captured.onActivityResult(null)

        verify(exactly = 0) { formatter.write(any(), any(), any(), any(), any()) }
    }

    @Test
    fun onActivityResult_withUnknownMimeType_defaultsToXml() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val callbackSlot = slot<ActivityResultCallback<android.net.Uri?>>()
        every {
            registry.register(
                any(),
                any(),
                any<androidx.activity.result.contract.ActivityResultContract<String, android.net.Uri?>>(),
                capture(callbackSlot),
            )
        } returns mockk(relaxed = true)

        val formatter = mockk<ExportFormatter>(relaxed = true)
        val uri = android.net.Uri.parse("content://dummy/unknown")
        val contentResolver = mockk<android.content.ContentResolver>()
        every { contentResolver.getType(uri) } returns "application/octet-stream"
        every { contentResolver.openOutputStream(uri) } answers { java.io.ByteArrayOutputStream() }

        val spyActivity = io.mockk.spyk(activity)
        every { spyActivity.contentResolver } returns contentResolver
        every { spyActivity.runOnUiThread(any()) } answers { (args[0] as Runnable).run() }

        val exporter = AppExporter(spyActivity, { apps }, formatter, appSettings, crashReporter, testDispatchers, registry)

        callbackSlot.captured.onActivityResult(uri)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.write(ExportFormat.XML, any(), any(), any(), any()) }
    }

    @Test
    fun export_showsDialog() {
        val formatter = mockk<ExportFormatter>(relaxed = true)
        val exporter =
            AppExporter(activity, { apps }, formatter, appSettings, crashReporter, testDispatchers, mockk(relaxed = true))

        exporter.export()
        org.robolectric.Shadows
            .shadowOf(activity.mainLooper)
            .idle()

        val dialog =
            org.robolectric.shadows.ShadowDialog
                .getLatestDialog()
        assertNotNull(dialog)
        assertTrue(dialog.isShowing)
    }

    @Test
    fun export_selectsCsv_launchesCsvPicker() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val launcher = mockk<ActivityResultLauncher<String>>(relaxed = true)
        // Match the specific contract type used in AppExporter
        every {
            registry.register(
                any(),
                any(),
                any<androidx.activity.result.contract.ActivityResultContract<String, android.net.Uri?>>(),
                any(),
            )
        } returns launcher

        val exporter = AppExporter(activity, { apps }, formatter, appSettings, crashReporter, testDispatchers, registry)

        exporter.export()
        org.robolectric.Shadows
            .shadowOf(activity.mainLooper)
            .idle()

        val dialog =
            org.robolectric.shadows.ShadowDialog
                .getLatestDialog() as androidx.appcompat.app.AlertDialog
        val radioCsv = dialog.findViewById<android.widget.RadioButton>(R.id.radio_csv)
        radioCsv?.isChecked = true

        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
        org.robolectric.Shadows
            .shadowOf(activity.mainLooper)
            .idle()

        verify { launcher.launch(match { it.endsWith(".csv") }) }
    }

    @Test
    fun export_selectsHtml_launchesHtmlPicker() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val launcher = mockk<ActivityResultLauncher<String>>(relaxed = true)
        every {
            registry.register(
                any(),
                any(),
                any<androidx.activity.result.contract.ActivityResultContract<String, android.net.Uri?>>(),
                any(),
            )
        } returns launcher

        val exporter = AppExporter(activity, { apps }, formatter, appSettings, crashReporter, testDispatchers, registry)

        exporter.export()
        org.robolectric.Shadows
            .shadowOf(activity.mainLooper)
            .idle()

        val dialog =
            org.robolectric.shadows.ShadowDialog
                .getLatestDialog() as androidx.appcompat.app.AlertDialog
        val radioHtml = dialog.findViewById<android.widget.RadioButton>(R.id.radio_html)
        radioHtml?.isChecked = true

        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
        org.robolectric.Shadows
            .shadowOf(activity.mainLooper)
            .idle()

        verify { launcher.launch(match { it.endsWith(".html") }) }
    }

    @Test
    fun export_selectsTsv_launchesTsvPicker() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val launcher = mockk<ActivityResultLauncher<String>>(relaxed = true)
        every {
            registry.register(
                any(),
                any(),
                any<androidx.activity.result.contract.ActivityResultContract<String, android.net.Uri?>>(),
                any(),
            )
        } returns launcher

        val exporter = AppExporter(activity, { apps }, formatter, appSettings, crashReporter, testDispatchers, registry)

        exporter.export()
        org.robolectric.Shadows
            .shadowOf(activity.mainLooper)
            .idle()

        val dialog =
            org.robolectric.shadows.ShadowDialog
                .getLatestDialog() as androidx.appcompat.app.AlertDialog
        val radioTsv = dialog.findViewById<android.widget.RadioButton>(R.id.radio_tsv)
        radioTsv?.isChecked = true

        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
        org.robolectric.Shadows
            .shadowOf(activity.mainLooper)
            .idle()

        verify { launcher.launch(match { it.endsWith(".tsv") }) }
    }
}
