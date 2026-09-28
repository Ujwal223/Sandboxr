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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.util.FloatProperty
import android.view.View
import android.view.ViewGroup
import com.sandboxr.launcher.celllayout.CellLayoutLayoutParams
import com.sandboxr.launcher.util.GridOccupancy

/**
 * Represents a single screen/page of items (shortcuts, widgets, folders) organized in a grid.
 */
open class CellLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    var countX: Int = 4
        protected set
    var countY: Int = 5
        protected set
    var cellWidth: Int = 0
        protected set
    var cellHeight: Int = 0
        protected set
    var borderSpace: Point = Point(0, 0)
        protected set

    private var mOccupied: GridOccupancy = GridOccupancy(countX, countY)
    val shortcutsAndWidgets: ShortcutAndWidgetContainer = ShortcutAndWidgetContainer(context)
    private var mContainer: CellLayoutContainer? = null
    var springLoadedProgress: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    private var mSpaceBetweenCellLayoutsPx: Int = 0

    init {
        addView(shortcutsAndWidgets)
    }

    fun setGridSize(columns: Int, rows: Int) {
        countX = columns
        countY = rows
        mOccupied = GridOccupancy(columns, rows)
        shortcutsAndWidgets.countX = columns
        shortcutsAndWidgets.countY = rows
        requestLayout()
    }

    fun setCellLayoutContainer(container: CellLayoutContainer) {
        mContainer = container
    }

    fun getCellLayoutContainer(): CellLayoutContainer? = mContainer

    fun setSpaceBetweenCellLayoutsPx(space: Int) {
        mSpaceBetweenCellLayoutsPx = space
    }

    fun getOccupied(): GridOccupancy = mOccupied

    fun addViewToCellLayout(
        child: View,
        index: Int,
        childId: Int,
        params: CellLayoutLayoutParams,
        markCellsAsOccupied: Boolean
    ): Boolean {
        if (childId != View.NO_ID) {
            child.id = childId
        }
        child.layoutParams = params

        if (markCellsAsOccupied && params.isLockedToGrid) {
            val cellX = params.cellX
            val cellY = params.cellY
            val spanX = params.cellHSpan
            val spanY = params.cellVSpan
            if (cellX >= 0 && cellY >= 0 && cellX + spanX <= countX && cellY + spanY <= countY) {
                mOccupied.markCells(cellX, cellY, spanX, spanY, true)
            }
        }

        shortcutsAndWidgets.addView(child, index, params)
        return true
    }

    override fun removeView(view: View) {
        if (view === shortcutsAndWidgets) {
            super.removeView(view)
        } else {
            shortcutsAndWidgets.removeView(view)
        }
    }

    override fun removeViewAt(index: Int) {
        if (getChildAt(index) === shortcutsAndWidgets) {
            super.removeViewAt(index)
        } else {
            shortcutsAndWidgets.removeViewAt(index)
        }
    }

    override fun removeAllViews() {
        shortcutsAndWidgets.removeAllViews()
    }

    override fun removeAllViewsInLayout() {
        shortcutsAndWidgets.removeAllViewsInLayout()
    }

    fun setFolderLeaveBehindCell(cellX: Int, cellY: Int) {
        // Leave behind visual indicator when folder is open
    }

    fun clearFolderLeaveBehind() {
        // Clear folder indicator
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val availWidth = width - paddingLeft - paddingRight - (countX - 1) * borderSpace.x
        val availHeight = height - paddingTop - paddingBottom - (countY - 1) * borderSpace.y

        cellWidth = if (countX > 0) (availWidth / countX).coerceAtLeast(0) else 0
        cellHeight = if (countY > 0) (availHeight / countY).coerceAtLeast(0) else 0

        shortcutsAndWidgets.setCellDimensions(cellWidth, cellHeight, countX, countY, borderSpace)
        val childWidthSpec = MeasureSpec.makeMeasureSpec(width - paddingLeft - paddingRight, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(height - paddingTop - paddingBottom, MeasureSpec.EXACTLY)
        shortcutsAndWidgets.measure(childWidthSpec, childHeightSpec)

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        shortcutsAndWidgets.layout(
            paddingLeft,
            paddingTop,
            r - l - paddingRight,
            b - t - paddingBottom
        )
    }

    companion object {
        const val CONTAINER_TYPE_WORKSPACE = 0
        const val CONTAINER_TYPE_HOTSEAT = 1
        const val CONTAINER_TYPE_FOLDER = 2

        @JvmField
        val SPRING_LOADED_PROGRESS: FloatProperty<CellLayout> =
            object : FloatProperty<CellLayout>("springLoadedProgress") {
                override fun setValue(target: CellLayout, value: Float) {
                    target.springLoadedProgress = value
                }

                override fun get(target: CellLayout): Float {
                    return target.springLoadedProgress
                }
            }
    }
}
