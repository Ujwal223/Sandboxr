/*
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

package com.sandboxr.launcher.splitscreen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.widget.FrameLayout
import com.android.systemui.shared.recents.model.Task
import com.sandboxr.launcher.desktop.DefaultDesktopStateProvider
import com.sandboxr.launcher.desktop.DesktopRecentsTransitionController
import com.sandboxr.launcher.desktop.DesktopShortcut
import com.sandboxr.launcher.desktop.DesktopStateProvider
import com.sandboxr.launcher.desktop.DesktopTask
import com.sandboxr.launcher.desktop.DesktopTaskContentView
import com.sandboxr.launcher.desktop.DesktopTaskView
import com.sandboxr.launcher.desktop.DesktopVisibilityController
import com.sandboxr.launcher.desktop.FloatingDesktopTaskView
import com.sandboxr.launcher.recents.views.TaskViewType
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.STAGE_POSITION_BOTTOM_OR_RIGHT
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.STAGE_POSITION_TOP_OR_LEFT
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.STAGE_POSITION_UNDEFINED
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
import java.io.PrintWriter
import java.io.StringWriter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SplitScreenAndDesktopTest {

    private lateinit var context: Context
    private lateinit var splitController: SplitSelectStateController

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        splitController = SplitSelectStateController(context)
    }

    // --- Split-Screen Tests ---

    @Test
    fun testSplitSelectStateController_stageInitiationAndCompletion() {
        var initiatedStage = STAGE_POSITION_UNDEFINED
        var secondAppSelected = false
        var confirmed = false
        var aborted = false

        splitController.registerListener(object : SplitSelectStateController.SplitSelectionListener {
            override fun onSplitSelectionInitiated(stagePosition: Int) {
                initiatedStage = stagePosition
            }

            override fun onSecondAppSelected() {
                secondAppSelected = true
            }

            override fun onSplitSelectionConfirmed() {
                confirmed = true
            }

            override fun onSplitSelectionAborted() {
                aborted = true
            }
        })

        // Stage 1: Initiate split with primary task
        splitController.initiateSplitSelect(101, STAGE_POSITION_TOP_OR_LEFT)
        assertTrue(splitController.isSplitSelectActive)
        assertEquals(STAGE_POSITION_TOP_OR_LEFT, splitController.activeStagePosition)
        assertEquals(STAGE_POSITION_TOP_OR_LEFT, initiatedStage)
        assertEquals(101, splitController.splitSelectDataHolder.initialTaskId)

        // Stage 2: Select second task
        splitController.setSecondTask(202)
        assertTrue(secondAppSelected)
        assertEquals(202, splitController.splitSelectDataHolder.secondTaskId)
        assertEquals(STAGE_POSITION_BOTTOM_OR_RIGHT, splitController.splitSelectDataHolder.secondStagePosition)
        assertTrue(splitController.splitSelectDataHolder.isBothSplitAppsConfirmed)

        // Stage 3: Launch split tasks
        var launchSuccess = false
        splitController.launchSplitTasks { success ->
            launchSuccess = success
        }
        assertTrue(confirmed)
        assertTrue(launchSuccess)
        assertFalse(splitController.isSplitSelectActive)
    }

    @Test
    fun testSplitSelectStateController_abortResetsState() {
        var aborted = false
        splitController.registerListener(object : SplitSelectStateController.SplitSelectionListener {
            override fun onSplitSelectionAborted() {
                aborted = true
            }
        })

        val intent = Intent("com.example.APP")
        splitController.initiateSplitSelect(intent, STAGE_POSITION_TOP_OR_LEFT)
        assertTrue(splitController.isSplitSelectActive)

        splitController.abortSplit()
        assertTrue(aborted)
        assertFalse(splitController.isSplitSelectActive)
        assertEquals(STAGE_POSITION_UNDEFINED, splitController.activeStagePosition)
    }

    @Test
    fun testSplitScreenUtils_calculateSplitBounds_portrait() {
        val displayBounds = Rect(0, 0, 1080, 2400)
        val outPrimary = Rect()
        val outSecondary = Rect()
        val outDivider = Rect()

        SplitScreenUtils.calculateSplitBounds(
            displayBounds = displayBounds,
            dividerSize = 48,
            splitRatio = 0.5f,
            isLeftRight = false,
            outPrimaryBounds = outPrimary,
            outSecondaryBounds = outSecondary,
            outDividerBounds = outDivider
        )

        // In portrait vertical split, widths are full screen
        assertEquals(0, outPrimary.left)
        assertEquals(1080, outPrimary.right)
        assertEquals(0, outSecondary.left)
        assertEquals(1080, outSecondary.right)

        // Divider is between primary bottom and secondary top
        assertEquals(outPrimary.bottom, outDivider.top)
        assertEquals(outDivider.bottom, outSecondary.top)
        assertEquals(48, outDivider.height())
        assertEquals(2400, outSecondary.bottom)
    }

    @Test
    fun testSplitScreenUtils_calculateSplitBounds_landscapeOrTablet() {
        val displayBounds = Rect(0, 0, 2560, 1600)
        val outPrimary = Rect()
        val outSecondary = Rect()
        val outDivider = Rect()

        SplitScreenUtils.calculateSplitBounds(
            displayBounds = displayBounds,
            dividerSize = 40,
            splitRatio = 0.5f,
            isLeftRight = true,
            outPrimaryBounds = outPrimary,
            outSecondaryBounds = outSecondary,
            outDividerBounds = outDivider
        )

        // In landscape horizontal split, heights are full screen
        assertEquals(0, outPrimary.top)
        assertEquals(1600, outPrimary.bottom)
        assertEquals(0, outSecondary.top)
        assertEquals(1600, outSecondary.bottom)

        // Divider is between primary right and secondary left
        assertEquals(outPrimary.right, outDivider.left)
        assertEquals(outDivider.right, outSecondary.left)
        assertEquals(40, outDivider.width())
        assertEquals(2560, outSecondary.right)
    }

    @Test
    fun testSplitScreenUtils_getOppositeStagePosition() {
        assertEquals(
            STAGE_POSITION_BOTTOM_OR_RIGHT,
            SplitScreenUtils.getOppositeStagePosition(STAGE_POSITION_TOP_OR_LEFT)
        )
        assertEquals(
            STAGE_POSITION_TOP_OR_LEFT,
            SplitScreenUtils.getOppositeStagePosition(STAGE_POSITION_BOTTOM_OR_RIGHT)
        )
        assertEquals(
            STAGE_POSITION_UNDEFINED,
            SplitScreenUtils.getOppositeStagePosition(STAGE_POSITION_UNDEFINED)
        )
    }

    @Test
    fun testSplitAnimationTimings() {
        val phoneTimings = SplitAnimationTimings.PHONE_OVERVIEW_TO_SPLIT
        assertEquals(SplitAnimationTimings.PHONE_ENTER_DURATION, phoneTimings.duration)
        assertTrue(phoneTimings.stagedRectSlideEndOffset in 0.0f..1.0f)
        assertTrue(phoneTimings.placeholderFadeInEndOffset in 0.0f..1.0f)

        val tabletTimings = SplitAnimationTimings.TABLET_OVERVIEW_TO_SPLIT
        assertEquals(SplitAnimationTimings.TABLET_ENTER_DURATION, tabletTimings.duration)
        assertTrue(tabletTimings.stagedRectSlideEndOffset in 0.0f..1.0f)
    }

    @Test
    fun testSplitDividerView_ratioAndDrag() {
        val dividerView = SplitDividerView(context)
        assertEquals(0.5f, dividerView.splitRatio, 0.001f)

        var draggedRatio = 0f
        var finishedRatio = 0f
        dividerView.dragListener = object : SplitDividerView.OnDividerDragListener {
            override fun onDividerDragged(ratio: Float) {
                draggedRatio = ratio
            }

            override fun onDividerDragFinished(ratio: Float) {
                finishedRatio = ratio
            }
        }

        dividerView.splitRatio = 0.6f
        assertEquals(0.6f, dividerView.splitRatio, 0.001f)

        // Test clamping (between 0.2 and 0.8)
        dividerView.splitRatio = 0.95f
        assertEquals(0.8f, dividerView.splitRatio, 0.001f)

        dividerView.splitRatio = 0.05f
        assertEquals(0.2f, dividerView.splitRatio, 0.001f)
    }

    @Test
    fun testSplitDropTarget_stagePositionAndHighlight() {
        val dropTarget = SplitDropTarget(context)
        dropTarget.stagePosition = STAGE_POSITION_TOP_OR_LEFT
        assertEquals(STAGE_POSITION_TOP_OR_LEFT, dropTarget.stagePosition)

        dropTarget.setHighlighted(true)
        assertEquals(1.0f, dropTarget.alpha, 0.01f)
        assertEquals(1.05f, dropTarget.scaleX, 0.01f)

        dropTarget.setHighlighted(false)
        assertEquals(0.7f, dropTarget.alpha, 0.01f)
        assertEquals(1.0f, dropTarget.scaleX, 0.01f)
    }

    @Test
    fun testSplitShortcut_executesSplitSelection() {
        val shortcut = SplitShortcut(
            context = context,
            splitController = splitController,
            taskId = 42
        )
        assertFalse(splitController.isSplitSelectActive)
        shortcut.onClick()
        assertTrue(splitController.isSplitSelectActive)
        assertEquals(42, splitController.splitSelectDataHolder.initialTaskId)
    }

    // --- Desktop Mode Tests ---

    @Test
    fun testDesktopVisibilityController_desksLifecycle() {
        val controller = DesktopVisibilityController(context)
        var addedDesk = -1
        var removedDesk = -1
        var activeDesk = -1
        var desktopModeState = false

        controller.registerDesktopVisibilityListener(object : DesktopVisibilityController.DesktopVisibilityListener {
            override fun onDeskAdded(displayId: Int, deskId: Int) {
                addedDesk = deskId
            }

            override fun onDeskRemoved(displayId: Int, deskId: Int) {
                removedDesk = deskId
            }

            override fun onActiveDeskChanged(displayId: Int, newActiveDesk: Int, oldActiveDesk: Int) {
                activeDesk = newActiveDesk
            }

            override fun onDesktopModeChanged(displayId: Int, inDesktopMode: Boolean) {
                desktopModeState = inDesktopMode
            }
        })

        val displayId = 0
        assertFalse(controller.isInDesktopMode(displayId))

        // Add Desk 1
        controller.addDesk(displayId, 1)
        assertEquals(1, addedDesk)
        assertFalse(controller.isInDesktopMode(displayId))

        // Activate Desk 1
        controller.setActiveDesk(displayId, 1)
        assertEquals(1, activeDesk)
        assertTrue(desktopModeState)
        assertTrue(controller.isInDesktopMode(displayId))
        assertEquals(1, controller.getActiveDeskId(displayId))

        // Test overview state interaction
        controller.setOverviewStateEnabled(displayId, true)
        assertFalse(controller.isInDesktopModeAndNotInOverview(displayId))

        controller.setOverviewStateEnabled(displayId, false)
        assertTrue(controller.isInDesktopModeAndNotInOverview(displayId))

        // Remove active desk -> exits desktop mode
        controller.removeDesk(displayId, 1)
        assertEquals(1, removedDesk)
        assertEquals(DesktopVisibilityController.INACTIVE_DESK_ID, activeDesk)
        assertFalse(desktopModeState)
        assertFalse(controller.isInDesktopMode(displayId))

        // Check dump logs
        val sw = StringWriter()
        controller.dumpLogs("  ", PrintWriter(sw))
        assertTrue(sw.toString().contains("DesktopVisibilityController"))
    }

    @Test
    fun testDesktopTask_groupingAndEquality() {
        val task1 = Task(Task.TaskKey(1, 0, Intent(), null, 0, 0L))
        val task2 = Task(Task.TaskKey(2, 0, Intent(), null, 0, 0L))

        val desktopTask1 = DesktopTask(deskId = 1, desktopDisplayId = 0, tasks = listOf(task1, task2))
        val desktopTask2 = DesktopTask(deskId = 1, desktopDisplayId = 0, tasks = listOf(task1, task2))
        val desktopTaskDifferentDesk = DesktopTask(deskId = 2, desktopDisplayId = 0, tasks = listOf(task1, task2))

        assertEquals(TaskViewType.DESKTOP, desktopTask1.taskViewType)
        assertEquals(2, desktopTask1.tasks.size)
        assertTrue(desktopTask1.containsTask(1))
        assertTrue(desktopTask1.containsTask(2))
        assertFalse(desktopTask1.containsTask(3))

        assertEquals(desktopTask1, desktopTask2)
        assertEquals(desktopTask1.hashCode(), desktopTask2.hashCode())
        assertFalse(desktopTask1 == desktopTaskDifferentDesk)

        val copy = desktopTask1.copy()
        assertEquals(desktopTask1.deskId, copy.deskId)
        assertEquals(desktopTask1.displayId, copy.displayId)
        assertEquals(desktopTask1.tasks.size, copy.tasks.size)
    }

    @Test
    fun testFloatingDesktopTaskView_boundsAndResize() {
        val container = FrameLayout(context)
        val startBounds = RectF(100f, 150f, 500f, 450f)
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val floatingView = FloatingDesktopTaskView.create(context, container, startBounds, bitmap)
        assertNotNull(floatingView)
        assertEquals(100f, floatingView.currentBounds.left, 0.01f)
        assertEquals(150f, floatingView.currentBounds.top, 0.01f)
        assertEquals(400f, floatingView.currentBounds.width(), 0.01f)
        assertEquals(300f, floatingView.currentBounds.height(), 0.01f)

        floatingView.resize(600, 500)
        assertEquals(600f, floatingView.currentBounds.width(), 0.01f)
        assertEquals(500f, floatingView.currentBounds.height(), 0.01f)
    }

    @Test
    fun testDesktopShortcut_activatesDesktopMode() {
        val controller = DesktopVisibilityController(context)
        var launchedTaskId = -1
        val shortcut = DesktopShortcut(
            context = context,
            taskId = 88,
            desktopController = controller,
            onLaunchedToDesktop = { taskId ->
                launchedTaskId = taskId
            }
        )

        shortcut.onClick()
        assertEquals(88, launchedTaskId)
        assertTrue(controller.isInDesktopMode(0))
    }

    @Test
    fun testDesktopRecentsTransitionController_transitionLifecycle() {
        val visibilityController = DesktopVisibilityController(context)
        val transitionController = DesktopRecentsTransitionController(context, visibilityController)

        val desktopTaskView = DesktopTaskView(context)
        val task1 = Task(Task.TaskKey(10, 0, Intent(), null, 0, 0L))
        val desktopTask = DesktopTask(deskId = 5, desktopDisplayId = 0, tasks = listOf(task1))
        desktopTaskView.bind(desktopTask)

        var launchedSuccess = false
        transitionController.launchDesktopFromRecents(
            desktopTaskView = desktopTaskView,
            animated = false
        ) { success ->
            launchedSuccess = success
        }

        assertTrue(launchedSuccess)
        assertFalse(transitionController.isDesktopLaunchOngoing)
        assertEquals(5, visibilityController.getActiveDeskId(0))
    }

    @Test
    fun testDesktopStateProvider() {
        val provider = DesktopStateProvider.get()
        assertNotNull(provider)
        assertTrue(provider.isDesktopModeSupported())

        DefaultDesktopStateProvider.setDesktopModeActive(true)
        assertTrue(provider.isDesktopModeActive())

        DefaultDesktopStateProvider.setDesktopModeActive(false)
        assertFalse(provider.isDesktopModeActive())
    }
}
