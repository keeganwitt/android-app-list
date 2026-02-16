package com.github.keeganwitt.applist

import android.content.Context
import android.util.TypedValue
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(application = TestAppListApplication::class)
class GridAutofitLayoutManagerTest {
    private lateinit var context: Context
    private lateinit var layoutManager: GridAutofitLayoutManager
    private lateinit var recyclerView: RecyclerView

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        recyclerView = RecyclerView(context)
        recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(android.view.View(parent.context)) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount(): Int = 20
        }
    }

    @Test
    fun `given valid column width, when layout happens, then span count is correct`() {
        layoutManager = GridAutofitLayoutManager(context, 100)
        recyclerView.layoutManager = layoutManager

        // 400 width, 100 column width -> 4 spans
        triggerLayout(400, 1000)

        assertEquals(4, layoutManager.spanCount)
    }

    @Test
    fun `given zero column width, when layout happens, then uses default width`() {
        layoutManager = GridAutofitLayoutManager(context, 0)
        recyclerView.layoutManager = layoutManager

        // Default width is 48dp.
        val expectedWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            48f,
            context.resources.displayMetrics
        ).roundToInt()

        // Use a width that is clearly divisible or test logic
        // E.g. 5 * expectedWidth
        val testWidth = expectedWidth * 5
        triggerLayout(testWidth, 1000)

        assertEquals(5, layoutManager.spanCount)
    }

    @Test
    fun `given negative column width, when layout happens, then uses default width`() {
        layoutManager = GridAutofitLayoutManager(context, -100)
        recyclerView.layoutManager = layoutManager

        // Default width is 48dp.
        val expectedWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            48f,
            context.resources.displayMetrics
        ).roundToInt()

        val testWidth = expectedWidth * 3
        triggerLayout(testWidth, 1000)

        assertEquals(3, layoutManager.spanCount)
    }

    @Test
    fun `given column width, when setColumnWidth called with new value, then updates span count`() {
        layoutManager = GridAutofitLayoutManager(context, 100)
        recyclerView.layoutManager = layoutManager

        triggerLayout(400, 1000)
        assertEquals(4, layoutManager.spanCount)

        layoutManager.setColumnWidth(200)
        triggerLayout(400, 1000)

        assertEquals(2, layoutManager.spanCount)
    }

    @Test
    fun `given layout done, when resized, then updates span count`() {
        layoutManager = GridAutofitLayoutManager(context, 100)
        recyclerView.layoutManager = layoutManager

        triggerLayout(400, 1000)
        assertEquals(4, layoutManager.spanCount)

        triggerLayout(200, 1000)
        assertEquals(2, layoutManager.spanCount)
    }

    @Test
    fun `given column width larger than total width, when layout happens, then span count is 1`() {
        layoutManager = GridAutofitLayoutManager(context, 200)
        recyclerView.layoutManager = layoutManager

        triggerLayout(100, 1000)

        assertEquals(1, layoutManager.spanCount)
    }

    @Test
    fun `given padding, when layout happens, then available space is reduced`() {
        layoutManager = GridAutofitLayoutManager(context, 100)
        recyclerView.layoutManager = layoutManager

        // 400 total width - 50 padding left - 50 padding right = 300 available
        // 300 / 100 = 3 spans
        recyclerView.setPadding(50, 0, 50, 0)
        triggerLayout(400, 1000)

        assertEquals(3, layoutManager.spanCount)
    }

    @Test
    fun `given valid column width, when layoutManager created, then initializes successfully`() {
        layoutManager = GridAutofitLayoutManager(context, 450)

        assertEquals(RecyclerView.VERTICAL, layoutManager.orientation)
    }

    private fun triggerLayout(width: Int, height: Int) {
        recyclerView.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY)
        )
        recyclerView.layout(0, 0, width, height)
    }
}
