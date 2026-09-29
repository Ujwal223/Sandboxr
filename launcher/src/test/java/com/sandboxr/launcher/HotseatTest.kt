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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.qsb.QsbContainerView
import com.sandboxr.launcher.qsb.QsbLayout
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
class HotseatTest {

    private lateinit var context: Context
    private lateinit var hotseat: Hotseat

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        hotseat = Hotseat(context)
        hotseat.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    @Test
    fun testHotseatInitializationAndCellLayoutGrid() {
        // Hotseat must be typed as CONTAINER_TYPE_HOTSEAT
        assertEquals(CellLayout.CONTAINER_TYPE_HOTSEAT, hotseat.containerType)

        // Must attach QSB view
        val qsb = hotseat.getQsb()
        assertNotNull("Hotseat must attach a QSB view", qsb)
        assertEquals(R.id.search_container_hotseat, qsb?.id)

        // Default horizontal layout has 5 columns and 1 row
        assertEquals(5, hotseat.countX)
        assertEquals(1, hotseat.countY)
        assertFalse(hotseat.isHasVerticalHotseat())
    }

    @Test
    fun testOrderToCellCoordinateMapping() {
        // Horizontal orientation: cellX = rank, cellY = 0
        assertEquals(0, hotseat.getCellXFromOrder(0))
        assertEquals(0, hotseat.getCellYFromOrder(0))
        assertEquals(3, hotseat.getCellXFromOrder(3))
        assertEquals(0, hotseat.getCellYFromOrder(3))

        // Reset to vertical hotseat (landscape phone mode)
        hotseat.resetLayout(hasVerticalHotseat = true)
        assertTrue(hotseat.isHasVerticalHotseat())
        assertEquals(1, hotseat.countX)
        assertEquals(5, hotseat.countY)

        // In vertical orientation: cellX = 0, cellY = countY - (rank + 1)
        assertEquals(0, hotseat.getCellXFromOrder(0))
        assertEquals(4, hotseat.getCellYFromOrder(0))
        assertEquals(0, hotseat.getCellXFromOrder(2))
        assertEquals(2, hotseat.getCellYFromOrder(2))
        assertEquals(0, hotseat.getCellXFromOrder(4))
        assertEquals(0, hotseat.getCellYFromOrder(4))
    }

    @Test
    fun testMultiValueAlphaChannels() {
        val shortcutsAndWidgets = hotseat.shortcutsAndWidgets

        // Initial alpha should be 1f
        assertEquals(1f, shortcutsAndWidgets.alpha, 0.001f)

        // Set Taskbar alignment alpha
        hotseat.setIconsAlpha(0.5f, Hotseat.ALPHA_CHANNEL_TASKBAR_ALIGNMENT)
        assertEquals(0.5f, hotseat.getIconsAlpha(Hotseat.ALPHA_CHANNEL_TASKBAR_ALIGNMENT).value, 0.001f)
        assertEquals(0.5f, shortcutsAndWidgets.alpha, 0.001f)

        // Blend with Preview Renderer alpha: 0.5 * 0.8 = 0.4
        hotseat.setIconsAlpha(0.8f, Hotseat.ALPHA_CHANNEL_PREVIEW_RENDERER)
        assertEquals(0.4f, shortcutsAndWidgets.alpha, 0.001f)

        // Reset
        hotseat.setIconsAlpha(1f, Hotseat.ALPHA_CHANNEL_TASKBAR_ALIGNMENT)
        hotseat.setIconsAlpha(1f, Hotseat.ALPHA_CHANNEL_PREVIEW_RENDERER)
        assertEquals(1f, shortcutsAndWidgets.alpha, 0.001f)

        // QSB Alpha channel
        hotseat.setQsbAlpha(0.6f, Hotseat.ALPHA_CHANNEL_TASKBAR_STASH)
        assertEquals(0.6f, hotseat.getQsbAlpha(Hotseat.ALPHA_CHANNEL_TASKBAR_STASH).value, 0.001f)
        assertEquals(0.6f, hotseat.getQsb()!!.alpha, 0.001f)
    }

