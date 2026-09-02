package com.github.keeganwitt.applist

import android.app.Activity
import android.content.Intent
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestAppListApplication::class)
class AppExporterTest {
    private lateinit var activity: AppCompatActivity
    private lateinit var crashReporter: CrashReporter
    private val results = mutableListOf<Uri?>()
    private val launchFailures = mutableListOf<String?>()

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        crashReporter = mockk(relaxed = true)
        results.clear()
        launchFailures.clear()
    }

    @Test
    fun `export launches picker with requested MIME type and filename`() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val contract = slot<ActivityResultContract<ExportFormat, Uri?>>()
        val launcher = mockk<ActivityResultLauncher<ExportFormat>>(relaxed = true)
        every { registry.register(any(), any(), capture(contract), any()) } returns launcher
        val exporter = createExporter(registry)

        ExportFormat.entries.forEach { format ->
            exporter.export(format)
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
    fun `picker result is forwarded without retaining activity export state`() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val callback = slot<ActivityResultCallback<Uri?>>()
        every {
            registry.register(any(), any(), any<ActivityResultContract<ExportFormat, Uri?>>(), capture(callback))
        } returns mockk(relaxed = true)
        createExporter(registry)
        val uri = Uri.parse("content://test/export")

        callback.captured.onActivityResult(uri)
        callback.captured.onActivityResult(null)

        assertEquals(listOf(uri, null), results)
    }

    @Test
    fun `picker launch failure is forwarded and recorded`() {
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val launcher = mockk<ActivityResultLauncher<ExportFormat>>()
        val exception = IllegalStateException("Picker unavailable")
        every {
            registry.register(any(), any(), any<ActivityResultContract<ExportFormat, Uri?>>(), any())
        } returns launcher
        every { launcher.launch(ExportFormat.XML) } throws exception
        val exporter = createExporter(registry)

        exporter.export(ExportFormat.XML)

        verify { crashReporter.recordException(exception, "Error opening export picker") }
        assertEquals(listOf("Picker unavailable"), launchFailures)
    }

    private fun createExporter(registry: ActivityResultRegistry): AppExporter =
        AppExporter(
            activity = activity,
            crashReporter = crashReporter,
            registry = registry,
            onResult = results::add,
            onLaunchFailure = launchFailures::add,
        )
}
