package com.github.keeganwitt.applist

import android.content.Context
import android.util.TypedValue
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import kotlin.math.max

class GridAutofitLayoutManager : GridLayoutManager {
    private var columnWidth = 0
    private var isColumnWidthChanged = true
    private var lastWidth = 0
    private var lastHeight = 0

    constructor(context: Context, columnWidth: Int) : super(context, 1) {
        this.columnWidth = getValidColumnWidth(context, columnWidth)
    }

    private fun getValidColumnWidth(
        context: Context,
        columnWidth: Int,
    ): Int {
        return if (columnWidth <= 0) {
            TypedValue
                .applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    48f,
                    context.resources.displayMetrics,
                ).toInt()
        } else {
            columnWidth
        }
    }

    fun setColumnWidth(newColumnWidth: Int) {
        if (newColumnWidth > 0 && newColumnWidth != columnWidth) {
            columnWidth = newColumnWidth
            isColumnWidthChanged = true
            requestLayout()
        }
    }

    override fun onLayoutChildren(
        recycler: Recycler,
        state: RecyclerView.State,
    ) {
        val width = getWidth()
        val height = getHeight()
        if (columnWidth > 0 && width > 0 && height > 0 && (isColumnWidthChanged || lastWidth != width || lastHeight != height)) {
            val totalSpace: Int =
                if (orientation == VERTICAL) {
                    width - paddingRight - paddingLeft
                } else {
                    height - paddingTop - paddingBottom
                }
            val spanCount = max(1, totalSpace / columnWidth)
            setSpanCount(spanCount)
            isColumnWidthChanged = false
        }
        lastWidth = width
        lastHeight = height
        super.onLayoutChildren(recycler, state)
    }
}
