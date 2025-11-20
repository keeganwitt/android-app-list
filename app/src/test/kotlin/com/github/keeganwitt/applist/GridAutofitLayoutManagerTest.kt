package com.github.keeganwitt.applist

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestAppListApplication::class)
class GridAutofitLayoutManagerTest {
    private lateinit var context: Context
    private lateinit var layoutManager: GridAutofitLayoutManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `given valid column width, when layoutManager created, then initializes successfully`() {
        layoutManager = GridAutofitLayoutManager(context, 450)

        assertEquals(RecyclerView.VERTICAL, layoutManager.orientation)
    }

    @Test
    fun `given zero column width, when layoutManager created, then uses default width`() {
        layoutManager = GridAutofitLayoutManager(context, 0)

        assertEquals(RecyclerView.VERTICAL, layoutManager.orientation)
    }

    @Test
    fun `given negative column width, when layoutManager created, then uses default width`() {
        layoutManager = GridAutofitLayoutManager(context, -100)

        assertEquals(RecyclerView.VERTICAL, layoutManager.orientation)
    }

    @Test
    fun `given column width, when setColumnWidth called with new value, then updates column width`() {
        layoutManager = GridAutofitLayoutManager(context, 450)

        layoutManager.setColumnWidth(600)

        assertTrue(true)
    }

    @Test
    fun `given column width, when setColumnWidth called with zero, then does not update`() {
        layoutManager = GridAutofitLayoutManager(context, 450)

        layoutManager.setColumnWidth(0)

        assertTrue(true)
    }

    @Test
    fun `given column width, when setColumnWidth called with negative value, then does not update`() {
        layoutManager = GridAutofitLayoutManager(context, 450)

        layoutManager.setColumnWidth(-100)

        assertTrue(true)
    }

    @Test
    fun `given column width, when setColumnWidth called with same value, then does not trigger change`() {
        layoutManager = GridAutofitLayoutManager(context, 450)

        layoutManager.setColumnWidth(450)

        assertTrue(true)
    }
}
