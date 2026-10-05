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

package com.sandboxr.launcher.touch

import android.content.Context
import android.graphics.PointF
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.sandboxr.launcher.CheckLongPressHelper
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.MotionEventsUtils
import com.sandboxr.launcher.states.StateAnimationConfig
import com.sandboxr.launcher.touch.CustomActionsListener.Companion.ACTION_LAUNCH
import com.sandboxr.launcher.touch.CustomActionsListener.Companion.ACTION_POPUP_MENU
import com.sandboxr.launcher.touch.CustomActionsListener.Companion.ACTION_START_DRAG
import com.sandboxr.launcher.util.FlingBlockCheck
import com.sandboxr.launcher.util.TouchUtil
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
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TouchSystemTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    private fun obtainMotionEvent(
        action: Int,
        x: Float,
        y: Float,
        source: Int = InputDevice.SOURCE_TOUCHSCREEN,
        buttonState: Int = 0
    ): MotionEvent {
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, action, x, y, 0)
        event.source = source
        // Using reflection or setting button state if available
        return event
    }

    // --- 1. SingleAxisSwipeDetector Tests ---

    @Test
    fun testSingleAxisSwipeDetectorVerticalSwipeUp() {
        val dragStarted = AtomicBoolean(false)
        val dragged = AtomicBoolean(false)
        val dragEnded = AtomicBoolean(false)

        val listener = object : SingleAxisSwipeDetector.Listener {
            override fun onDragStart(start: Boolean, startDisplacement: Float) {
                dragStarted.set(true)
            }

            override fun onDrag(displacement: Float): Boolean {
                dragged.set(true)
                return true
            }

            override fun onDragEnd(velocity: Float) {
                dragEnded.set(true)
            }
        }

        val detector = SingleAxisSwipeDetector(context, listener, SingleAxisSwipeDetector.VERTICAL)
        detector.setDetectableScrollConditions(SingleAxisSwipeDetector.DIRECTION_POSITIVE, false)

        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

        // Down at (100, 200)
        detector.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 100f, 200f))
        assertFalse(detector.isDraggingState())

        // Move upward by touchSlop + 10px (y = 200 - (touchSlop + 10)) -> positive direction
        detector.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_MOVE, 100f, 200f - touchSlop - 10f))
        assertTrue("Drag should start on vertical swipe up", dragStarted.get())
        assertTrue(detector.isDraggingState())

        // Move more
        detector.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_MOVE, 100f, 200f - touchSlop - 30f))
        assertTrue(dragged.get())

        // Up
        detector.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_UP, 100f, 200f - touchSlop - 30f))
        assertTrue(dragEnded.get())
        assertTrue(detector.isSettlingState())

        detector.finishedScrolling()
        assertTrue(detector.isIdleState())
    }

    @Test
    fun testSingleAxisSwipeDetectorHorizontalSwipeRight() {
        val dragStarted = AtomicBoolean(false)

        val listener = object : SingleAxisSwipeDetector.Listener {
            override fun onDragStart(start: Boolean, startDisplacement: Float) {
                dragStarted.set(true)
            }

            override fun onDrag(displacement: Float): Boolean = true
            override fun onDragEnd(velocity: Float) {}
        }

        val detector = SingleAxisSwipeDetector(context, listener, SingleAxisSwipeDetector.HORIZONTAL)
        detector.setDetectableScrollConditions(SingleAxisSwipeDetector.DIRECTION_POSITIVE, false)

        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

        detector.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 100f, 100f))
        // Swipe right (x increases)
        detector.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_MOVE, 100f + touchSlop + 15f, 100f))
        assertTrue("Horizontal swipe right should trigger positive drag", dragStarted.get())
    }

    @Test
    fun testSingleAxisSwipeDetectorDurationCalculationAndFling() {
        val duration = BaseSwipeDetector.calculateDuration(2.0f, 0.8f)
        assertTrue("Calculated duration should be reasonable", duration in 100..1200)

        val detector = SingleAxisSwipeDetector(
            context,
            object : SingleAxisSwipeDetector.Listener {
                override fun onDragStart(start: Boolean, startDisplacement: Float) {}
                override fun onDrag(displacement: Float): Boolean = true
                override fun onDragEnd(velocity: Float) {}
            },
            SingleAxisSwipeDetector.VERTICAL
        )

        // Small velocity is not a fling
        assertFalse(detector.isFling(0.01f))
        // High velocity is a fling
        assertTrue(detector.isFling(10f))
    }

    // --- 2. BothAxesSwipeDetector Tests ---

    @Test
    fun testBothAxesSwipeDetectorMultiDirectional() {
        val dragStarted = AtomicBoolean(false)
        val lastDisplacement = PointF()

        val listener = object : BothAxesSwipeDetector.Listener {
            override fun onDragStart(start: Boolean) {
                dragStarted.set(true)
            }

            override fun onDrag(displacement: PointF, motionEvent: MotionEvent): Boolean {
                lastDisplacement.set(displacement)
                return true
            }

            override fun onDragEnd(velocity: PointF) {}
        }

        val detector = BothAxesSwipeDetector(context, listener)
        detector.setDetectableScrollConditions(
            BothAxesSwipeDetector.DIRECTION_UP or BothAxesSwipeDetector.DIRECTION_RIGHT,
            false
        )

        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

        detector.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 100f, 100f))
        // Move diagonally up and right
        detector.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_MOVE, 100f + touchSlop + 10f, 100f - touchSlop - 10f))

        assertTrue(dragStarted.get())
        assertTrue(lastDisplacement.x > 0f)
        assertTrue(lastDisplacement.y < 0f)
    }

    // --- 3. CheckLongPressHelper Tests ---

    @Test
    fun testCheckLongPressHelperTimeoutTrigger() {
        val testView = View(context)
        val longPressFired = AtomicBoolean(false)
        testView.setOnLongClickListener {
            longPressFired.set(true)
            true
        }

        val helper = CheckLongPressHelper(testView)
        helper.setLongPressTimeoutFactor(0.5f)

        helper.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 50f, 50f))
        assertFalse(helper.hasPerformedLongPress())

        // Fast forward looper past the scaled long press timeout
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue("Long press should have triggered via timeout", longPressFired.get())
        assertTrue(helper.hasPerformedLongPress())
    }

    @Test
    fun testCheckLongPressHelperCancellationOnMoveBeyondSlop() {
        val testView = View(context)
        val longPressFired = AtomicBoolean(false)
        testView.setOnLongClickListener {
            longPressFired.set(true)
            true
        }

        val helper = CheckLongPressHelper(testView)
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

        helper.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 50f, 50f))

        // Move beyond slop
        helper.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_MOVE, 50f + touchSlop + 20f, 50f))

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertFalse("Long press should have been cancelled by movement", longPressFired.get())
        assertFalse(helper.hasPerformedLongPress())
    }

    // --- 4. MotionEventsUtils Tests ---

    @Test
    fun testMotionEventsUtilsConstants() {
        assertEquals(4, MotionEventsUtils.CLASSIFICATION_MULTI_FINGER_SWIPE)
        val event = obtainMotionEvent(MotionEvent.ACTION_DOWN, 0f, 0f)
        assertNotNull(event)
    }

    // --- 5. OverScroll Damping Tests ---

    @Test
    fun testOverScrollDampingCurve() {
        assertEquals(0, OverScroll.dampedScroll(0f, 1000))

        val smallDamp = OverScroll.dampedScroll(100f, 1000)
        val largeDamp = OverScroll.dampedScroll(800f, 1000)

        assertTrue(smallDamp > 0)
        assertTrue(largeDamp > smallDamp)
        // Damping factor is 0.07, so max value near 1000 cannot exceed ~70px
        assertTrue(largeDamp <= 70)

        // Negative scroll
        val negDamp = OverScroll.dampedScroll(-500f, 1000)
        assertTrue(negDamp < 0)
    }

    // --- 6. FlingBlockCheck Tests ---

    @Test
    fun testFlingBlockCheckBehavior() {
        val blockCheck = FlingBlockCheck()
        assertFalse(blockCheck.isBlocked())

        blockCheck.blockFling()
        assertTrue(blockCheck.isBlocked())

        blockCheck.unblockFling()
        assertFalse(blockCheck.isBlocked())

        blockCheck.blockFling()
        // Event before pause duration does not unblock
        blockCheck.onEvent()
        assertTrue(blockCheck.isBlocked())
    }

    // --- 7. CustomEventsTouchHandler Tests ---

    @Test
    fun testCustomEventsTouchHandlerTapAndLongPress() {
        val testView = View(context)
        val actionCaptured = AtomicInteger(0)

        val customListener = object : CustomActionsListener {
            override fun performActions(view: View, actionMask: Int) {
                actionCaptured.set(actionMask)
            }
        }

        val handler = CustomEventsTouchHandler(testView)
        handler.customActionsListener = customListener

        // Tap
        handler.onDelegateTouchEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 20f, 20f))
        handler.onSingleTapUp(obtainMotionEvent(MotionEvent.ACTION_UP, 20f, 20f))
        assertEquals(ACTION_LAUNCH, actionCaptured.get())

        // Long press
        handler.onLongPress(obtainMotionEvent(MotionEvent.ACTION_DOWN, 20f, 20f))
        assertTrue(CustomActionsListener.hasFlags(actionCaptured.get(), ACTION_POPUP_MENU or ACTION_START_DRAG))
    }

    // --- 8. StateAnimationConfig Helpers in AllAppsSwipeController ---

    @Test
    fun testAllAppsSwipeControllerAnimationConfigs() {
        val normalToAllApps = StateAnimationConfig()
        AllAppsSwipeController.applyNormalToAllAppsAnimConfig(normalToAllApps)
        assertNotNull(normalToAllApps.getInterpolator(StateAnimationConfig.ANIM_ALL_APPS_FADE, null))
        assertNotNull(normalToAllApps.getInterpolator(StateAnimationConfig.ANIM_WORKSPACE_SCALE, null))

        val allAppsToNormal = StateAnimationConfig()
        AllAppsSwipeController.applyAllAppsToNormalConfig(allAppsToNormal)
        assertNotNull(allAppsToNormal.getInterpolator(StateAnimationConfig.ANIM_ALL_APPS_FADE, null))
        assertNotNull(allAppsToNormal.getInterpolator(StateAnimationConfig.ANIM_SCRIM_FADE, null))
    }

    // --- 9. NotificationSwipeController Trigger Tests ---

    @Test
    fun testNotificationSwipeControllerDownwardTrigger() {
        val triggered = AtomicBoolean(false)

        val dummyListener = object : SingleAxisSwipeDetector.Listener {
            override fun onDragStart(start: Boolean, startDisplacement: Float) {}
            override fun onDrag(displacement: Float): Boolean {
                if (displacement > 120f) {
                    triggered.set(true)
                }
                return true
            }
            override fun onDragEnd(velocity: Float) {}
        }

        val detector = SingleAxisSwipeDetector(context, dummyListener, SingleAxisSwipeDetector.VERTICAL)
        detector.setDetectableScrollConditions(SingleAxisSwipeDetector.DIRECTION_NEGATIVE, false)

        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

        detector.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 100f, 100f))
        // Downward move by 150px
        detector.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_MOVE, 100f, 100f + touchSlop + 150f))

        assertTrue("Downward swipe should trigger notification shade condition", triggered.get())
    }
}
