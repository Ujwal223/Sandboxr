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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Process
import android.os.UserHandle
import android.view.View
import com.sandboxr.launcher.dot.DotInfo
import com.sandboxr.launcher.dot.DotRenderer
import com.sandboxr.launcher.dot.FolderDotInfo
import com.sandboxr.launcher.dragndrop.DraggableView
import com.sandboxr.launcher.icons.BitmapInfo
import com.sandboxr.launcher.icons.FastBitmapDrawable
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.notification.NotificationKeyData
import com.sandboxr.launcher.shapes.RoundedSquareShape
import com.sandboxr.launcher.shapes.ShapesProvider
import com.sandboxr.launcher.shapes.SuperellipseShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BubbleTextViewTest {

    private lateinit var context: Context
    private lateinit var bubbleTextView: BubbleTextView
    private lateinit var testBitmap: Bitmap
    private lateinit var testBitmapInfo: BitmapInfo

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        bubbleTextView = BubbleTextView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(200, 250)
        }

        testBitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        testBitmapInfo = BitmapInfo.fromBitmap(testBitmap, Color.BLUE)

        val wSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY)
        val hSpec = View.MeasureSpec.makeMeasureSpec(250, View.MeasureSpec.EXACTLY)
        bubbleTextView.measure(wSpec, hSpec)
        bubbleTextView.layout(0, 0, 200, 250)
    }

    @Test
    fun testWorkspaceItemBindingAndLabelTruncation() {
        val itemInfo = WorkspaceItemInfo().apply {
            title = "Sandboxr Secure Browser"
            contentDescription = "Open Sandboxr Secure Browser"
            bitmap = testBitmapInfo
            intent = Intent().setComponent(ComponentName("com.sandboxr.browser", "com.sandboxr.browser.MainActivity"))
            user = Process.myUserHandle()
        }

        bubbleTextView.applyFromWorkspaceItem(itemInfo)

        assertEquals("Sandboxr Secure Browser", bubbleTextView.text.toString())
        assertEquals("Open Sandboxr Secure Browser", bubbleTextView.contentDescription.toString())
        assertEquals(itemInfo, bubbleTextView.tag)
        assertNotNull(bubbleTextView.getIcon())
        assertTrue(bubbleTextView.isIconVisible())

        // Test label re-application
        itemInfo.title = "Sandboxr Browser"
        bubbleTextView.reapplyItemInfo(itemInfo)
        assertEquals("Sandboxr Browser", bubbleTextView.text.toString())
    }

    @Test
    fun testApplicationInfoBinding() {
        val appInfo = AppInfo(
            ComponentName("com.sandboxr.mail", "com.sandboxr.mail.MainActivity"),
            "Sandboxr Mail",
            Process.myUserHandle(),
            Intent()
        ).apply {
            bitmap = testBitmapInfo
        }

        bubbleTextView.applyFromApplicationInfo(appInfo)

        assertEquals("Sandboxr Mail", bubbleTextView.text.toString())
        assertEquals(appInfo, bubbleTextView.tag)
        assertNotNull(bubbleTextView.getIcon())
    }

    @Test
    fun testNotificationDotBadgeRendering() {
        val dotInfo = DotInfo()
        val notif1 = NotificationKeyData(notificationKey = "key_msg_1", count = 3)
        val notif2 = NotificationKeyData(notificationKey = "key_msg_2", count = 2)

        dotInfo.addOrUpdateNotificationKey(notif1)
        dotInfo.addOrUpdateNotificationKey(notif2)

        assertEquals(5, dotInfo.getNotificationCount())
        assertTrue(dotInfo.hasDot())

        bubbleTextView.setDotInfo(dotInfo, animate = false)
        assertTrue(bubbleTextView.hasDot())

        val drawParams = bubbleTextView.getDotParams()
        bubbleTextView.animateDotScale(1f, animate = false)
        assertEquals(1f, drawParams.scale, 0.01f)

        // Draw onto test canvas
        val canvasBitmap = Bitmap.createBitmap(200, 250, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)
        bubbleTextView.draw(canvas)

        // Test force hide dot
        bubbleTextView.setForceHideDot(true)
        bubbleTextView.draw(canvas)

        // Test removing notification key
        dotInfo.removeNotificationKey(notif1)
        assertEquals(2, dotInfo.getNotificationCount())

        dotInfo.removeNotificationKey(notif2)
        assertEquals(0, dotInfo.getNotificationCount())
        assertFalse(dotInfo.hasDot())
    }

    @Test
    fun testFolderDotInfoAggregation() {
        val folderDotInfo = FolderDotInfo()
        val itemDot1 = DotInfo().apply {
            addOrUpdateNotificationKey(NotificationKeyData("item1_k1", count = 4))
        }
        val itemDot2 = DotInfo().apply {
            addOrUpdateNotificationKey(NotificationKeyData("item2_k1", count = 6))
        }

        assertFalse(folderDotInfo.hasDot())

        folderDotInfo.addDotInfo(itemDot1)
        assertTrue(folderDotInfo.hasDot())
        assertEquals(4, folderDotInfo.getNotificationCount())

        folderDotInfo.addDotInfo(itemDot2)
        assertEquals(10, folderDotInfo.getNotificationCount())

        folderDotInfo.subtractDotInfo(itemDot1)
        assertEquals(6, folderDotInfo.getNotificationCount())

        folderDotInfo.subtractDotInfo(itemDot2)
        assertEquals(0, folderDotInfo.getNotificationCount())
        assertFalse(folderDotInfo.hasDot())
    }

    @Test
    fun testIconBoundsAndGeometry() {
        bubbleTextView.iconSize = 96
        val bounds = Rect()
        bubbleTextView.getIconBounds(bounds)

        // In 200px width with 96px icon, left offset should be (200 - 96) / 2 = 52
        assertEquals(52, bounds.left)
        assertEquals(148, bounds.right)
        assertEquals(bubbleTextView.paddingTop, bounds.top)
        assertEquals(bubbleTextView.paddingTop + 96, bounds.bottom)

        // Source bounds for drag
        val srcBounds = Rect()
        bubbleTextView.getSourceBounds(srcBounds)
        assertEquals(bounds, srcBounds)
        assertEquals(DraggableView.DRAGGABLE_ICON, bubbleTextView.getViewType())
    }

    @Test
    fun testTouchFeedbackAndReorderDynamics() {
        assertFalse(bubbleTextView.isStayPressed())
        bubbleTextView.setStayPressed(true)
        assertTrue(bubbleTextView.isStayPressed())

        // Reorder bounce scale
        bubbleTextView.setReorderBounceScale(0.88f)
        assertEquals(0.88f, bubbleTextView.getReorderBounceScale(), 0.01f)
        assertEquals(0.88f, bubbleTextView.scaleX, 0.01f)
        assertEquals(0.88f, bubbleTextView.scaleY, 0.01f)

        // Translate delegate
        val delegate = bubbleTextView.getTranslateDelegate()
        assertNotNull(delegate)

        // Icon visibility toggling
        bubbleTextView.setIconVisible(false)
        assertFalse(bubbleTextView.isIconVisible())
        bubbleTextView.setIconVisible(true)
        assertTrue(bubbleTextView.isIconVisible())

        // Text alpha property
        bubbleTextView.setTextAlpha(0.6f)
        assertEquals(0.6f, bubbleTextView.getTextAlpha(), 0.01f)
    }

    @Test
    fun testShapesProviderAndSuperellipseSquircle() {
        val shapes = ShapesProvider.iconShapes
        assertTrue("Must provide icon shapes", shapes.isNotEmpty())

        val squircleModel = ShapesProvider.getShapeByKey(ShapesProvider.SQUIRCLE_KEY)
        assertNotNull(squircleModel)
        assertEquals(ShapesProvider.SQUIRCLE_KEY, squircleModel.key)

        val circleModel = ShapesProvider.getShapeByKey(ShapesProvider.CIRCLE_KEY)
        assertNotNull(circleModel)
        assertEquals(ShapesProvider.CIRCLE_KEY, circleModel.key)

        // Test SuperellipseShape generator
        val squircle = SuperellipseShape(exponent = 4.0f)
        val path = squircle.createPath(width = 100f, height = 100f)
        assertNotNull(path)
        assertFalse("Path should not be empty", path.isEmpty)

        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED }
        squircle.draw(canvas, RectF(0f, 0f, 100f, 100f), paint)

        // Test RoundedSquareShape
        val roundedSquare = RoundedSquareShape(radiusRatio = 0.3f)
        val squarePath = roundedSquare.createPath(100f, 100f)
        assertNotNull(squarePath)
        assertFalse("Square path should not be empty", squarePath.isEmpty)
        roundedSquare.draw(canvas, RectF(0f, 0f, 100f, 100f), paint)
    }
}
