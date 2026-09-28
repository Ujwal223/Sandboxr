/*
 * Copyright (C) 2026 The Android Open Source Project
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
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.sandboxr.launcher.celllayout.CellLayoutLayoutParams

/**
 * ViewGroup hosting child views (shortcuts, app widgets, and folders) inside a [CellLayout].
 */
open class ShortcutAndWidgetContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    val containerType: Int = CellLayout.CONTAINER_TYPE_WORKSPACE
) : ViewGroup(context, attrs, defStyleAttr) {

    constructor(context: Context, containerType: Int) : this(context, null, 0, containerType)

    var cellWidth: Int = 0
    var cellHeight: Int = 0
    var countX: Int = 4
    var countY: Int = 5
    var borderSpace: Point = Point(0, 0)
    var invertIfRtl: Boolean = false

    init {
        clipChildren = false
    }

    open fun setCellDimensions(cellWidth: Int, cellHeight: Int, countX: Int, countY: Int, borderSpace: Point) {
        this.cellWidth = cellWidth
        this.cellHeight = cellHeight
        this.countX = countX
        this.countY = countY
        this.borderSpace = borderSpace
        requestLayout()
    }

    fun invertLayoutHorizontally(): Boolean {
        return invertIfRtl && layoutDirection == LAYOUT_DIRECTION_RTL
    }

    open fun getChildAt(cellX: Int, cellY: Int): View? {
        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            val lp = child.layoutParams as? CellLayoutLayoutParams ?: continue
            val cx = lp.cellX
            val cy = lp.cellY
            if (cx <= cellX && cellX < cx + lp.cellHSpan && cy <= cellY && cellY < cy + lp.cellVSpan) {
                return child
            }
        }
        return null
    }

    open fun setupLp(child: View) {
        val lp = child.layoutParams as? CellLayoutLayoutParams ?: return
        lp.setup(cellWidth, cellHeight, invertLayoutHorizontally(), countX, countY, borderSpace)
    }

    open fun addViewInLayout(child: View, params: LayoutParams): Boolean {
        return super.addViewInLayout(child, -1, params, true)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)

        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                measureChild(child)
            }
        }
    }

    open fun measureChild(child: View) {
        val lp = child.layoutParams as? CellLayoutLayoutParams ?: return
        setupLp(child)
        val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width.coerceAtLeast(0), MeasureSpec.EXACTLY)
        val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(lp.height.coerceAtLeast(0), MeasureSpec.EXACTLY)
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                layoutChild(child)
            }
        }
    }

    open fun layoutChild(child: View) {
        val lp = child.layoutParams as? CellLayoutLayoutParams ?: return
        val childLeft = lp.x
        val childTop = lp.y
        child.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return CellLayoutLayoutParams(0, 0, 1, 1)
    }

    override fun checkLayoutParams(p: LayoutParams?): Boolean {
        return p is CellLayoutLayoutParams
    }

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams {
        return CellLayoutLayoutParams(p)
    }
}
