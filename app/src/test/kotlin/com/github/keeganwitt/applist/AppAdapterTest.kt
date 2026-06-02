package com.github.keeganwitt.applist

import android.text.Spanned
import android.text.style.ClickableSpan
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `given item without infoUrl, when bound, then app info is not clickable`() {
        val holder = createViewHolder()
        adapter.submitList(listOf(AppItemUiModel("com.test.app", "App", "1.0.0")))

        adapter.onBindViewHolder(holder, 0)

        assertFalse(holder.binding.appInfo.isClickable)
    }

    @Test
    fun `given item with infoUrl, when bound and link clicked, then store URL click is reported`() {
        val url = "https://play.google.com/store/apps/details?id=com.test.app"
        val holder = createViewHolder()
        adapter.submitList(listOf(AppItemUiModel("com.test.app", "App", url, infoUrl = url)))

        adapter.onBindViewHolder(holder, 0)
        val spanned = holder.binding.appInfo.text as Spanned
        val spans = spanned.getSpans(0, spanned.length, ClickableSpan::class.java)
        spans.single().onClick(holder.binding.appInfo)

        assertTrue(holder.binding.appInfo.isClickable)
        verify { onClickListener.onStoreUrlClick(url) }
    }

    private fun createViewHolder(): AppAdapter.AppInfoViewHolder {
        val context = ApplicationProvider.getApplicationContext<TestAppListApplication>()
        val parent = FrameLayout(context)
        return adapter.onCreateViewHolder(parent, 0)
    }
}
