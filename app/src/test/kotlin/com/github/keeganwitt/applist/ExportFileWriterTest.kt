package com.github.keeganwitt.applist

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(application = TestAppListApplication::class)
class ExportFileWriterTest {
    private lateinit var settings: AppSettings
    private lateinit var crashReporter: CrashReporter
    private val request = ExportRequest(ExportFormat.XML, listOf(app("beta", "Beta"), app("alpha", "Alpha")))

    @Before
    fun setUp() {
        settings = mockk(relaxed = true)
        crashReporter = mockk(relaxed = true)
        every { settings.isIncludeUsageStatsInExportEnabled() } returns true
    }

    @Test
    fun `write preserves explicit ordered snapshot for every format`() =
        runTest {
            ExportFormat.entries.forEach { format ->
                val file = File.createTempFile("app-export", ".${format.extension}").apply { deleteOnExit() }
                val formatRequest = request.copy(format = format)

                val completion = createWriter(ExportFormatter()).write(Uri.fromFile(file), formatRequest)

                assertEquals(ExportOutcome.SUCCESS, completion.outcome)
                val text = file.readText()
                assertTrue(text.indexOf("Beta") < text.indexOf("Alpha"))
                assertTrue(text.indexOf("beta") < text.indexOf("alpha"))
            }
        }

    @Test
    fun `formatter failure is returned and recorded`() =
        runTest {
            val exception = IOException("Disk full")
            val formatter = mockk<ExportFormatter>()
            every { formatter.write(any(), any(), any(), any(), any()) } throws exception
            val file = File.createTempFile("app-export", ".xml").apply { deleteOnExit() }

            val completion = createWriter(formatter).write(Uri.fromFile(file), request)

            verify { crashReporter.recordException(exception, "Error exporting XML") }
            assertEquals(ExportCompletion(ExportOutcome.FAILURE, "Disk full"), completion)
        }

    @Test
    fun `write reads usage stat setting at write time`() =
        runTest {
            every { settings.isIncludeUsageStatsInExportEnabled() } returns false
            val file = File.createTempFile("app-export", ".xml").apply { deleteOnExit() }

            createWriter(ExportFormatter()).write(Uri.fromFile(file), request)

            val text = file.readText()
            assertFalse(text.contains("<APP_SIZE>"))
            assertTrue(text.contains("<VERSION>"))
        }

    @Test
    fun `output stream failure is returned and recorded`() =
        runTest {
            val authority = "com.github.keeganwitt.applist.test.null-output"
            Robolectric.setupContentProvider(NullOutputContentProvider::class.java, authority)

            val completion =
                createWriter(ExportFormatter()).write(Uri.parse("content://$authority/file"), request)

            verify { crashReporter.recordException(any<IOException>(), "Error exporting XML") }
            assertEquals(ExportCompletion(ExportOutcome.FAILURE, "Failed to open output stream"), completion)
        }

    private fun createWriter(formatter: ExportFormatter): DefaultExportFileWriter {
        val context = ApplicationProvider.getApplicationContext<TestAppListApplication>()
        return DefaultExportFileWriter(
            contentResolver = context.contentResolver,
            formatter = formatter,
            appSettings = settings,
            loadingFailedValue = "Loading failed",
            crashReporter = crashReporter,
        )
    }

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
