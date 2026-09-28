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
import android.graphics.Rect
import android.view.View
import com.sandboxr.launcher.celllayout.CellLayoutLayoutParams
import com.sandboxr.launcher.celllayout.ItemConfiguration
import com.sandboxr.launcher.celllayout.ReorderAlgorithm
import com.sandboxr.launcher.celllayout.ReorderParameters
import com.sandboxr.launcher.celllayout.ViewCluster
import com.sandboxr.launcher.util.CellAndSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CellLayoutTest {

    private lateinit var context: Context
    private lateinit var cellLayout: CellLayout

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        cellLayout = CellLayout(context)
        cellLayout.setGridSize(4, 5)
        cellLayout.borderSpace = Point(16, 16)

        // Measure and layout with 1080x1920 dimension
        val wSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val hSpec = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        cellLayout.measure(wSpec, hSpec)
        cellLayout.layout(0, 0, 1080, 1920)
    }

    @Test
    fun testGridDimensionsAndMeasurement() {
        assertEquals(4, cellLayout.countX)
        assertEquals(5, cellLayout.countY)
        assertTrue("Cell width must be greater than zero", cellLayout.cellWidth > 0)
        assertTrue("Cell height must be greater than zero", cellLayout.cellHeight > 0)
        assertEquals(1080, cellLayout.measuredWidth)
        assertEquals(1920, cellLayout.measuredHeight)
    }

    @Test
    fun testCoordinateConversions() {
        val outPoint = IntArray(2)
        val outRect = Rect()

        // cell (0, 0)
        cellLayout.cellToPoint(0, 0, outPoint)
        assertEquals(outPoint[0], cellLayout.paddingLeft + (cellLayout.getUnusedHorizontalSpace() / 2))
        assertEquals(outPoint[1], cellLayout.paddingTop)

        // cellToRect for 1x1
        cellLayout.cellToRect(0, 0, 1, 1, outRect)
        assertEquals(cellLayout.cellWidth, outRect.width())
        assertEquals(cellLayout.cellHeight, outRect.height())

        // regionToCenterPoint
        cellLayout.regionToCenterPoint(0, 0, 1, 1, outPoint)
        assertEquals(outRect.centerX(), outPoint[0])
        assertEquals(outRect.centerY(), outPoint[1])

        // pointToCellExact maps center of cell (0, 0) back to (0, 0)
        val cellCoords = IntArray(2)
        cellLayout.pointToCellExact(outRect.centerX(), outRect.centerY(), cellCoords)
        assertEquals(0, cellCoords[0])
        assertEquals(0, cellCoords[1])
    }

    @Test
    fun testOccupancyOperations() {
        assertFalse(cellLayout.isOccupied(0, 0))
        assertTrue(cellLayout.existsEmptyCell())

        val testView = View(context)
        val params = CellLayoutLayoutParams(0, 0, 1, 1)
        val added = cellLayout.addViewToCellLayout(testView, -1, 1001, params, true)
        assertTrue(added)

        assertTrue(cellLayout.isOccupied(0, 0))
        assertFalse(cellLayout.isOccupied(1, 0))
        assertFalse(cellLayout.isRegionVacant(0, 0, 1, 1))
        assertTrue(cellLayout.isRegionVacant(1, 0, 1, 1))

        // Remove view and check unoccupied
        cellLayout.removeView(testView)
        assertFalse(cellLayout.isOccupied(0, 0))
        assertTrue(cellLayout.isRegionVacant(0, 0, 1, 1))
    }

    @Test
    fun testFindNearestVacantArea() {
        // Place an item at (0, 0)
        val view1 = View(context)
        val params1 = CellLayoutLayoutParams(0, 0, 1, 1)
        cellLayout.addViewToCellLayout(view1, -1, 1001, params1, true)

        val outRect = Rect()
        cellLayout.cellToRect(0, 0, 1, 1, outRect)

        val target = IntArray(2)
        val targetSpan = IntArray(2)

        // Requesting 1x1 near (0, 0) pixel position should find nearest available cell (1, 0) or (0, 1)
        cellLayout.findNearestVacantArea(outRect.centerX(), outRect.centerY(), 1, 1, 1, 1, target, targetSpan)
        assertTrue(target[0] >= 0 && target[1] >= 0)
        assertFalse("Cannot pick the already occupied (0,0) cell", target[0] == 0 && target[1] == 0)
        assertEquals(1, targetSpan[0])
        assertEquals(1, targetSpan[1])
    }

    @Test
    fun testItemConfigurationAndReorder() {
        val config = ItemConfiguration()
        val view1 = View(context)
        val lp1 = CellLayoutLayoutParams(1, 1, 1, 1)
        view1.layoutParams = lp1

        config.add(view1, CellAndSpan(1, 1, 1, 1))
        assertEquals(1, config.sortedViews.size)
        assertEquals(1, config.area())

        // Test save and restore
        config.map[view1]?.cellX = 2
        assertEquals(2, config.map[view1]?.cellX)
        config.save()

        config.map[view1]?.cellX = 3
        assertEquals(3, config.map[view1]?.cellX)

        config.restore()
        assertEquals(2, config.map[view1]?.cellX)

        // Bounding rect
        val bounds = Rect()
        config.getBoundingRectForViews(config.sortedViews, bounds)
        assertEquals(2, bounds.left)
        assertEquals(1, bounds.top)
        assertEquals(3, bounds.right)
        assertEquals(2, bounds.bottom)
    }

    @Test
    fun testViewClusterEdgeDetectionAndShift() {
        val config = ItemConfiguration()
        val view1 = View(context)
        view1.layoutParams = CellLayoutLayoutParams(1, 1, 1, 1)
        config.add(view1, CellAndSpan(1, 1, 1, 1))

        val viewsList = ArrayList<View>().apply { add(view1) }
        val cluster = ViewCluster(cellLayout, viewsList, config)

        val view2 = View(context)
        view2.layoutParams = CellLayoutLayoutParams(2, 1, 1, 1)
        config.add(view2, CellAndSpan(2, 1, 1, 1))

        // view2 is immediately to the right of view1
        val touchingRight = cluster.isViewTouchingEdge(view2, ViewCluster.RIGHT)
        assertTrue("view2 should touch cluster on RIGHT", touchingRight)

        // Shift cluster to the left
        cluster.shift(ViewCluster.LEFT, 1)
        assertEquals(0, config.map[view1]?.cellX)
    }

    @Test
    fun testMultipageCellLayoutSeam() {
        val multiPage = MultipageCellLayout(context)
        multiPage.setGridSize(8, 5)
        assertEquals(8, multiPage.countX)
        assertEquals(5, multiPage.countY)

        val algorithm = multiPage.createReorderAlgorithm()
        assertNotNull(algorithm)

        var executedUnderSeam = false
        val result = algorithm.simulateSeam {
            executedUnderSeam = true
            multiPage.isSeamWasAdded()
        }
        assertTrue(executedUnderSeam)
        assertTrue("Seam should have been added during simulateSeam block", result)
        assertFalse("Seam should be removed after simulateSeam completes", multiPage.isSeamWasAdded())
    }
}
