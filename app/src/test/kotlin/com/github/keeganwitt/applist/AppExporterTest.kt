package com.github.keeganwitt.applist

import android.app.Activity
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
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
    private lateinit var appSettings: AppSettings
    private lateinit var crashReporter: CrashReporter
    private val outcomes = mutableListOf<ExportOutcome>()
    private val apps = listOf(app("beta", "Beta"), app("alpha", "Alpha"))
    private val dispatchers =
        object : DispatcherProvider {
            override val io = Dispatchers.Unconfined
            override val main = Dispatchers.Unconfined
            override val default = Dispatchers.Unconfined
        }

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        appSettings = mockk(relaxed = true)
        crashReporter = mockk(relaxed = true)
        every { appSettings.isIncludeUsageStatsInExportEnabled() } returns true
        outcomes.clear()
    }

    @Test
    fun `export launches picker with requested MIME type and filename`() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val contract = slot<ActivityResultContract<ExportFormat, Uri?>>()
        val launcher = mockk<ActivityResultLauncher<ExportFormat>>(relaxed = true)
        every { registry.register(any(), any(), capture(contract), any()) } returns launcher
        val exporter = createExporter(registry, mockk(relaxed = true))

        ExportFormat.entries.forEach { format ->
            exporter.export(format, apps)
            verify { launcher.launch(format) }
            val intent = contract.captured.createIntent(activity, format)
            assertEquals(Intent.ACTION_CREATE_DOCUMENT, intent.action)
            assertTrue(intent.categories?.contains(Intent.CATEGORY_OPENABLE) == true)
            assertEquals(format.mimeType, intent.type)
            assertEquals("app-list.${format.extension}", intent.getStringExtra(Intent.EXTRA_TITLE))
        }

        val uri = Uri.parse("content://test/export")
        assertEquals(uri, contract.captured.parseResult(Activity.RESULT_OK, Intent().setData(uri)))
        assertEquals(null, contract.captured.parseResult(Activity.RESULT_CANCELED, null))
    }

    @Test
    fun `picker result writes explicit snapshot in supplied order for every format`() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val callback = slot<ActivityResultCallback<Uri?>>()
        every {
            registry.register(any(), any(), any<ActivityResultContract<ExportFormat, Uri?>>(), capture(callback))
        } returns mockk(relaxed = true)
        val formatter = mockk<ExportFormatter>(relaxed = true)
        val exporter = createExporter(registry, formatter)

        ExportFormat.entries.forEach { format ->
            val file = File.createTempFile("app-export", ".${format.extension}").apply { deleteOnExit() }
            val mutableSnapshot = apps.toMutableList()
            exporter.export(format, mutableSnapshot)
            mutableSnapshot += app("later", "Later")
            callback.captured.onActivityResult(Uri.fromFile(file))
            Shadows.shadowOf(activity.mainLooper).idle()

            verify { formatter.write(format, any(), apps, true, any()) }
        }
        assertEquals(List(ExportFormat.entries.size) { ExportOutcome.SUCCESS }, outcomes)
    }

    @Test
    fun `usage-stat setting is preserved`() {
        every { appSettings.isIncludeUsageStatsInExportEnabled() } returns false
        val formatter = mockk<ExportFormatter>(relaxed = true)
        val exporter = createExporter(mockk(relaxed = true), formatter)
        val file = File.createTempFile("app-export", ".xml").apply { deleteOnExit() }

        exporter.writeToFile(Uri.fromFile(file), ExportFormat.XML, apps)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.write(ExportFormat.XML, any(), apps, false, any()) }
    }

    @Test
    fun `successful write reports success and shows existing message`() {
        val formatter = mockk<ExportFormatter>()
        every { formatter.write(ExportFormat.XML, any(), any(), any(), any()) } answers {
            (args[1] as java.io.Writer).write("written")
        }
        val exporter = createExporter(mockk(relaxed = true), formatter)
        val file = File.createTempFile("app-export", ".xml").apply { deleteOnExit() }

        exporter.writeToFile(Uri.fromFile(file), ExportFormat.XML, apps)
        Shadows.shadowOf(activity.mainLooper).idle()

        assertEquals("written", file.readText())
        assertEquals(listOf(ExportOutcome.SUCCESS), outcomes)
        assertEquals(activity.getString(R.string.export_successful), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `picker cancellation reports canceled without writing`() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val callback = slot<ActivityResultCallback<Uri?>>()
        every {
            registry.register(any(), any(), any<ActivityResultContract<ExportFormat, Uri?>>(), capture(callback))
        } returns mockk(relaxed = true)
        val formatter = mockk<ExportFormatter>(relaxed = true)
        val exporter = createExporter(registry, formatter)

        exporter.export(ExportFormat.XML, apps)
        callback.captured.onActivityResult(null)

        verify(exactly = 0) { formatter.write(any(), any(), any(), any(), any()) }
        assertEquals(listOf(ExportOutcome.CANCELED), outcomes)
    }

    @Test
    fun `restored pending request writes the retained snapshot`() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val callback = slot<ActivityResultCallback<Uri?>>()
        every {
            registry.register(any(), any(), any<ActivityResultContract<ExportFormat, Uri?>>(), capture(callback))
        } returns mockk(relaxed = true)
        val formatter = mockk<ExportFormatter>(relaxed = true)
        val exporter = createExporter(registry, formatter)
        val request = ExportRequest(ExportFormat.TSV, apps)
        val file = File.createTempFile("restored-export", ".tsv").apply { deleteOnExit() }

        exporter.restorePendingRequest(request)
        callback.captured.onActivityResult(Uri.fromFile(file))
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { formatter.write(ExportFormat.TSV, any(), apps, true, any()) }
        assertEquals(listOf(ExportOutcome.SUCCESS), outcomes)
    }

    @Test
    fun `empty request is rejected without opening picker`() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val launcher = mockk<ActivityResultLauncher<ExportFormat>>(relaxed = true)
        every {
            registry.register(any(), any(), any<ActivityResultContract<ExportFormat, Uri?>>(), any())
        } returns launcher
        val exporter = createExporter(registry, mockk(relaxed = true))

        exporter.export(ExportFormat.XML, emptyList())

        verify(exactly = 0) { launcher.launch(any()) }
        assertEquals(listOf(ExportOutcome.FAILURE), outcomes)
    }

    @Test
    fun `picker launch failure reports failure and records exception`() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val launcher = mockk<ActivityResultLauncher<ExportFormat>>()
        val exception = IllegalStateException("Picker unavailable")
        every {
            registry.register(any(), any(), any<ActivityResultContract<ExportFormat, Uri?>>(), any())
        } returns launcher
        every { launcher.launch(ExportFormat.XML) } throws exception
        val exporter = createExporter(registry, mockk(relaxed = true))

        exporter.export(ExportFormat.XML, apps)

        verify { crashReporter.recordException(exception, "Error opening export picker") }
        assertEquals(listOf(ExportOutcome.FAILURE), outcomes)
        assertEquals(
            activity.getString(R.string.export_failed, exception.message),
            ShadowToast.getTextOfLatestToast(),
        )
    }

    @Test
    fun `formatter failure is reported and records exception`() {
        val exception = IOException("Disk full")
        val formatter = mockk<ExportFormatter>()
        every { formatter.write(any(), any(), any(), any(), any()) } throws exception
        val exporter = createExporter(mockk(relaxed = true), formatter)
        val file = File.createTempFile("app-export", ".xml").apply { deleteOnExit() }

        exporter.writeToFile(Uri.fromFile(file), ExportFormat.XML, apps)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { crashReporter.recordException(exception, "Error exporting XML") }
        assertEquals(listOf(ExportOutcome.FAILURE), outcomes)
        assertEquals(activity.getString(R.string.export_failed, exception.message), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `output stream failure is reported and records exception`() {
        val authority = "com.github.keeganwitt.applist.test.null-output"
        Robolectric.setupContentProvider(NullOutputContentProvider::class.java, authority)
        val exporter = createExporter(mockk(relaxed = true), ExportFormatter())

        exporter.writeToFile(Uri.parse("content://$authority/file"), ExportFormat.XML, apps)
        Shadows.shadowOf(activity.mainLooper).idle()

        verify { crashReporter.recordException(any<IOException>(), "Error exporting XML") }
        assertEquals(listOf(ExportOutcome.FAILURE), outcomes)
        assertEquals(
            activity.getString(R.string.export_failed, "Failed to open output stream"),
            ShadowToast.getTextOfLatestToast(),
        )
    }

    private fun createExporter(
        registry: ActivityResultRegistry,
        formatter: ExportFormatter,
    ): AppExporter =
        AppExporter(
            activity = activity,
            formatter = formatter,
            appSettings = appSettings,
            crashReporter = crashReporter,
            dispatchers = dispatchers,
            registry = registry,
            onOutcome = outcomes::add,
        )

    class NullOutputContentProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? = null

        override fun getType(uri: Uri): String? = null

        override fun insert(
            uri: Uri,
            values: ContentValues?,
        ): Uri? = null

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        override fun openFile(
            uri: Uri,
            mode: String,
        ): android.os.ParcelFileDescriptor? = null
    }

    companion object {
        private fun app(
            packageName: String,
            name: String,
        ): App =
            App(
                packageName = packageName,
                name = name,
                versionName = "1.0",
                archived = false,
                minSdk = 24,
                targetSdk = 37,
                firstInstalled = 1L,
                lastUpdated = 1L,
                lastUsed = 1L,
                sizes = StorageUsage(),
                installerName = null,
                existsInStore = null,
                grantedPermissionsCount = 0,
                requestedPermissionsCount = 0,
                enabled = true,
                isDetailed = true,
            )
    }
}
