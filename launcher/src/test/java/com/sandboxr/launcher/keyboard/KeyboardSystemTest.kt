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

package com.sandboxr.launcher.keyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.AbstractFloatingView
import com.sandboxr.launcher.CellLayout
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.dragndrop.DragOptions
import com.sandboxr.launcher.keyboard.FocusIndicatorHelper.SimpleFocusIndicatorHelper
import com.sandboxr.launcher.keyboard.KeyboardStateManager.KeyboardState
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.views.ActivityContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyboardSystemTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    // --- 1. FocusIndicatorHelper & ItemFocusIndicatorHelper Tests ---

    @Test
    fun testFocusIndicatorHelperPropertiesAndProperties() {
        val container = View(context)
        val helper = SimpleFocusIndicatorHelper(container)

        // Float properties
        ItemFocusIndicatorHelper.ALPHA.setValue(helper, 0.75f)
        assertEquals(0.75f, ItemFocusIndicatorHelper.ALPHA.get(helper)!!, 0.001f)

        ItemFocusIndicatorHelper.SHIFT.setValue(helper, 0.5f)
        assertEquals(0.5f, ItemFocusIndicatorHelper.SHIFT.get(helper)!!, 0.001f)

        // View to rect mapping
        val child = View(context).apply {
            layout(10, 20, 110, 120)
        }
        val outRect = Rect()
        helper.viewToRect(child, outRect)
        assertEquals(10, outRect.left)
        assertEquals(20, outRect.top)
        assertEquals(110, outRect.right)
        assertEquals(120, outRect.bottom)

        // Focus change trigger
        helper.onFocusChange(child, true)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Canvas drawing should not crash
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        helper.draw(canvas)
    }

    // --- 2. ViewGroupFocusHelper Tests ---

    @Test
    fun testViewGroupFocusHelperRelativeCoordinates() {
        val root = FrameLayout(context).apply {
            layout(0, 0, 500, 500)
        }
        val parent = FrameLayout(context).apply {
            layout(50, 50, 250, 250)
            x = 50f
            y = 50f
        }
        val child = View(context).apply {
            layout(10, 15, 60, 65)
            x = 10f
            y = 15f
        }
        parent.addView(child)
        root.addView(parent)

        val helper = ViewGroupFocusHelper(root)
        val outRect = Rect()
        helper.viewToRect(child, outRect)

        // 50 (parent.x) + 10 (child.x) = 60
        // 50 (parent.y) + 15 (child.y) = 65
        assertEquals(60, outRect.left)
        assertEquals(65, outRect.top)
        assertEquals(110, outRect.right) // width is 50
        assertEquals(115, outRect.bottom) // height is 50
    }

    // --- 3. FocusedItemDecorator Tests ---

    @Test
    fun testFocusedItemDecoratorAttachment() {
        val container = View(context)
        val helper = SimpleFocusIndicatorHelper(container)
        val decorator = FocusedItemDecorator(helper)

        assertEquals(helper, decorator.focusListener)

        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val recyclerView = RecyclerView(context)
        val state = RecyclerView.State()

        // Draw over should forward without exceptions
        decorator.onDrawOver(canvas, recyclerView, state)
    }

    // --- 4. KeyboardStateManager Tests ---

    @Test
    fun testKeyboardStateManagerStateTransitions() {
        val launcher = Robolectric.buildActivity(Launcher::class.java).setup().get()
        val stateManager = KeyboardStateManager(launcher)

        assertEquals(KeyboardState.HIDE, stateManager.keyboardState)
        assertTrue(stateManager.imeShownHeight > 0)

        // Update state to SHOW
        stateManager.keyboardState = KeyboardState.SHOW
        assertEquals(KeyboardState.SHOW, stateManager.keyboardState)
        assertTrue(stateManager.lastUpdatedTime > 0)

        // Update height
        stateManager.imeHeight = 420
        assertEquals(420, stateManager.imeHeight)
        assertEquals(420, stateManager.imeShownHeight)

        // Hide keyboard call should complete safely
        stateManager.hideKeyboard()

        // Verify software keyboard hidden check runs without error
        assertNotNull(stateManager.isSoftwareKeyboardHidden())
    }

    // --- 5. KeyboardDragAndDropView Tests ---

    @Test
    fun testKeyboardDragAndDropViewTypeAndLifecycle() {
        val launcher = Robolectric.buildActivity(Launcher::class.java).setup().get()
        val dndView = KeyboardDragAndDropView(launcher, null)

        assertTrue(dndView.isOfType(AbstractFloatingView.TYPE_DRAG_DROP_POPUP))
        assertFalse(dndView.isOfType(AbstractFloatingView.TYPE_ACTION_POPUP))

        // Consume touch
        val event = android.view.MotionEvent.obtain(0L, 0L, android.view.MotionEvent.ACTION_DOWN, 100f, 100f, 0)
        assertTrue(dndView.onControllerInterceptTouchEvent(event))
        event.recycle()

        // Set Insets
        dndView.setInsets(Rect(10, 20, 30, 40))
        assertEquals(10, dndView.paddingLeft)
        assertEquals(20, dndView.paddingTop)
        assertEquals(30, dndView.paddingRight)
        assertEquals(40, dndView.paddingBottom)
    }

    @Test
    fun testKeyboardDragAndDropViewKeyHandling() {
        val launcher = Robolectric.buildActivity(Launcher::class.java).setup().get()
        val dndView = KeyboardDragAndDropView(launcher, null)

        // Non-enter keys should delegate to super
        val aKeyEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A)
        assertFalse(dndView.onKeyUp(KeyEvent.KEYCODE_A, aKeyEvent))

        // Enter key when selection is null delegates to super
        val enterKeyEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
        assertFalse(dndView.onKeyUp(KeyEvent.KEYCODE_ENTER, enterKeyEvent))

        // Navigation directions when no selection is present returns false gracefully
        assertFalse(dndView.dispatchUnhandledMove(dndView, View.FOCUS_RIGHT))
        assertFalse(dndView.dispatchUnhandledMove(dndView, View.FOCUS_LEFT))
        assertFalse(dndView.dispatchUnhandledMove(dndView, View.FOCUS_UP))
        assertFalse(dndView.dispatchUnhandledMove(dndView, View.FOCUS_DOWN))
        assertFalse(dndView.dispatchUnhandledMove(dndView, View.FOCUS_FORWARD))
        assertFalse(dndView.dispatchUnhandledMove(dndView, View.FOCUS_BACKWARD))
    }

    @Test
    fun testCellLayoutDragAndDropAccessibilityDelegate() {
        val cellLayout = CellLayout(context)
        val delegate = cellLayout.getDragAndDropAccessibilityDelegate()
        assertNotNull(delegate)
        assertEquals(cellLayout, delegate.host)

        val virtualViews = ArrayList<Int>()
        delegate.getVisibleVirtualViews(virtualViews)
        assertTrue(virtualViews.size > 0)
    }
}
