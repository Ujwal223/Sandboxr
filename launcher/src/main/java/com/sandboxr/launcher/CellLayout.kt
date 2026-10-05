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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.util.ArrayMap
import android.util.AttributeSet
import android.util.FloatProperty
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import com.sandboxr.launcher.celllayout.CellLayoutLayoutParams
import com.sandboxr.launcher.celllayout.DelegatedCellDrawing
import com.sandboxr.launcher.celllayout.ItemConfiguration
import com.sandboxr.launcher.celllayout.ReorderAlgorithm
import com.sandboxr.launcher.celllayout.ReorderParameters
import com.sandboxr.launcher.celllayout.ReorderPreviewAnimation
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.util.CellAndSpan
import com.sandboxr.launcher.util.GridOccupancy
import com.sandboxr.launcher.util.MultiTranslateDelegate
import java.util.Stack

/**
 * Represents a single screen/page of items (shortcuts, widgets, folders) organized in a grid.
 * Handles cell measurement, item positioning, drag previews, and reordering algorithms.
 */
open class CellLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    val containerType: Int = CONTAINER_TYPE_WORKSPACE
) : ViewGroup(context, attrs, defStyleAttr) {

    constructor(context: Context, container: CellLayoutContainer) : this(context, null, 0, CONTAINER_TYPE_WORKSPACE) {
        this.mContainer = container
    }

    var countX: Int = 4
        set(value) {
            field = value
            mOccupied = GridOccupancy(field, countY)
            mTmpOccupied = GridOccupancy(field, countY)
            shortcutsAndWidgets.countX = field
        }
    var countY: Int = 5
        set(value) {
            field = value
            mOccupied = GridOccupancy(countX, field)
            mTmpOccupied = GridOccupancy(countX, field)
            shortcutsAndWidgets.countY = field
        }

    var cellWidth: Int = 0
        protected set
    var cellHeight: Int = 0
        protected set
    private var mFixedCellWidth: Int = -1
    private var mFixedCellHeight: Int = -1

    var borderSpace: Point = Point(0, 0)
        set(value) {
            field = value
            shortcutsAndWidgets.setCellDimensions(cellWidth, cellHeight, countX, countY, field)
            requestLayout()
        }

    protected var mOccupied: GridOccupancy = GridOccupancy(countX, countY)
    @JvmField var mTmpOccupied: GridOccupancy = GridOccupancy(countX, countY)

    val shortcutsAndWidgets: ShortcutAndWidgetContainer = ShortcutAndWidgetContainer(context, containerType)
    private var mContainer: CellLayoutContainer? = null

    var springLoadedProgress: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var scrollProgress: Float = 0f
        set(value) {
            field = Math.abs(value)
            invalidate()
        }

    var isDragOverlapping: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var spaceBetweenCellLayoutsPx: Int = 0
    var isItemPlacementDirty: Boolean = false

    @JvmField val mDirectionVector: IntArray = IntArray(2)
    var previousSolution: ItemConfiguration? = null

    private val mTempRect: Rect = Rect()
    private val mTmpPoint: IntArray = IntArray(2)

    private val mDelegatedCellDrawings: ArrayList<DelegatedCellDrawing> = ArrayList()
    private val mFolderLeaveBehindCell: Point = Point(-1, -1)

    // Drag outline visualization
    private val mDragOutlines: Array<CellLayoutLayoutParams> = Array(4) { CellLayoutLayoutParams(0, 0, 0, 0) }
    private val mDragOutlineAlphas: FloatArray = FloatArray(4) { 0f }
    private val mDragOutlineAnims: Array<InterruptibleInOutAnimator> = Array(4) { index ->
        InterruptibleInOutAnimator(200L, 0f, 120f).apply {
            animator.addUpdateListener { anim ->
                mDragOutlineAlphas[index] = anim.animatedValue as Float
                invalidate()
            }
        }
    }
    private var mDragOutlineCurrent: Int = 0
    private val mDragCell: IntArray = intArrayOf(-1, -1)
    private val mDragCellSpan: IntArray = intArrayOf(-1, -1)

    private val mDragOutlinePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.parseColor("#4080FF")
    }

    private val mVisualizeGridPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var visualizeCells: Boolean = false
    var visualizeDropLocation: Boolean = true

    @JvmField val mReorderAnimators: ArrayMap<CellLayoutLayoutParams, Animator> = ArrayMap()
    @JvmField val mShakeAnimators: ArrayMap<Reorderable, ReorderPreviewAnimation<*>> = ArrayMap()
    var reorderPreviewAnimationMagnitude: Float = 24f

    private var mDragging: Boolean = false

    init {
        setWillNotDraw(false)
        clipToPadding = false
        clipChildren = false
        addView(shortcutsAndWidgets)
    }

    open fun setGridSize(columns: Int, rows: Int) {
        countX = columns
        countY = rows
        shortcutsAndWidgets.setCellDimensions(cellWidth, cellHeight, columns, rows, borderSpace)
        requestLayout()
    }

    open fun setCellDimensions(width: Int, height: Int) {
        mFixedCellWidth = width
        mFixedCellHeight = height
        cellWidth = width
        cellHeight = height
        shortcutsAndWidgets.setCellDimensions(cellWidth, cellHeight, countX, countY, borderSpace)
        requestLayout()
    }

    open fun setCellLayoutContainer(container: CellLayoutContainer) {
        mContainer = container
    }

    open fun getCellLayoutContainer(): CellLayoutContainer? = mContainer

    open fun getOccupied(): GridOccupancy = mOccupied

    open fun setOccupied(occupied: GridOccupancy) {
        mOccupied = occupied
    }

    open fun acceptsWidget(): Boolean = containerType == CONTAINER_TYPE_WORKSPACE

    open fun addViewToCellLayout(
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

        if (params.cellHSpan < 0) params.cellHSpan = countX
        if (params.cellVSpan < 0) params.cellVSpan = countY

        if (markCellsAsOccupied && params.isLockedToGrid) {
            val cellX = params.cellX
            val cellY = params.cellY
            val spanX = params.cellHSpan
            val spanY = params.cellVSpan
            if (cellX in 0 until countX && cellY in 0 until countY) {
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
            markCellsAsUnoccupiedForView(view)
            shortcutsAndWidgets.removeView(view)
        }
    }

    override fun removeViewAt(index: Int) {
        if (getChildAt(index) === shortcutsAndWidgets) {
            super.removeViewAt(index)
        } else {
            val child = shortcutsAndWidgets.getChildAt(index)
            if (child != null) markCellsAsUnoccupiedForView(child)
            shortcutsAndWidgets.removeViewAt(index)
        }
    }

    override fun removeAllViews() {
        mOccupied.clear()
        mTmpOccupied.clear()
        shortcutsAndWidgets.removeAllViews()
    }

    override fun removeAllViewsInLayout() {
        mOccupied.clear()
        mTmpOccupied.clear()
        shortcutsAndWidgets.removeAllViewsInLayout()
    }

    open fun markCellsAsOccupiedForView(view: View?) {
        if (view == null || view.parent !== shortcutsAndWidgets) return
        val lp = view.layoutParams as? CellLayoutLayoutParams ?: return
        mOccupied.markCells(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, true)
    }

    open fun markCellsAsUnoccupiedForView(view: View?) {
        if (view == null || view.parent !== shortcutsAndWidgets) return
        val lp = view.layoutParams as? CellLayoutLayoutParams ?: return
        mOccupied.markCells(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, false)
    }

    open fun isOccupied(x: Int, y: Int): Boolean {
        return if (x in 0 until countX && y in 0 until countY) {
            mOccupied.cells[x][y]
        } else {
            true
        }
    }

    open fun isRegionVacant(x: Int, y: Int, spanX: Int, spanY: Int): Boolean {
        return mOccupied.isRegionVacant(x, y, spanX, spanY)
    }

    open fun existsEmptyCell(): Boolean {
        return findCellForSpan(null, 1, 1)
    }

    open fun findCellForSpan(cellXY: IntArray?, spanX: Int, spanY: Int): Boolean {
        val target = cellXY ?: IntArray(2)
        return mOccupied.findVacantCell(target, spanX, spanY)
    }

    open fun getChildAt(cellX: Int, cellY: Int): View? {
        return shortcutsAndWidgets.getChildAt(cellX, cellY)
    }

    // Coordinate & Pixel Conversions
    open fun pointToCellExact(x: Int, y: Int, result: IntArray) {
        val hStartPadding = paddingLeft + (getUnusedHorizontalSpace() / 2)
        val vStartPadding = paddingTop
        val cellW = (cellWidth + borderSpace.x).coerceAtLeast(1)
        val cellH = (cellHeight + borderSpace.y).coerceAtLeast(1)

        result[0] = ((x - hStartPadding) / cellW).coerceIn(0, (countX - 1).coerceAtLeast(0))
        result[1] = ((y - vStartPadding) / cellH).coerceIn(0, (countY - 1).coerceAtLeast(0))
    }

    open fun cellToPoint(cellX: Int, cellY: Int, result: IntArray) {
        cellToRect(cellX, cellY, 1, 1, mTempRect)
        result[0] = mTempRect.left
        result[1] = mTempRect.top
    }

    open fun cellToCenterPoint(cellX: Int, cellY: Int, result: IntArray) {
        regionToCenterPoint(cellX, cellY, 1, 1, result)
    }

    open fun regionToCenterPoint(cellX: Int, cellY: Int, spanX: Int, spanY: Int, result: IntArray) {
        cellToRect(cellX, cellY, spanX, spanY, mTempRect)
        result[0] = mTempRect.centerX()
        result[1] = mTempRect.centerY()
    }

    open fun cellToRect(cellX: Int, cellY: Int, cellHSpan: Int, cellVSpan: Int, resultRect: Rect) {
        val hStartPadding = paddingLeft + (getUnusedHorizontalSpace() / 2)
        val vStartPadding = paddingTop

        val x = hStartPadding + (cellX * borderSpace.x) + (cellX * cellWidth) + getTranslationXForCell(cellX, cellY)
        val y = vStartPadding + (cellY * borderSpace.y) + (cellY * cellHeight)

        val width = cellHSpan * cellWidth + ((cellHSpan - 1) * borderSpace.x)
        val height = cellVSpan * cellHeight + ((cellVSpan - 1) * borderSpace.y)

        resultRect.set(x, y, x + width, y + height)
    }

    protected open fun getTranslationXForCell(cellX: Int, cellY: Int): Int = 0

    open fun getUnusedHorizontalSpace(): Int {
        val avail = measuredWidth - paddingLeft - paddingRight
        val totalCells = countX * cellWidth + ((countX - 1) * borderSpace.x)
        return (avail - totalCells).coerceAtLeast(0)
    }

    open fun getDesiredWidth(): Int {
        return paddingLeft + paddingRight + (countX * cellWidth) + ((countX - 1) * borderSpace.x)
    }

    open fun getDesiredHeight(): Int {
        return paddingTop + paddingBottom + (countY * cellHeight) + ((countY - 1) * borderSpace.y)
    }

    // Vacant Area & Nearest Area Calculation
    open fun findNearestVacantArea(
        pixelX: Int,
        pixelY: Int,
        minSpanX: Int,
        minSpanY: Int,
        spanX: Int,
        spanY: Int,
        result: IntArray?,
        resultSpan: IntArray?
    ): IntArray {
        return findNearestArea(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, false, result, resultSpan)
    }

    open fun findNearestAreaIgnoreOccupied(
        pixelX: Int,
        pixelY: Int,
        spanX: Int,
        spanY: Int,
        result: IntArray?
    ): IntArray {
        return findNearestArea(pixelX, pixelY, spanX, spanY, spanX, spanY, true, result, null)
    }

    protected open fun findNearestArea(
        relativeXPos: Int,
        relativeYPos: Int,
        minSpanX: Int,
        minSpanY: Int,
        spanX: Int,
        spanY: Int,
        ignoreOccupied: Boolean,
        result: IntArray?,
        resultSpan: IntArray?
    ): IntArray {
        val cellW = (cellWidth + borderSpace.x).coerceAtLeast(1)
        val cellH = (cellHeight + borderSpace.y).coerceAtLeast(1)

        val adjustedX = (relativeXPos - cellW * (spanX - 1) / 2f).toInt()
        val adjustedY = (relativeYPos - cellH * (spanY - 1) / 2f).toInt()

        val bestXY = result ?: IntArray(2)
        var bestDistance = Double.MAX_VALUE
        val bestRect = Rect(-1, -1, -1, -1)
        val validRegions = Stack<Rect>()

        if (minSpanX <= 0 || minSpanY <= 0 || spanX <= 0 || spanY <= 0 || spanX < minSpanX || spanY < minSpanY) {
            return bestXY
        }

        for (y in 0..(countY - minSpanY)) {
            outer@ for (x in 0..(countX - minSpanX)) {
                var xSize = minSpanX
                var ySize = minSpanY

                if (!ignoreOccupied) {
                    for (i in 0 until minSpanX) {
                        for (j in 0 until minSpanY) {
                            if (mOccupied.cells[x + i][y + j]) {
                                continue@outer
                            }
                        }
                    }

                    var incX = true
                    var hitMaxX = xSize >= spanX
                    var hitMaxY = ySize >= spanY
                    while (!(hitMaxX && hitMaxY)) {
                        if (incX && !hitMaxX) {
                            for (j in 0 until ySize) {
                                if (x + xSize >= countX || mOccupied.cells[x + xSize][y + j]) {
                                    hitMaxX = true
                                    break
                                }
                            }
                            if (!hitMaxX) xSize++
                        } else if (!hitMaxY) {
                            for (i in 0 until xSize) {
                                if (y + ySize >= countY || mOccupied.cells[x + i][y + ySize]) {
                                    hitMaxY = true
                                    break
                                }
                            }
                            if (!hitMaxY) ySize++
                        }
                        hitMaxX = hitMaxX || (xSize >= spanX)
                        hitMaxY = hitMaxY || (ySize >= spanY)
                        incX = !incX
                    }
                }

                val cellXY = mTmpPoint
                cellToCenterPoint(x, y, cellXY)
                val currentRect = Rect(x, y, x + xSize, y + ySize)

                var contained = false
                for (r in validRegions) {
                    if (r.contains(currentRect)) {
                        contained = true
                        break
                    }
                }
                validRegions.push(currentRect)

                val distance = Math.hypot((cellXY[0] - adjustedX).toDouble(), (cellXY[1] - adjustedY).toDouble())
                if ((distance <= bestDistance && !contained) || currentRect.contains(bestRect)) {
                    bestDistance = distance
                    bestXY[0] = x
                    bestXY[1] = y
                    resultSpan?.let {
                        it[0] = xSize
                        it[1] = ySize
                    }
                    bestRect.set(currentRect)
                }
            }
        }

        if (bestDistance == Double.MAX_VALUE) {
            bestXY[0] = -1
            bestXY[1] = -1
        }
        return bestXY
    }

    open fun getIntersectingRectanglesInRegion(region: Rect, dragView: View?): Rect? {
        val boundingRect = Rect(region)
        val r1 = Rect()
        var isOverlapping = false
        val count = shortcutsAndWidgets.childCount
        for (i in 0 until count) {
            val child = shortcutsAndWidgets.getChildAt(i)
            if (child === dragView) continue
            val lp = child.layoutParams as? CellLayoutLayoutParams ?: continue
            r1.set(lp.cellX, lp.cellY, lp.cellX + lp.cellHSpan, lp.cellY + lp.cellVSpan)
            if (Rect.intersects(region, r1)) {
                isOverlapping = true
                boundingRect.union(r1)
            }
        }
        return if (isOverlapping) boundingRect else null
    }

    open fun isNearestDropLocationOccupied(
        pixelX: Int,
        pixelY: Int,
        spanX: Int,
        spanY: Int,
        dragView: View?,
        result: IntArray?
    ): Boolean {
        val target = findNearestAreaIgnoreOccupied(pixelX, pixelY, spanX, spanY, result)
        val region = Rect(target[0], target[1], target[0] + spanX, target[1] + spanY)
        return getIntersectingRectanglesInRegion(region, dragView) != null
    }

    // Reorder Engine Integration
    open fun createReorderAlgorithm(): ReorderAlgorithm = ReorderAlgorithm(this)

    open fun copyCurrentStateToSolution(solution: ItemConfiguration) {
        val childCount = shortcutsAndWidgets.childCount
        for (i in 0 until childCount) {
            val child = shortcutsAndWidgets.getChildAt(i)
            val lp = child.layoutParams as? CellLayoutLayoutParams ?: continue
            solution.add(child, CellAndSpan(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan))
        }
    }

    open fun calculateReorder(
        pixelX: Int,
        pixelY: Int,
        minSpanX: Int,
        minSpanY: Int,
        spanX: Int,
        spanY: Int,
        dragView: View?
    ): ItemConfiguration? {
        val configuration = ItemConfiguration()
        copyCurrentStateToSolution(configuration)
        val params = ReorderParameters(pixelX, pixelY, spanX, spanY, minSpanX, minSpanY, dragView, configuration)
        return createReorderAlgorithm().calculateReorder(params)
    }

    open fun performReorder(
        pixelX: Int,
        pixelY: Int,
        minSpanX: Int,
        minSpanY: Int,
        spanX: Int,
        spanY: Int,
        dragView: View?,
        result: IntArray?,
        resultSpan: IntArray?,
        mode: Int
    ): IntArray {
        val res = result ?: intArrayOf(-1, -1)
        val resSpan = resultSpan ?: intArrayOf(-1, -1)

        val finalSolution: ItemConfiguration? = if (mode == MODE_SHOW_REORDER_HINT || previousSolution == null) {
            calculateReorder(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, dragView).also {
                previousSolution = it
            }
        } else {
            previousSolution.also {
                if (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
                    previousSolution = null
                }
            }
        }

        if (finalSolution == null || !finalSolution.isSolution) {
            res[0] = -1
            res[1] = -1
            resSpan[0] = -1
            resSpan[1] = -1
        } else {
            res[0] = finalSolution.cellX
            res[1] = finalSolution.cellY
            resSpan[0] = finalSolution.spanX
            resSpan[1] = finalSolution.spanY
            performReorder(finalSolution, dragView, mode)
        }
        return res
    }

    open fun performReorder(solution: ItemConfiguration, dragView: View?, mode: Int) {
        if (mode == MODE_SHOW_REORDER_HINT) {
            beginOrAdjustReorderPreviewAnimations(solution, dragView, ReorderPreviewAnimation.MODE_HINT)
            return
        }

        if (mode == MODE_DRAG_OVER || mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
            setUseTempCoords(true)
            copySolutionToTempState(solution, dragView)
            isItemPlacementDirty = true
            animateItemsToSolution(solution, dragView, mode == MODE_ON_DROP)

            if (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
                commitTempPlacement(dragView)
                completeAndClearReorderPreviewAnimations()
                isItemPlacementDirty = false
            } else {
                beginOrAdjustReorderPreviewAnimations(solution, dragView, ReorderPreviewAnimation.MODE_PREVIEW)
            }
        }

        if (mode == MODE_ON_DROP) {
            setUseTempCoords(false)
        }

        shortcutsAndWidgets.requestLayout()
    }

    open fun animateChildToPosition(
        child: View,
        cellX: Int,
        cellY: Int,
        duration: Int,
        delay: Int,
        permanent: Boolean,
        adjustOccupied: Boolean
    ): Boolean {
        if (shortcutsAndWidgets.indexOfChild(child) == -1) return false
        val item = child as? Reorderable ?: return false
        val lp = child.layoutParams as? CellLayoutLayoutParams ?: return false

        mReorderAnimators.remove(lp)?.cancel()

        if (adjustOccupied) {
            val occupied = if (permanent) mOccupied else mTmpOccupied
            occupied.markCells(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, false)
            occupied.markCells(cellX, cellY, lp.cellHSpan, lp.cellVSpan, true)
        }

        val oldX = lp.x
        val oldY = lp.y
        lp.isLockedToGrid = true
        if (permanent) {
            lp.cellX = cellX
            lp.cellY = cellY
        } else {
            lp.tmpCellX = cellX
            lp.tmpCellY = cellY
        }
        shortcutsAndWidgets.setupLp(child)
        val newX = lp.x
        val newY = lp.y
        lp.x = oldX
        lp.y = oldY
        lp.isLockedToGrid = false

        val mtd = item.getTranslateDelegate()
        val initPreviewOffsetX = mtd.getTranslationX(MultiTranslateDelegate.INDEX_REORDER_PREVIEW_OFFSET).value
        val initPreviewOffsetY = mtd.getTranslationY(MultiTranslateDelegate.INDEX_REORDER_PREVIEW_OFFSET).value
        val finalPreviewOffsetX = (newX - oldX).toFloat()
        val finalPreviewOffsetY = (newY - oldY).toFloat()

        if (finalPreviewOffsetX == 0f && finalPreviewOffsetY == 0f && initPreviewOffsetX == 0f && initPreviewOffsetY == 0f) {
            lp.isLockedToGrid = true
            return true
        }

        val va = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration.toLong()
            startDelay = delay.toLong()
            addUpdateListener { animation ->
                val r = animation.animatedValue as Float
                val x = (1 - r) * initPreviewOffsetX + r * finalPreviewOffsetX
                val y = (1 - r) * initPreviewOffsetY + r * finalPreviewOffsetY
                item.getTranslateDelegate().setTranslation(MultiTranslateDelegate.INDEX_REORDER_PREVIEW_OFFSET, x, y)
            }
            addListener(object : AnimatorListenerAdapter() {
                var cancelled = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }
                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled) {
                        lp.isLockedToGrid = true
                        item.getTranslateDelegate().setTranslation(MultiTranslateDelegate.INDEX_REORDER_PREVIEW_OFFSET, 0f, 0f)
                        child.requestLayout()
                    }
                    mReorderAnimators.remove(lp)
                }
            })
        }
        mReorderAnimators[lp] = va
        va.start()
        return true
    }

    private fun animateItemsToSolution(solution: ItemConfiguration, dragView: View?, commitDragView: Boolean) {
        mTmpOccupied.clear()
        val childCount = shortcutsAndWidgets.childCount
        for (i in 0 until childCount) {
            val child = shortcutsAndWidgets.getChildAt(i)
            if (child === dragView) continue
            val c = solution.map[child]
            if (c != null) {
                animateChildToPosition(child, c.cellX, c.cellY, REORDER_ANIMATION_DURATION, 0, false, false)
                mTmpOccupied.markCells(c, true)
            }
        }
        if (commitDragView) {
            mTmpOccupied.markCells(solution, true)
        }
    }

    private fun beginOrAdjustReorderPreviewAnimations(solution: ItemConfiguration, dragView: View?, mode: Int) {
        val childCount = shortcutsAndWidgets.childCount
        for (i in 0 until childCount) {
            val child = shortcutsAndWidgets.getChildAt(i)
            if (child === dragView) continue
            val c = solution.map[child] ?: continue
            val skip = mode == ReorderPreviewAnimation.MODE_HINT && !solution.intersectingViews.contains(child)
            val lp = child.layoutParams as? CellLayoutLayoutParams ?: continue
            if (!skip && child is Reorderable) {
                val rha = ReorderPreviewAnimation(
                    child,
                    mode,
                    lp.cellX,
                    lp.cellY,
                    c.cellX,
                    c.cellY,
                    c.spanX,
                    c.spanY,
                    reorderPreviewAnimationMagnitude,
                    this,
                    mShakeAnimators
                )
                rha.animate()
            }
        }
    }

    open fun completeAndClearReorderPreviewAnimations() {
        for (anim in mShakeAnimators.values) {
            anim.finishAnimation()
        }
        mShakeAnimators.clear()
    }

    open fun revertTempState() {
        completeAndClearReorderPreviewAnimations()
        if (isItemPlacementDirty) {
            val count = shortcutsAndWidgets.childCount
            for (i in 0 until count) {
                val child = shortcutsAndWidgets.getChildAt(i)
                val lp = child.layoutParams as? CellLayoutLayoutParams ?: continue
                if (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.cellY) {
                    lp.tmpCellX = lp.cellX
                    lp.tmpCellY = lp.cellY
                    animateChildToPosition(child, lp.cellX, lp.cellY, REORDER_ANIMATION_DURATION, 0, false, false)
                }
            }
            isItemPlacementDirty = false
        }
    }

    private fun commitTempPlacement(dragView: View?) {
        mTmpOccupied.copyTo(mOccupied)
        val childCount = shortcutsAndWidgets.childCount
        for (i in 0 until childCount) {
            val child = shortcutsAndWidgets.getChildAt(i)
            val lp = child.layoutParams as? CellLayoutLayoutParams ?: continue
            if (child !== dragView) {
                lp.cellX = lp.tmpCellX
                lp.cellY = lp.tmpCellY
            }
        }
    }

    open fun copySolutionToTempState(solution: ItemConfiguration, dragView: View?) {
        mTmpOccupied.clear()
        val childCount = shortcutsAndWidgets.childCount
        for (i in 0 until childCount) {
            val child = shortcutsAndWidgets.getChildAt(i)
            if (child === dragView) continue
            val lp = child.layoutParams as? CellLayoutLayoutParams ?: continue
            val c = solution.map[child]
            if (c != null) {
                lp.tmpCellX = c.cellX
                lp.tmpCellY = c.cellY
                lp.cellHSpan = c.spanX
                lp.cellVSpan = c.spanY
                mTmpOccupied.markCells(c, true)
            }
        }
        mTmpOccupied.markCells(solution, true)
    }

    private fun setUseTempCoords(useTempCoords: Boolean) {
        val childCount = shortcutsAndWidgets.childCount
        for (i in 0 until childCount) {
            val lp = shortcutsAndWidgets.getChildAt(i).layoutParams as? CellLayoutLayoutParams ?: continue
            lp.useTmpCoords = useTempCoords
        }
    }

    // Drag Over & Visualizer Methods
    open fun onDragEnter() {
        mDragging = true
        previousSolution = null
    }

    open fun onDragExit() {
        if (mDragging) {
            mDragging = false
        }
        previousSolution = null
        clearDragOutlines()
        revertTempState()
        isDragOverlapping = false
    }

    open fun onDropChild(child: View?) {
        if (child != null) {
            val lp = child.layoutParams as? CellLayoutLayoutParams
            if (lp != null) {
                lp.dropped = true
                child.requestLayout()
                markCellsAsOccupiedForView(child)
            }
        }
    }

    open fun visualizeDropLocation(cellX: Int, cellY: Int, spanX: Int, spanY: Int) {
        if (mDragCell[0] != cellX || mDragCell[1] != cellY || mDragCellSpan[0] != spanX || mDragCellSpan[1] != spanY) {
            mDragCell[0] = cellX
            mDragCell[1] = cellY
            mDragCellSpan[0] = spanX
            mDragCellSpan[1] = spanY

            val oldIndex = mDragOutlineCurrent
            mDragOutlineAnims[oldIndex].animateOut()
            mDragOutlineCurrent = (oldIndex + 1) % mDragOutlines.size

            val cell = mDragOutlines[mDragOutlineCurrent]
            cell.cellX = cellX
            cell.cellY = cellY
            cell.cellHSpan = spanX
            cell.cellVSpan = spanY

            mDragOutlineAnims[mDragOutlineCurrent].animateIn()
            invalidate()
        }
    }

    open fun clearDragOutlines() {
        val oldIndex = mDragOutlineCurrent
        mDragOutlineAnims[oldIndex].animateOut()
        mDragCell[0] = -1
        mDragCell[1] = -1
        mDragCellSpan[0] = -1
        mDragCellSpan[1] = -1
        invalidate()
    }

    open fun hasAreaForResize(cellX: Int, cellY: Int, spanX: Int, spanY: Int, dragView: View?, direction: IntArray?): Boolean {
        val pixelXY = IntArray(2)
        regionToCenterPoint(cellX, cellY, spanX, spanY, pixelXY)
        val config = ItemConfiguration()
        copyCurrentStateToSolution(config)
        val params = ReorderParameters(pixelXY[0], pixelXY[1], spanX, spanY, spanX, spanY, dragView, config)
        val solution = createReorderAlgorithm().findReorderSolution(params, direction ?: mDirectionVector, true)
        return solution.isSolution
    }

    open fun createAreaForResize(cellX: Int, cellY: Int, spanX: Int, spanY: Int, dragView: View?, direction: IntArray?, commit: Boolean): Boolean {
        val pixelXY = IntArray(2)
        regionToCenterPoint(cellX, cellY, spanX, spanY, pixelXY)
        val config = ItemConfiguration()
        copyCurrentStateToSolution(config)
        val params = ReorderParameters(pixelXY[0], pixelXY[1], spanX, spanY, spanX, spanY, dragView, config)
        val solution = createReorderAlgorithm().findReorderSolution(params, direction ?: mDirectionVector, true)

        if (solution.isSolution) {
            setUseTempCoords(true)
            copySolutionToTempState(solution, dragView)
            isItemPlacementDirty = true
            animateItemsToSolution(solution, dragView, commit)

            if (commit) {
                commitTempPlacement(null)
                completeAndClearReorderPreviewAnimations()
                isItemPlacementDirty = false
            } else {
                beginOrAdjustReorderPreviewAnimations(solution, dragView, ReorderPreviewAnimation.MODE_PREVIEW)
            }
            shortcutsAndWidgets.requestLayout()
        }
        return solution.isSolution
    }

    open fun hasReorderSolution(itemInfo: ItemInfo): Boolean {
        val cellPoint = IntArray(2)
        for (x in 0 until countX) {
            for (y in 0 until countY) {
                cellToPoint(x, y, cellPoint)
                val config = ItemConfiguration()
                copyCurrentStateToSolution(config)
                val params = ReorderParameters(cellPoint[0], cellPoint[1], itemInfo.spanX, itemInfo.spanY, itemInfo.minSpanX, itemInfo.minSpanY, null, config)
                if (createReorderAlgorithm().findReorderSolution(params, mDirectionVector, true).isSolution) {
                    return true
                }
            }
        }
        return false
    }

    open fun makeSpaceForHotseatMigration(commitConfig: Boolean): Boolean {
        val cellPoint = IntArray(2)
        val directionVector = intArrayOf(0, -1)
        cellToPoint(0, countY - 1, cellPoint)
        val config = ItemConfiguration()
        copyCurrentStateToSolution(config)
        val params = ReorderParameters(cellPoint[0], cellPoint[1], countX, 1, countX, 1, null, config)
        val solution = createReorderAlgorithm().findReorderSolution(params, directionVector, false)
        if (solution.isSolution) {
            if (commitConfig) {
                copySolutionToTempState(solution, null)
                commitTempPlacement(null)
                mOccupied.markCells(0, countY - 1, countX, 1, false)
            }
            return true
        }
        return false
    }

    // Delegated Cell Drawing
    open fun addDelegatedCellDrawing(drawing: DelegatedCellDrawing) {
        mDelegatedCellDrawings.add(drawing)
        invalidate()
    }

    open fun removeDelegatedCellDrawing(drawing: DelegatedCellDrawing) {
        mDelegatedCellDrawings.remove(drawing)
        invalidate()
    }

    open fun setFolderLeaveBehindCell(cellX: Int, cellY: Int) {
        mFolderLeaveBehindCell.set(cellX, cellY)
        invalidate()
    }

    open fun clearFolderLeaveBehind() {
        mFolderLeaveBehindCell.set(-1, -1)
        invalidate()
    }

    // Measure, Layout & Draw
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val availWidth = width - paddingLeft - paddingRight - (countX - 1) * borderSpace.x
        val availHeight = height - paddingTop - paddingBottom - (countY - 1) * borderSpace.y

        if (mFixedCellWidth > 0 && mFixedCellHeight > 0) {
            cellWidth = mFixedCellWidth
            cellHeight = mFixedCellHeight
        } else {
            cellWidth = if (countX > 0) (availWidth / countX).coerceAtLeast(0) else 0
            cellHeight = if (countY > 0) (availHeight / countY).coerceAtLeast(0) else 0
        }

        shortcutsAndWidgets.setCellDimensions(cellWidth, cellHeight, countX, countY, borderSpace)
        val childWidthSpec = MeasureSpec.makeMeasureSpec(width - paddingLeft - paddingRight, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(height - paddingTop - paddingBottom, MeasureSpec.EXACTLY)
        shortcutsAndWidgets.measure(childWidthSpec, childHeightSpec)

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val hOffset = (getUnusedHorizontalSpace() / 2)
        shortcutsAndWidgets.layout(
            paddingLeft + hOffset,
            paddingTop,
            r - l - paddingRight - hOffset,
            b - t - paddingBottom
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw delegated drawings under items
        for (i in 0 until mDelegatedCellDrawings.size) {
            val bg = mDelegatedCellDrawings[i]
            canvas.save()
            if (bg.mDelegateCellX >= 0 && bg.mDelegateCellY >= 0) {
                cellToPoint(bg.mDelegateCellX, bg.mDelegateCellY, mTmpPoint)
                canvas.translate(mTmpPoint[0].toFloat(), mTmpPoint[1].toFloat())
            }
            bg.drawUnderItem(canvas)
            canvas.restore()
        }

        // Draw drop location outlines
        if (visualizeDropLocation) {
            val outlineRect = Rect()
            for (i in mDragOutlines.indices) {
                val alpha = mDragOutlineAlphas[i]
                if (alpha <= 0f) continue
                val params = mDragOutlines[i]
                cellToRect(params.cellX, params.cellY, params.cellHSpan, params.cellVSpan, outlineRect)
                mDragOutlinePaint.alpha = alpha.toInt().coerceIn(0, 255)
                canvas.drawRoundRect(RectF(outlineRect), 16f, 16f, mDragOutlinePaint)
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        // Draw delegated drawings over items
        for (i in 0 until mDelegatedCellDrawings.size) {
            val bg = mDelegatedCellDrawings[i]
            canvas.save()
            if (bg.mDelegateCellX >= 0 && bg.mDelegateCellY >= 0) {
                cellToPoint(bg.mDelegateCellX, bg.mDelegateCellY, mTmpPoint)
                canvas.translate(mTmpPoint[0].toFloat(), mTmpPoint[1].toFloat())
            }
            bg.drawOverItem(canvas)
            canvas.restore()
        }
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

    protected var mDragAndDropAccessibilityDelegate: com.sandboxr.launcher.accessibility.DragAndDropAccessibilityDelegate? = null
    open var isDropPending: Boolean = false

    open fun setDragAndDropAccessibilityDelegate(delegate: com.sandboxr.launcher.accessibility.DragAndDropAccessibilityDelegate?) {
        mDragAndDropAccessibilityDelegate = delegate
        androidx.core.view.ViewCompat.setAccessibilityDelegate(this, delegate)
    }

    open fun getItemMoveDescription(cellX: Int, cellY: Int): String {
        return if (containerType == CONTAINER_TYPE_HOTSEAT) {
            context.getString(R.string.move_to_hotseat_position, (maxOf(cellX, cellY) + 1).toString())
        } else {
            val row = cellY + 1
            val col = cellX + 1
            val pageDesc = getContainerPageDescription()
            context.getString(R.string.move_to_empty_cell_description, row.toString(), col.toString(), pageDesc)
        }
    }

    open fun getContainerPageDescription(): String {
        val container = mContainer
        return if (container != null) {
            val pageIndex = container.getCellLayoutIndex(this)
            container.getPageDescription(pageIndex) ?: ""
        } else {
            ""
        }
    }

    open fun getDragAndDropAccessibilityDelegate(): com.sandboxr.launcher.accessibility.DragAndDropAccessibilityDelegate {
        if (mDragAndDropAccessibilityDelegate == null) {
            mDragAndDropAccessibilityDelegate = object : com.sandboxr.launcher.accessibility.DragAndDropAccessibilityDelegate(this) {
                override fun intersectsValidDropTarget(id: Int): Int = id
                override fun getLocationDescriptionForIconDrop(id: Int): String = "Cell $id"
            }
        }
        return mDragAndDropAccessibilityDelegate!!
    }

    companion object {
        const val CONTAINER_TYPE_WORKSPACE = 0
        const val CONTAINER_TYPE_HOTSEAT = 1
        const val CONTAINER_TYPE_FOLDER = 2

        const val WORKSPACE = 0
        const val HOTSEAT = 1
        const val FOLDER = 2

        const val DEFAULT_SCALE: Float = 1.0f
        const val REORDER_ANIMATION_DURATION: Int = 150
        const val REORDER_PREVIEW_MAGNITUDE: Float = 0.12f

        const val MODE_SHOW_REORDER_HINT: Int = 0
        const val MODE_DRAG_OVER: Int = 1
        const val MODE_ON_DROP: Int = 2
        const val MODE_ON_DROP_EXTERNAL: Int = 3
        const val MODE_ACCEPT_DROP: Int = 4

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
