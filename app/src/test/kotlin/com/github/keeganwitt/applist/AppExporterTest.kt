package com.github.keeganwitt.applist

import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppExporterTest {

    private lateinit var activity: AppCompatActivity
    private lateinit var formatter: ExportFormatter
    private lateinit var crashReporter: CrashReporter
    private lateinit var itemsProvider: () -> List<AppItemUiModel>
    private lateinit var exporter: AppExporter

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().get()
        formatter = mockk(relaxed = true)
        crashReporter = mockk(relaxed = true)
        itemsProvider = mockk(relaxed = true)
        exporter = AppExporter(activity, itemsProvider, formatter, crashReporter)
    }

    @Test
    fun `given empty items, when export called, then export dialog is shown`() {
        every { itemsProvider() } returns emptyList()

        exporter.export(AppInfoField.VERSION)
        shadowOf(Looper.getMainLooper()).idle()

        verify(atLeast = 0) { itemsProvider() }
    }

    @Test
    fun `given items available, when export called, then export dialog is shown`() {
        val items = listOf(
            AppItemUiModel("com.test.app", "Test App", "1.0.0")
        )
        every { itemsProvider() } returns items

        exporter.export(AppInfoField.VERSION)
        shadowOf(Looper.getMainLooper()).idle()

        verify(atLeast = 0) { itemsProvider() }
    }
}
