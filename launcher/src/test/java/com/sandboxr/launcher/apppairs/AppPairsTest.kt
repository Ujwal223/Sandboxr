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

package com.sandboxr.launcher.apppairs

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import org.robolectric.RuntimeEnvironment
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.DeviceProfile
import com.sandboxr.launcher.R
import com.sandboxr.launcher.dragndrop.DraggableView
import com.sandboxr.launcher.icons.BitmapInfo
import com.sandboxr.launcher.model.data.AppPairInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppPairsTest {

    private lateinit var context: Context
    private lateinit var app1: WorkspaceItemInfo
    private lateinit var app2: WorkspaceItemInfo

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()

        app1 = WorkspaceItemInfo().apply {
            title = "Browser"
            intent = Intent(Intent.ACTION_VIEW).apply {
                component = ComponentName("com.example.browser", "com.example.browser.MainActivity")
            }
            bitmap = BitmapInfo.fromBitmap(Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888))
        }

        app2 = WorkspaceItemInfo().apply {
            title = "Notes"
            intent = Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName("com.example.notes", "com.example.notes.MainActivity")
            }
            bitmap = BitmapInfo.fromBitmap(Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888))
        }
    }

    @Test
    fun testCanCreateAppPairValidation() {
        assertTrue(AppPairsController.canCreateAppPair(app1, app2))
        assertFalse(AppPairsController.canCreateAppPair(app1, app1))
        assertFalse(AppPairsController.canCreateAppPair(app1, null))
        assertFalse(AppPairsController.canCreateAppPair(null, app2))

        val appPair = AppPairsController.createAppPair(app1, app2)
        assertFalse(AppPairsController.canCreateAppPair(appPair, app1))
    }

    @Test
    fun testCreateAppPairInfo() {
        val appPair = AppPairsController.createAppPair(app1, app2, "Work & Notes")
        assertEquals(2, appPair.getAppContents().size)
        assertEquals("Work & Notes", appPair.title)
        assertEquals(0, appPair.getFirstApp().rank)
        assertEquals(1, appPair.getSecondApp().rank)
        assertEquals("Browser", appPair.getFirstApp().title)
        assertEquals("Notes", appPair.getSecondApp().title)
    }

    @Test
    fun testAppPairIconDrawingParams() {
        val params = AppPairIconDrawingParams(context, BubbleTextView.DISPLAY_WORKSPACE)
        assertTrue(params.iconSize > 0)
        assertTrue(params.backgroundSize > 0)
        assertTrue(params.centerChannelSize > 0)
        assertTrue(params.bigRadius > params.smallRadius)
        assertTrue(params.memberIconSize > 0)
        assertEquals(1f, params.hoverScale)

        val dpLandscape = DeviceProfile().apply {
            isLandscape = true
        }
        params.updateOrientation(dpLandscape)
        assertTrue(params.isLeftRightSplit)
    }

    @Test
    fun testAppPairIconDrawableDrawing() {
        val params = AppPairIconDrawingParams(context, BubbleTextView.DISPLAY_WORKSPACE)
        val icon1 = app1.newIcon(context, 0)
        val icon2 = app2.newIcon(context, 0)
        val drawable = AppPairIconDrawable(params, icon1, icon2)

        assertEquals(params.iconSize, drawable.intrinsicWidth)
        assertEquals(params.iconSize, drawable.intrinsicHeight)

        val bitmap = Bitmap.createBitmap(params.iconSize, params.iconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw left-right split
        params.isLeftRightSplit = true
        drawable.draw(canvas)

        // Draw top-bottom split
        params.isLeftRightSplit = false
        drawable.draw(canvas)
    }

    @Test
    fun testAppPairIconViewLifecycle() {
        val appPair = AppPairsController.createAppPair(app1, app2)
        val icon = AppPairIcon(context)
        assertEquals(DraggableView.DRAGGABLE_ICON, icon.getViewType())

        icon.setReorderBounceScale(0.95f)
        assertEquals(0.95f, icon.getReorderBounceScale())

        icon.updateInfo(appPair)
        assertEquals(appPair, icon.info)
        assertEquals(appPair, icon.tag)

        val bounds = Rect()
        icon.getSourceBounds(bounds)
        assertNotNull(bounds)

        // Test inflating full icon from layout
        val inflated = android.view.LayoutInflater.from(context)
            .inflate(R.layout.app_pair_icon, null) as AppPairIcon
        inflated.updateInfo(appPair)
        assertNotNull(inflated.iconDrawableArea)
        assertNotNull(inflated.titleTextView)
        assertEquals("Browser & Notes", inflated.titleTextView.text.toString())
    }

    @Test
    fun testAppPairsControllerLaunch() {
        val controller = AppPairsController(context)
        val appPair = AppPairsController.createAppPair(app1, app2)
        val launched = controller.launchAppPair(appPair)
        assertTrue(launched)
    }
}
