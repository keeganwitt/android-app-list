package com.github.keeganwitt.applist

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TapjackingProtectionTest {

    @Test
    fun `activity_main layout should have filterTouchesWhenObscured set to true`() {
        val context = ApplicationProvider.getApplicationContext<AppListApplication>()
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.activity_main, null)

        assertTrue("activity_main.xml root view should filter touches when obscured", view.filterTouchesWhenObscured)
    }

    @Test
    fun `activity_settings layout should have filterTouchesWhenObscured set to true`() {
        // MaterialToolbar requires a valid Material theme
        val context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.Theme_AppList_Settings
        )
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.activity_settings, null)

        assertTrue("activity_settings.xml root view should filter touches when obscured", view.filterTouchesWhenObscured)
    }

    @Test
    fun `dialog_export_type layout should have filterTouchesWhenObscured set to true`() {
        val context = ApplicationProvider.getApplicationContext<AppListApplication>()
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_export_type, null)

        assertTrue("dialog_export_type.xml root view should filter touches when obscured", view.filterTouchesWhenObscured)
    }
}
