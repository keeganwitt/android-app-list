package com.github.keeganwitt.applist.utils

import android.content.Context
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WindowInsetsExtensionsTest {
    @Test
    fun `safe drawing insets preserve original padding and exclude system overlays`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context).apply { setPadding(1, 2, 3, 4) }
        view.applySafeDrawingInsets()
        val safeTypes =
            WindowInsetsCompat.Type.statusBars() or
                WindowInsetsCompat.Type.navigationBars() or
                WindowInsetsCompat.Type.captionBar() or
                WindowInsetsCompat.Type.displayCutout()
        val insets =
            WindowInsetsCompat
                .Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 20, 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 40))
                .setInsets(WindowInsetsCompat.Type.captionBar(), Insets.of(10, 0, 0, 0))
                .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(0, 0, 30, 0))
                .setInsets(WindowInsetsCompat.Type.systemOverlays(), Insets.of(100, 100, 100, 100))
                .build()

        ViewCompat.dispatchApplyWindowInsets(view, insets)

        assertEquals(Insets.of(10, 20, 30, 40), insets.getInsets(safeTypes))
        assertEquals(11, view.paddingLeft)
        assertEquals(22, view.paddingTop)
        assertEquals(33, view.paddingRight)
        assertEquals(44, view.paddingBottom)
    }

    @Test
    fun `reapplying safe drawing insets does not accumulate padding`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context).apply { setPadding(1, 2, 3, 4) }
        view.applySafeDrawingInsets()
        val insets =
            WindowInsetsCompat
                .Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 20, 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 40))
                .build()

        ViewCompat.dispatchApplyWindowInsets(view, insets)
        ViewCompat.dispatchApplyWindowInsets(view, insets)

        assertEquals(1, view.paddingLeft)
        assertEquals(22, view.paddingTop)
        assertEquals(3, view.paddingRight)
        assertEquals(44, view.paddingBottom)
    }
}