    @Test
    fun testHorizontalInsetsAndQsbTheming() {
        val qsbLayout = QsbLayout(context)
        qsbLayout.layoutParams = ViewGroup.LayoutParams(1000, 150)
        qsbLayout.measure(
            MeasureSpec.makeMeasureSpec(1000, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(150, MeasureSpec.EXACTLY)
        )
        qsbLayout.layout(0, 0, 1000, 150)

        assertEquals(0f, qsbLayout.getHorizontalInsets(), 0.001f)

        // Inset by 15%
        qsbLayout.setHorizontalInsets(0.15f)
        assertEquals(0.15f, qsbLayout.getHorizontalInsets(), 0.001f)

        // Test QsbContainerView wrapping
        val container = QsbContainerView(context)
        container.addView(qsbLayout)
        container.setHorizontalInsets(0.20f)
        assertEquals(0.20f, container.getHorizontalInsets(), 0.001f)
        assertEquals(0.20f, qsbLayout.getHorizontalInsets(), 0.001f)

        // Test custom search hint
        qsbLayout.setSearchHint("Search Sandboxr...")
    }

    @Test
    fun testHotseatInsetsAndLayoutMeasurement() {
        // Set insets
        val insets = Rect(0, 0, 0, 96)
        hotseat.setInsets(insets)

        // Perform measure and layout
        val widthSpec = MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(400, MeasureSpec.EXACTLY)
        hotseat.measure(widthSpec, heightSpec)

        assertEquals(1080, hotseat.measuredWidth)
        assertEquals(400, hotseat.measuredHeight)

        hotseat.layout(0, 680, 1080, 1080)

        val qsb = hotseat.getQsb()
        assertNotNull(qsb)
        assertTrue("QSB should be measured", qsb!!.measuredWidth > 0)
        assertTrue("QSB should be laid out", qsb.bottom > qsb.top)
    }

    @Test
    fun testPredictedItemsIntegrationPoint() {
        var receivedItems: List<ItemInfo>? = null
        hotseat.setPredictionListener { items ->
            receivedItems = items
        }

        val itemInfo = WorkspaceItemInfo().apply {
            title = "Sandboxr Browser"
            intent = Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName("com.sandboxr.browser", "com.sandboxr.browser.MainActivity")
            }
        }

        hotseat.onPredictionsUpdated(listOf(itemInfo))
        val items = receivedItems
        assertNotNull(items)
        assertEquals(1, items!!.size)
        assertEquals("Sandboxr Browser", (items[0] as WorkspaceItemInfo).title)
    }

    @Test
    fun testWorkspaceTouchForwarding() {
        val workspace = Workspace<View>(context)
        hotseat.setWorkspace(workspace)
        assertEquals(workspace, hotseat.workspace)
        assertEquals(workspace, hotseat.getCellLayoutContainer())

        // Measure and layout so height is valid
        hotseat.measure(
            MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(300, MeasureSpec.EXACTLY)
        )
        hotseat.layout(0, 780, 1080, 1080)

        // Touch event inside threshold
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 500f, 100f, 0)
        // onInterceptTouchEvent will delegate to workspace
        val intercepted = hotseat.onInterceptTouchEvent(event)
        // Workspace doesn't intercept simple tap by default
        assertFalse(intercepted)
        event.recycle()
    }

    @Test
    fun testCustomQsbAttachment() {
        val customQsb = QsbLayout(context).apply {
            id = View.generateViewId()
        }

        hotseat.setQsb(customQsb)
        assertEquals(customQsb, hotseat.getQsb())

        // Alpha channel operations should now target customQsb
        hotseat.setQsbAlpha(0.3f, Hotseat.ALPHA_CHANNEL_TASKBAR_ALIGNMENT)
        assertEquals(0.3f, customQsb.alpha, 0.001f)
    }
}
