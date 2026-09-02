package com.github.keeganwitt.applist

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
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
class ExportAppAdapterTest {
    private val selectedPackages = mutableListOf<String>()
    private lateinit var adapter: ExportAppAdapter

    @Before
    fun setUp() {
        selectedPackages.clear()
        adapter = ExportAppAdapter(selectedPackages::add)
    }

    @Test
    fun `bound row exposes app identity classification and deselect action`() {
        val holder = createViewHolder()
        adapter.submitList(
            listOf(
                ExportAppItemUiModel(
                    packageName = "com.example.app",
                    appName = "Example",
                    isUserInstalled = true,
                    isArchived = false,
                    isSelected = true,
                ),
            ),
        )

        adapter.onBindViewHolder(holder, 0)

        assertEquals(
            "Example",
            holder.binding.appName.text
                .toString(),
        )
        assertEquals(
            "com.example.app",
            holder.binding.packageName.text
                .toString(),
        )
        assertEquals(
            "User app",
            holder.binding.appClassification.text
                .toString(),
        )
        assertTrue(holder.binding.appSelectionCheckbox.isChecked)
        assertEquals(
            "Example, com.example.app, User app, Deselect Example",
            holder.binding.root.contentDescription
                .toString(),
        )
    }

    @Test
    fun `recycled row clears archived and selected state`() {
        val holder = createViewHolder()
        adapter.submitList(
            listOf(
                ExportAppItemUiModel("system.archived", "Archived", false, true, true),
                ExportAppItemUiModel("user.active", "Active", true, false, false),
            ),
        )
        adapter.onBindViewHolder(holder, 0)
        assertEquals(
            "System app • Archived",
            holder.binding.appClassification.text
                .toString(),
        )

        adapter.onBindViewHolder(holder, 1)

        assertEquals(
            "User app",
            holder.binding.appClassification.text
                .toString(),
        )
        assertFalse(holder.binding.appSelectionCheckbox.isChecked)
        assertEquals(
            "Active, user.active, User app, Select Active",
            holder.binding.root.contentDescription
                .toString(),
        )
    }

    @Test
    fun `row and checkbox clicks report the bound package`() {
        val holder = createViewHolder()
        adapter.submitList(
            listOf(
                ExportAppItemUiModel("com.example.app", "Example", true, false, false),
            ),
        )
        adapter.onBindViewHolder(holder, 0)

        holder.binding.root.performClick()
        holder.binding.appSelectionCheckbox.performClick()

        assertEquals(listOf("com.example.app", "com.example.app"), selectedPackages)
    }

    @Test
    fun `row is the single accessible checkable selection action`() {
        val holder = createViewHolder()
        adapter.submitList(
            listOf(
                ExportAppItemUiModel("com.example.app", "Example", false, true, true),
            ),
        )
        adapter.onBindViewHolder(holder, 0)

        val rowInfo = holder.binding.root.createAccessibilityNodeInfo()
        assertTrue(rowInfo.isCheckable)
        assertTrue(rowInfo.isChecked)
        assertEquals(
            1,
            rowInfo.actionList.count { it.id == AccessibilityNodeInfo.ACTION_CLICK },
        )
        assertEquals("Example, com.example.app, System app, Archived, Deselect Example", rowInfo.contentDescription)
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, holder.binding.appSelectionCheckbox.importantForAccessibility)
        assertFalse(holder.binding.appSelectionCheckbox.isFocusable)
    }

    private fun createViewHolder(): ExportAppAdapter.ExportAppViewHolder {
        val context = ApplicationProvider.getApplicationContext<TestAppListApplication>()
        val themedContext = ContextThemeWrapper(context, R.style.Theme_AppList_Settings)
        return adapter.onCreateViewHolder(FrameLayout(themedContext), 0)
    }
}
