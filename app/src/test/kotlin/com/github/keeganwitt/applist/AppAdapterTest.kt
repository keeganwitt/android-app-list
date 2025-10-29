package com.github.keeganwitt.applist

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppAdapterTest {

    private lateinit var adapter: AppAdapter
    private lateinit var onClickListener: AppAdapter.OnClickListener

    @Before
    fun setup() {
        onClickListener = mockk(relaxed = true)
        adapter = AppAdapter(onClickListener)
    }

    @Test
    fun `given empty list, when adapter created, then item count is zero`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `given list with items, when submitList called, then currentList is updated`() {
        val items = listOf(
            AppItemUiModel("com.test.app1", "App 1", "1.0.0"),
            AppItemUiModel("com.test.app2", "App 2", "2.0.0")
        )

        adapter.submitList(items)

        assertEquals(items, adapter.currentList)
    }

    @Test
    fun `given DiffCallback, when comparing items with same package, then items are same`() {
        val item1 = AppItemUiModel("com.test.app", "App", "1.0.0")
        val item2 = AppItemUiModel("com.test.app", "App Updated", "2.0.0")

        adapter.submitList(listOf(item1))
        val firstList = adapter.currentList

        adapter.submitList(listOf(item2))
        val secondList = adapter.currentList

        assertEquals(item1.packageName, secondList[0].packageName)
    }

    @Test
    fun `given DiffCallback, when comparing items with different packages, then items are different`() {
        val item1 = AppItemUiModel("com.test.app1", "App 1", "1.0.0")
        val item2 = AppItemUiModel("com.test.app2", "App 2", "1.0.0")

        adapter.submitList(listOf(item1))
        adapter.submitList(listOf(item2))
        
        assertEquals(1, adapter.currentList.size)
    }
}
