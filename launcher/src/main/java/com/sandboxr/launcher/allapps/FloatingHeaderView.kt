/*
 * Copyright (C) 2017 The Android Open Source Project
 * Copyright (C) 2026 Sandboxr Platform
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandboxr.launcher.allapps

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout

/**
 * A vertically-stacked container for [FloatingHeaderRow] views that "floats" above
 * the All Apps RecyclerView. It clips overscroll and manages row visibility.
 *
 * Rows are stacked vertically from top-to-bottom: prediction row first, then tab strip.
 */
class FloatingHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val rows: MutableList<FloatingHeaderRow> = mutableListOf()
    private var tabsHidden: Boolean = true

    private val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundBounds = Rect()

    /** Whether the header is currently floating above the content (partially visible). */
    var isFloating: Boolean = false
        private set

    private var onScrollListeners: MutableList<OnScrollChangedListener> = mutableListOf()

    fun interface OnScrollChangedListener {
        fun onFloatingHeaderScrollChanged(scrollY: Int, isHeaderVisible: Boolean)
    }

    init {
        orientation = VERTICAL
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
    }

    /**
     * Adds a row and calls [FloatingHeaderRow.setup] once all rows are registered.
     */
    fun addRow(row: FloatingHeaderRow) {
        addView(row.asView())
        rows.add(row)
    }

    /**
     * Finalizes setup and notifies all rows.
     */
    fun setup(tabsHidden: Boolean) {
        this.tabsHidden = tabsHidden
        val rowArray = rows.toTypedArray()
        for (row in rows) {
            row.setup(this, rowArray, tabsHidden)
        }
    }

    fun addOnScrollListener(listener: OnScrollChangedListener) {
        onScrollListeners.add(listener)
    }

    fun removeOnScrollListener(listener: OnScrollChangedListener) {
        onScrollListeners.remove(listener)
    }

    /**
     * Called by the [ActivityAllAppsContainerView] when the inner RecyclerView scrolls.
     */
    fun onScrolled(scrollY: Int, isHeaderVisible: Boolean) {
        isFloating = scrollY > 0 && isHeaderVisible
        for (row in rows) {
            row.onScrollChanged(scrollY, isHeaderVisible)
        }
        for (listener in onScrollListeners) {
            listener.onFloatingHeaderScrollChanged(scrollY, isHeaderVisible)
        }
        invalidate()
    }

    /**
     * Notifies all rows that the active tab changed.
     * @param tabIndex 0 = Personal, 1 = Work, 2 = Private
     */
    fun setActiveTab(tabIndex: Int) {
        for (row in rows) {
            row.setActiveTab(tabIndex)
        }
    }

    /**
     * Returns the total height in pixels currently occupied by visible rows.
     */
    fun getExpectedHeight(): Int {
        var total = 0
        for (row in rows) {
            if (row.isVisible()) {
                total += row.getExpectedHeight()
            }
        }
        return total
    }

    fun onHeightUpdated() {
        requestLayout()
        invalidate()
    }

    /**
     * Returns the first registered row of the given [type], or null.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : FloatingHeaderRow> findFirstRowOfType(type: Class<T>): T? {
        for (row in rows) {
            if (type.isInstance(row)) {
                return row as T
            }
        }
        return null
    }

    fun getRows(): List<FloatingHeaderRow> = rows

    fun isTabsHidden(): Boolean = tabsHidden

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Let rows handle their own touches
        return super.onInterceptTouchEvent(ev)
    }

    fun destroy() {
        for (row in rows) {
            row.destroy()
        }
    }
}
