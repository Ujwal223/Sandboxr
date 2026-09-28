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
import android.util.AttributeSet
import android.view.View
import com.sandboxr.launcher.celllayout.CellLayoutLayoutParams
import com.sandboxr.launcher.celllayout.ItemConfiguration
import com.sandboxr.launcher.celllayout.MulticellReorderAlgorithm
import com.sandboxr.launcher.util.CellAndSpan

/**
 * CellLayout that simulates a split in the middle for use in foldable devices or multi-page displays.
 */
open class MultipageCellLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CellLayout(context, attrs, defStyleAttr) {

    private var mSeamWasAdded: Boolean = false

    override fun createReorderAlgorithm(): MulticellReorderAlgorithm {
        return MulticellReorderAlgorithm(this)
    }

    override fun findNearestArea(
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
        return createReorderAlgorithm().simulateSeam {
            super.findNearestArea(relativeXPos, relativeYPos, minSpanX, minSpanY, spanX, spanY, ignoreOccupied, result, resultSpan)
        }
    }

    override fun isNearestDropLocationOccupied(
        pixelX: Int,
        pixelY: Int,
        spanX: Int,
        spanY: Int,
        dragView: View?,
        result: IntArray?
    ): Boolean {
        return createReorderAlgorithm().simulateSeam {
            super.isNearestDropLocationOccupied(pixelX, pixelY, spanX, spanY, dragView, result)
        }
    }

    override fun createAreaForResize(
        cellX: Int,
        cellY: Int,
        spanX: Int,
        spanY: Int,
        dragView: View?,
        direction: IntArray?,
        commit: Boolean
    ): Boolean {
        val finalCellX = if (cellX >= countX / 2) cellX + 1 else cellX
        return createReorderAlgorithm().simulateSeam {
            super.createAreaForResize(finalCellX, cellY, spanX, spanY, dragView, direction, commit)
        }
    }

    override fun performReorder(
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
        val adjustedPixelX = if (pixelX >= width / 2) pixelX + cellWidth else pixelX
        return super.performReorder(adjustedPixelX, pixelY, minSpanX, minSpanY, spanX, spanY, dragView, result, resultSpan, mode)
    }

    override fun copyCurrentStateToSolution(solution: ItemConfiguration) {
        val childCount = shortcutsAndWidgets.childCount
        for (i in 0 until childCount) {
            val child = shortcutsAndWidgets.getChildAt(i)
            val lp = child.layoutParams as? CellLayoutLayoutParams ?: continue
            val seamOffset = if (lp.cellX >= countX / 2 && lp.canReorder) 1 else 0
            val c = CellAndSpan(lp.cellX + seamOffset, lp.cellY, lp.cellHSpan, lp.cellVSpan)
            solution.add(child, c)
        }
    }

    fun isSeamWasAdded(): Boolean = mSeamWasAdded

    fun setSeamWasAdded(seamWasAdded: Boolean) {
        mSeamWasAdded = seamWasAdded
    }
}
