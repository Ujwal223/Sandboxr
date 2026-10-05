/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.sandboxr.launcher

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.sandboxr.launcher.views.RecyclerViewFastScroller

/**
 * Base RecyclerView with support for fast scrolling thumb and letter overlay synchronization.
 */
abstract class FastScrollRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    @JvmField
    protected var mScrollbar: RecyclerViewFastScroller? = null

    val scrollbar: RecyclerViewFastScroller?
        get() = mScrollbar

    open fun bindFastScrollbar(scrollbar: RecyclerViewFastScroller) {
        mScrollbar = scrollbar
        scrollbar.setRecyclerView(this)
        scrollToTop()
        onUpdateScrollbar(0)
    }

    open fun getScrollBarTop(): Int = paddingTop

    open fun getScrollBarMarginBottom(): Int = paddingBottom

    fun getScrollbarTrackHeight(): Int {
        val scroller = mScrollbar ?: return 0
        return scroller.height - getScrollBarTop() - getScrollBarMarginBottom()
    }

    protected open fun getAvailableScrollHeight(): Int {
        val firstPageHeight = measuredHeight - paddingTop - paddingBottom
        val availableScrollHeight = computeVerticalScrollRange() - firstPageHeight
        return maxOf(0, availableScrollHeight)
    }

    protected open fun getAvailableScrollBarHeight(): Int {
        val scroller = mScrollbar ?: return 0
        return getScrollbarTrackHeight() - scroller.getThumbHeight()
    }

    protected open fun synchronizeScrollBarThumbOffsetToViewScroll(
        scrollY: Int,
        availableScrollHeight: Int
    ) {
        val scroller = mScrollbar ?: return
        if (availableScrollHeight <= 0) {
            scroller.setThumbOffsetY(-1)
            return
        }

        val scrollBarY = ((scrollY.toFloat() / availableScrollHeight) * getAvailableScrollBarHeight()).toInt()
        scroller.setThumbOffsetY(scrollBarY)
    }

    open fun scrollToTop() {
        if (layoutManager != null) {
            stopScroll()
            layoutManager?.scrollToPosition(0)
        }
    }

    /**
     * Map a touch fraction [0.0, 1.0] from fast scroller dragging to a scroll position,
     * returning the section name / letter to display in the overlay popup.
     */
    open fun scrollToPositionAtProgress(touchFraction: Float): CharSequence = ""

    open fun onFastScrollCompleted() {}

    open fun onUpdateScrollbar(dy: Int) {}

    open fun shouldContainerScroll(ev: MotionEvent, eventSource: View): Boolean {
        val scroller = mScrollbar ?: return true
        return !scroller.isDraggingThumb()
    }
}
