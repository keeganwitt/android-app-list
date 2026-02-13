package com.github.keeganwitt.applist

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.github.keeganwitt.applist.AppInfoField.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SummaryAdapterTest {

    @Test
    fun `getItemCount returns correct size`() {
        val items = listOf(
            SummaryItem(ENABLED, mapOf()),
            SummaryItem(ARCHIVED, mapOf())
        )
        val adapter = SummaryAdapter(items)
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `onBindViewHolder binds data correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val buckets = mapOf("Label1" to 5, "Label2" to 10)
        val items = listOf(SummaryItem(VERSION, buckets))
        val adapter = SummaryAdapter(items)

        val parent = LinearLayout(context)
        val viewHolder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(viewHolder, 0)

        assertEquals(context.getString(R.string.appInfoField_version), viewHolder.title.text)
        assertEquals(2, viewHolder.container.childCount)

        val row1 = viewHolder.container.getChildAt(0) as LinearLayout
        val label1 = row1.getChildAt(0) as TextView
        val count1 = row1.getChildAt(1) as TextView
        assertEquals("Label1", label1.text)
        assertEquals("5", count1.text)

        val row2 = viewHolder.container.getChildAt(1) as LinearLayout
        val label2 = row2.getChildAt(0) as TextView
        val count2 = row2.getChildAt(1) as TextView
        assertEquals("Label2", label2.text)
        assertEquals("10", count2.text)
    }
}
