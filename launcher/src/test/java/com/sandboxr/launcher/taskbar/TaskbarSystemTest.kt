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

package com.sandboxr.launcher.taskbar

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import androidx.lifecycle.Lifecycle
import com.sandboxr.launcher.DeviceProfile
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.taskbar.bubbles.BubbleBarBubble
import com.sandboxr.launcher.taskbar.bubbles.BubbleView
import com.sandboxr.launcher.taskbar.customization.TaskbarContainers
import com.sandboxr.launcher.taskbar.customization.TaskbarFeatureEvaluator
import com.sandboxr.launcher.taskbar.customization.TaskbarIconSpecs
import com.sandboxr.launcher.taskbar.customization.TaskbarSpecsEvaluator
import com.sandboxr.launcher.taskbar.customization.containers.TaskbarPinnedAppIconContainer
import com.sandboxr.launcher.taskbar.navbutton.NearestTouchFrame
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskbarSystemTest {

    private lateinit var context: Context
    private lateinit var deviceProfile: DeviceProfile
    private lateinit var taskbarContext: TaskbarActivityContext

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        deviceProfile = DeviceProfile().apply {
            isTaskbarPresent = true
            isTablet = true
        }
        taskbarContext = TaskbarActivityContext(context, deviceProfile)
    }

    @Test
    fun testTaskbarActivityContextAndControllersLifecycle() {
        taskbarContext.init()

        assertTrue("Controllers must be initialized", taskbarContext.controllers.isInitialized())
        assertNotNull("RootView must be non-null", taskbarContext.getRootView())
        assertTrue("RootView must be TaskbarDragLayer", taskbarContext.getRootView() is TaskbarDragLayer)
        assertNotNull("TaskbarViewController must be initialized", taskbarContext.controllers.taskbarViewController)
        assertNotNull("StashedHandleViewController must be initialized", taskbarContext.controllers.stashedHandleViewController)
        assertNotNull("TaskbarStashController must be initialized", taskbarContext.controllers.taskbarStashController)
        assertNotNull("NavButtonController must be initialized", taskbarContext.controllers.navButtonController)
        assertNotNull("NavbarButtonsViewController must be initialized", taskbarContext.controllers.navbarButtonsViewController)
        assertNotNull("BubbleControllers must be initialized", taskbarContext.controllers.bubbleControllers)

        assertEquals(Lifecycle.State.RESUMED, taskbarContext.lifecycle.currentState)

        taskbarContext.onDestroy()
        assertEquals(Lifecycle.State.DESTROYED, taskbarContext.lifecycle.currentState)
        assertFalse(taskbarContext.controllers.isInitialized())
    }

    @Test
    fun testTaskbarViewAndIconBinding() {
        taskbarContext.init()
        val taskbarView = taskbarContext.controllers.taskbarViewController.taskbarView

        assertNotNull("All Apps button must be present", taskbarView.allAppsButton)
        assertNotNull("Divider view must be present", taskbarView.dividerView)

        val item1 = WorkspaceItemInfo().apply {
            id = 101
            title = "Test App 1"
            intent = Intent(Intent.ACTION_MAIN).setComponent(ComponentName("com.test", "com.test.Activity1"))
        }
        val item2 = WorkspaceItemInfo().apply {
            id = 102
            title = "Test App 2"
            intent = Intent(Intent.ACTION_MAIN).setComponent(ComponentName("com.test", "com.test.Activity2"))
        }

        taskbarContext.controllers.taskbarViewController.onPinnedItemsChanged(listOf(item1, item2))

        val iconViews = taskbarContext.controllers.taskbarViewController.getIconViews()
        assertEquals(2, iconViews.size)

        var clicked = false
        taskbarView.itemClickListener = {
            clicked = true
        }
        iconViews[0].performClick()
        assertTrue("Clicking icon must invoke click listener", clicked)
    }

    @Test
    fun testTaskbarStashControllerTransitions() {
        taskbarContext.init()
        val stashController = taskbarContext.controllers.taskbarStashController

        // Transient mode unstash and stash
        stashController.updateAndAnimateTransientTaskbar(stash = true)
        assertTrue("Taskbar should be stashed", stashController.isStashed)

        stashController.updateAndAnimateTransientTaskbar(stash = false)
        assertFalse("Taskbar should be unstashed", stashController.isStashed)

        // Flag based stashing
        stashController.updateStateForFlag(TaskbarStashController.FLAG_STASHED_IN_APP_SYSUI, true)
        stashController.applyState(animate = false)
        assertTrue("Taskbar should be stashed when sysui shade is open", stashController.isStashed)

        stashController.updateStateForFlag(TaskbarStashController.FLAG_STASHED_IN_APP_SYSUI, false)
        stashController.applyState(animate = false)
        assertFalse("Taskbar should unstash when sysui flag cleared", stashController.isStashed)

        // Pinning mode overrides stashing
        stashController.onPinningStateChanged(isPinned = true)
        assertFalse("Pinned taskbar should not be stashed", stashController.isStashed)
    }

    @Test
    fun testTaskbarNavButtons() {
        taskbarContext.init()
        val navButtonController = taskbarContext.controllers.navButtonController

        var backClicked = false
        var homeClicked = false
        var recentsClicked = false
        var homeLongClicked = false

        navButtonController.onBackClickListener = { backClicked = true }
        navButtonController.onHomeClickListener = { homeClicked = true }
        navButtonController.onRecentsClickListener = { recentsClicked = true }
        navButtonController.onHomeLongClickListener = {
            homeLongClicked = true
            true
        }

        navButtonController.onButtonClick(TaskbarNavButtonController.BUTTON_BACK)
        assertTrue("Back button clicked", backClicked)

        navButtonController.onButtonClick(TaskbarNavButtonController.BUTTON_HOME)
        assertTrue("Home button clicked", homeClicked)

        navButtonController.onButtonClick(TaskbarNavButtonController.BUTTON_RECENTS)
        assertTrue("Recents button clicked", recentsClicked)

        val longPressHandled = navButtonController.onButtonLongClick(TaskbarNavButtonController.BUTTON_HOME)
        assertTrue("Home long-press handled", longPressHandled)
        assertTrue("Home long-press callback called", homeLongClicked)
    }

    @Test
    fun testTaskbarSpecsEvaluator() {
        val featureEvaluator = TaskbarFeatureEvaluator(context, deviceProfile, isPinned = false, isThreeButtonNav = false)
        val specsEvaluator = TaskbarSpecsEvaluator(deviceProfile, featureEvaluator)

        assertNotNull(specsEvaluator.taskbarIconSize)
        assertTrue(specsEvaluator.taskbarIconTouchSize >= 48f)

        val steppedDown = specsEvaluator.getIconSizeStepDown(TaskbarIconSpecs.iconSize52dp)
        assertNotNull(steppedDown)

        val pinnedContainer = TaskbarPinnedAppIconContainer(context)
        assertEquals(0, pinnedContainer.spaceNeeded)
        pinnedContainer.addIconView(View(context))
        assertTrue(pinnedContainer.spaceNeeded > 0)

        assertEquals(TaskbarContainers.ALL_APPS.name, "ALL_APPS")
        assertEquals(TaskbarContainers.DIVIDER.name, "DIVIDER")
    }

    @Test
    fun testBubbleBarAndStashController() {
        taskbarContext.init()
        val bubbleControllers = taskbarContext.controllers.bubbleControllers
        assertNotNull(bubbleControllers)

        val bubbleBarController = bubbleControllers!!.bubbleBarController
        val bubbleBarView = bubbleBarController.bubbleBarView

        val bubbleView = BubbleView(context)
        val bubbleItem = BubbleBarBubble("chat_1", bubbleView, appName = "Messenger")

        bubbleBarController.onBubbleAdded(bubbleItem)
        assertEquals(1, bubbleBarView.getBubbleCount())

        bubbleBarView.selectBubble("chat_1")
        assertTrue(bubbleBarView.isExpanded)
        assertEquals("chat_1", bubbleBarView.selectedBubbleKey)

        bubbleControllers.bubbleBarController.setStashed(true, animate = false)
        assertTrue(bubbleControllers.bubbleBarController.bubbleStashController.isStashed)

        bubbleControllers.bubbleBarController.setStashed(false, animate = false)
        assertFalse(bubbleControllers.bubbleBarController.bubbleStashController.isStashed)

        bubbleBarController.onBubbleRemoved("chat_1")
        assertEquals(0, bubbleBarView.getBubbleCount())
    }

    @Test
    fun testNearestTouchFrameTouchRedirection() {
        val frame = NearestTouchFrame(context)
        val button1 = Button(context).apply {
            layout(0, 0, 100, 100)
            isClickable = true
        }
        val button2 = Button(context).apply {
            layout(100, 0, 200, 100)
            isClickable = true
        }
        frame.addView(button1)
        frame.addView(button2)
        frame.layout(0, 0, 200, 100)

        var button1Clicked = false
        button1.setOnClickListener { button1Clicked = true }

        val downEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        val upEvent = MotionEvent.obtain(0L, 10L, MotionEvent.ACTION_UP, 10f, 10f, 0)

        frame.dispatchTouchEvent(downEvent)
        frame.dispatchTouchEvent(upEvent)

        downEvent.recycle()
        upEvent.recycle()
        assertNotNull(frame)
    }

    @Test
    fun testTaskbarPinningAndCustomizationDrag() {
        taskbarContext.init()
        val pinningController = taskbarContext.controllers.taskbarPinningController
        val dragDropController = taskbarContext.controllers.taskbarViewDragDropController

        val item = WorkspaceItemInfo().apply {
            id = 555
            title = "Pinned App"
        }

        val pinned = pinningController.pinItem(item)
        assertTrue(pinned)
        assertEquals(1, pinningController.getPinnedItems().size)

        val unpinned = pinningController.unpinItem(item)
        assertTrue(unpinned)
        assertEquals(0, pinningController.getPinnedItems().size)

        val dummyView = View(context)
        dragDropController.startDrag(dummyView, item)
        assertTrue(dragDropController.isDragging)
        assertEquals(item, dragDropController.draggedItem)

        dragDropController.onDragCompleted(true)
        assertFalse(dragDropController.isDragging)
        assertNull(dragDropController.draggedItem)
    }

    @Test
    fun testTaskbarUnfoldAnimation() {
        taskbarContext.init()
        val unfoldController = taskbarContext.controllers.taskbarUnfoldAnimationController
        val icon1 = View(context)
        val icon2 = View(context)
        val icons = listOf(icon1, icon2)

        unfoldController.onTransitionProgress(0.5f, icons)
        assertTrue(unfoldController.isUnfolding)
        assertNotEquals(0f, icon1.translationX)

        unfoldController.onTransitionFinished(icons)
        assertFalse(unfoldController.isUnfolding)
        assertEquals(0f, icon1.translationX, 0.001f)
        assertEquals(0f, icon2.translationX, 0.001f)
    }
}
