package com.github.keeganwitt.applist.utils

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updatePadding

internal fun View.applySafeDrawingInsets() {
    val initialPadding = Insets.of(paddingLeft, paddingTop, paddingRight, paddingBottom)
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val safeDrawingInsets =
            windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
        view.updatePadding(
            left = initialPadding.left + safeDrawingInsets.left,
            top = initialPadding.top + safeDrawingInsets.top,
            right = initialPadding.right + safeDrawingInsets.right,
            bottom = initialPadding.bottom + safeDrawingInsets.bottom,
        )
        windowInsets
    }
    doOnAttach(ViewCompat::requestApplyInsets)
}
