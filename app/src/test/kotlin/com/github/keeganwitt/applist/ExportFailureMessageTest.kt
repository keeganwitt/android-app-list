package com.github.keeganwitt.applist

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestAppListApplication::class)
class ExportFailureMessageTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `missing error detail uses a generic failure message`() {
        assertEquals("Export failed", exportFailureMessage(context, null))
        assertEquals("Export failed", exportFailureMessage(context, ""))
    }

    @Test
    fun `available error detail is included in the failure message`() {
        assertEquals("Export failed: Disk full", exportFailureMessage(context, "Disk full"))
    }
}
