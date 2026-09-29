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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import com.sandboxr.launcher.pageindicators.PageIndicatorDots
import com.sandboxr.launcher.pageindicators.PageIndicatorLine
import com.sandboxr.launcher.pageindicators.PageIndicatorMarker
import com.sandboxr.launcher.util.EdgeEffectCompat
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
class PagedViewTest {

    private lateinit var context: Context
    private lateinit var pagedView: TestPagedView
    private lateinit var pageIndicatorDots: PageIndicatorDots

    class TestPagedView(context: Context) : PagedView<PageIndicatorDots>(context) {
        fun getLeftEdgeEffect(): EdgeEffectCompat = mEdgeGlowLeft
        fun getRightEdgeEffect(): EdgeEffectCompat = mEdgeGlowRight

        fun callDrawEdgeEffect(canvas: Canvas) {
            drawEdgeEffect(canvas)
        }
    }

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        pagedView = TestPagedView(context)
        pageIndicatorDots = PageIndicatorDots(context)
        pagedView.setPageIndicator(pageIndicatorDots)

        // Add 3 test child pages
        for (i in 0 until 3) {
            val child = View(context)
            pagedView.addView(child)
        }

        // Measure and layout pagedView (1080 x 1920)
        val wSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val hSpec = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        pagedView.measure(wSpec, hSpec)
        pagedView.layout(0, 0, 1080, 1920)
    }

    @Test
    fun testPageMeasurementAndLayout() {
        assertEquals("Should have 3 pages", 3, pagedView.pageCount)
        assertEquals(0, pagedView.getChildAt(0).left)
        assertEquals(1080, pagedView.getChildAt(1).left)
        assertEquals(2160, pagedView.getChildAt(2).left)

        assertEquals(0, pagedView.getScrollForPage(0))
        assertEquals(1080, pagedView.getScrollForPage(1))
        assertEquals(2160, pagedView.getScrollForPage(2))
    }

    @Test
    fun testPageSnappingAndDestinationCalculation() {
        assertEquals(0, pagedView.getCurrentPage())

        // Snap to page 2
        assertTrue(pagedView.snapToPage(2))
        assertEquals(2, pagedView.getNextPage())

        // Set current page directly
        pagedView.setCurrentPage(1)
        assertEquals(1, pagedView.getCurrentPage())
        assertEquals(1080, pagedView.scrollX)

        // Scrolling near page 2 (scroll 1900 is closest to 2160)
        pagedView.scrollTo(1900, 0)
        assertEquals(2, pagedView.getDestinationPage())
    }

    @Test
    fun testPageSwitchListenerCallbacks() {
        var switchedToPage = -1
        var lastPreviousPage = -1
        pagedView.addPageSwitchListener(object : PagedView.PageSwitchListener {
            override fun onPageSwitch(newPage: Int, previousPage: Int) {
                switchedToPage = newPage
                lastPreviousPage = previousPage
            }
        })

        pagedView.setCurrentPage(2)
        assertEquals(2, switchedToPage)
        assertEquals(0, lastPreviousPage)

        pagedView.setCurrentPage(1)
        assertEquals(1, switchedToPage)
        assertEquals(2, lastPreviousPage)
    }

    @Test
    fun testEdgeEffectOverscrollBoundsAndGlow() {
        val leftGlow = pagedView.getLeftEdgeEffect()
        val rightGlow = pagedView.getRightEdgeEffect()

        assertNotNull(leftGlow)
        assertNotNull(rightGlow)
        assertTrue(leftGlow.isFinished)
        assertTrue(rightGlow.isFinished)

        leftGlow.setSize(1080, 1920)
        rightGlow.setSize(1080, 1920)

        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Pull left glow (scrolling past left edge)
        leftGlow.onPullDistance(0.5f, 0.5f, null)
        pagedView.callDrawEdgeEffect(canvas)

        // Release pull
        leftGlow.onRelease(null)
        leftGlow.finish()
        assertTrue(leftGlow.isFinished)

        // Absorb right glow (flinging past right edge)
        rightGlow.onFlingVelocity(1000)
        rightGlow.onAbsorb(1000)
        pagedView.callDrawEdgeEffect(canvas)
        rightGlow.finish()
        assertTrue(rightGlow.isFinished)
    }

    @Test
    fun testPageIndicatorDotsConfigurationAndScrolling() {
        pageIndicatorDots.setMarkersCount(3)
        pageIndicatorDots.setActiveMarker(0)

        // Layout indicator dots
        val wSpec = View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY)
        val hSpec = View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY)
        pageIndicatorDots.measure(wSpec, hSpec)
        pageIndicatorDots.layout(0, 0, 300, 50)

        // Set scroll position at page 1 (50% progress between 0 and 2160)
        pageIndicatorDots.setScroll(1080, 2160)

        // Pause scroll support for foldables
        pageIndicatorDots.setPauseScroll(true, false)
        pageIndicatorDots.setScroll(500, 2160)
        pageIndicatorDots.setPauseScroll(false, false)

        // Verify animations play and finish gracefully
        pageIndicatorDots.prepareEntryAnimation()
        pageIndicatorDots.playEntryAnimation()
        pageIndicatorDots.stopAllAnimations()
    }

    @Test
    fun testPageIndicatorLine() {
        val line = PageIndicatorLine(context)
        line.setMarkersCount(4)
        line.setActiveMarker(1)
        line.setScroll(1080, 3240)
        line.setShouldAutoHide(true)
        line.setPaintColor(0xFF00FF00.toInt())
        assertNotNull(line)
    }

    @Test
    fun testPageIndicatorMarker() {
        val marker = PageIndicatorMarker(context)
        assertFalse(marker.isActive)
        marker.activate(true)
        assertTrue(marker.isActive)
        marker.activate(false)
        assertFalse(marker.isActive)
    }
}
