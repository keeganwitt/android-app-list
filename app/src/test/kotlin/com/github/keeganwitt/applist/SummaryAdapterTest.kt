package com.github.keeganwitt.applist

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
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
        val buckets = listOf("Label1" to 5, "Label2" to 10)
        val adapter = SummaryAdapter(buckets)
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `onBindViewHolder binds data correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val buckets = listOf("Label1" to 5)
        val adapter = SummaryAdapter(buckets)

        val parent = LinearLayout(context)
        val viewHolder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(viewHolder, 0)

        assertEquals("Label1", viewHolder.label.text)
        assertEquals("5", viewHolder.count.text)
    }
}
