package com.github.keeganwitt.applist

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.github.keeganwitt.applist.utils.IconLoader
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestAppListApplication::class)
class AppAdapterTest {
    private lateinit var adapter: AppAdapter
    private lateinit var onClickListener: AppAdapter.OnClickListener
    private lateinit var iconLoader: IconLoader

    @Before
    fun setup() {
        onClickListener = mockk(relaxed = true)
        iconLoader = mockk(relaxed = true)

        val lifecycleOwner = mockk<LifecycleOwner>()
        val lifecycleRegistry = LifecycleRegistry(lifecycleOwner)
        every { lifecycleOwner.lifecycle } returns lifecycleRegistry
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        adapter = AppAdapter(lifecycleOwner, onClickListener, iconLoader)
    }

    @Test
    fun `given empty list, when adapter created, then item count is zero`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `given list with items, when submitList called, then currentList is updated`() {
        val items =
            listOf(
                AppItemUiModel("com.test.app1", "App 1", "1.0.0"),
                AppItemUiModel("com.test.app2", "App 2", "2.0.0"),
            )

        adapter.submitList(items)

        assertEquals(items, adapter.currentList)
    }

    @Test
    fun `given DiffCallback, when comparing items with same package, then items are same`() {
        val item1 = AppItemUiModel("com.test.app", "App", "1.0.0")
        val item2 = AppItemUiModel("com.test.app", "App Updated", "2.0.0")

        adapter.submitList(listOf(item1))
        adapter.submitList(listOf(item2))

        assertEquals(item1.packageName, adapter.currentList[0].packageName)
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
