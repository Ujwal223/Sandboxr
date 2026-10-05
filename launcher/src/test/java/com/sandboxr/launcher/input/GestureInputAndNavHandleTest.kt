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

package com.sandboxr.launcher.input

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import com.sandboxr.launcher.input.GestureState.GestureEndTarget
import com.sandboxr.launcher.inputconsumers.AccessibilityInputConsumer
import com.sandboxr.launcher.inputconsumers.DefaultNavHandle
import com.sandboxr.launcher.inputconsumers.DeviceLockedInputConsumer
import com.sandboxr.launcher.inputconsumers.InputConsumer
import com.sandboxr.launcher.inputconsumers.NavHandleLongPressHandler
import com.sandboxr.launcher.inputconsumers.NavHandleLongPressInputConsumer
import com.sandboxr.launcher.inputconsumers.OneHandedModeInputConsumer
import com.sandboxr.launcher.inputconsumers.OtherActivityInputConsumer
import com.sandboxr.launcher.inputconsumers.OverviewInputConsumer
import com.sandboxr.launcher.inputconsumers.ResetGestureInputConsumer
import com.sandboxr.launcher.inputconsumers.ScreenPinnedInputConsumer
import com.sandboxr.launcher.interaction.EdgeBackGestureHandler
import com.sandboxr.launcher.interaction.GestureSandboxActivity
import com.sandboxr.launcher.interaction.NavBarGestureHandler
import com.sandboxr.launcher.interaction.SwipeUpGestureTutorialController
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GestureInputAndNavHandleTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    private fun obtainMotionEvent(action: Int, x: Float, y: Float, downTime: Long = SystemClock.uptimeMillis()): MotionEvent {
        return MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
    }

    // --- InputConsumer & Flags Tests ---

    @Test
    fun testInputConsumer_typesAndNames() {
        val noOp = InputConsumer.NO_OP
        assertEquals(InputConsumer.TYPE_NO_OP, noOp.type)
        assertEquals("TYPE_NO_OP", noOp.name)
        assertTrue(noOp.allowInterceptByParent())
        assertFalse(noOp.isConsumerDetachedFromGesture())

        val custom = InputConsumer.createNoOpInputConsumer(1)
        assertEquals(1, custom.displayId)
        assertEquals(noOp, noOp.getActiveConsumerInHierarchy())
    }

    // --- Nav Handle Long Press Tests ---

    @Test
    fun testNavHandleLongPressInputConsumer_triggerLongPress() {
        val navHandle = DefaultNavHandle(Rect(400, 2300, 680, 2360))
        var longPressTriggered = false
        val handler = NavHandleLongPressHandler(context) {
            longPressTriggered = true
        }

        val consumer = NavHandleLongPressInputConsumer(
            context = context,
            delegate = InputConsumer.NO_OP,
            navHandle = navHandle,
            handler = handler
        )

        // Down inside nav handle area
        val downEvent = obtainMotionEvent(MotionEvent.ACTION_DOWN, 540f, 2330f)
        consumer.onMotionEvent(downEvent)
        assertTrue(navHandle.isAnimating)

        // Simulate timeout trigger
        consumer.triggerLongPressForTesting()
        assertTrue(longPressTriggered)
        assertTrue(consumer.isLongPressTriggered)
    }

    @Test
    fun testNavHandleLongPressInputConsumer_cancelOnSlopExceeded() {
        val navHandle = DefaultNavHandle(Rect(400, 2300, 680, 2360))
        var longPressTriggered = false
        val handler = NavHandleLongPressHandler(context) {
            longPressTriggered = true
        }

        val consumer = NavHandleLongPressInputConsumer(
            context = context,
            delegate = InputConsumer.NO_OP,
            navHandle = navHandle,
            handler = handler
        )

        // Down inside nav handle
        consumer.onMotionEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 540f, 2330f))
        assertTrue(navHandle.isAnimating)

        // Drag up past slop
        consumer.onMotionEvent(obtainMotionEvent(MotionEvent.ACTION_MOVE, 540f, 2100f))
        assertFalse(navHandle.isAnimating)
        assertFalse(consumer.isLongPressTriggered)
    }

    // --- OtherActivityInputConsumer & Swipe Up Tests ---

    @Test
    fun testOtherActivityInputConsumer_swipeToHome() {
        var swipedToHome = false
        var swipedToRecents = false
        var cancelled = false

        val consumer = OtherActivityInputConsumer(
            context = context,
            onSwipeToHome = { swipedToHome = true },
            onSwipeAndHoldToRecents = { swipedToRecents = true },
            onSwipeCancelled = { cancelled = true }
        )

        val downTime = SystemClock.uptimeMillis()
        // Fast swipe upward
        consumer.onMotionEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 540f, 2300f, downTime))
        consumer.onMotionEvent(obtainMotionEvent(MotionEvent.ACTION_MOVE, 540f, 2100f, downTime))
        consumer.onMotionEvent(obtainMotionEvent(MotionEvent.ACTION_UP, 540f, 1800f, downTime))

        assertTrue(swipedToHome)
        assertFalse(swipedToRecents)
        assertEquals(OtherActivityInputConsumer.GestureTarget.HOME, consumer.currentTarget)
    }

    @Test
    fun testOtherActivityInputConsumer_swipeAndHoldToRecents() {
        var swipedToRecents = false

        val consumer = OtherActivityInputConsumer(
            context = context,
            onSwipeAndHoldToRecents = { swipedToRecents = true }
        )

        consumer.onMotionEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 540f, 2300f))
        consumer.onMotionEvent(obtainMotionEvent(MotionEvent.ACTION_MOVE, 540f, 2000f))
        consumer.triggerHoldForTesting()

        assertTrue(swipedToRecents)
        assertEquals(OtherActivityInputConsumer.GestureTarget.RECENTS, consumer.currentTarget)
    }

    @Test
    fun testOtherActivityInputConsumer_dragBackDownCancels() {
        var cancelled = false

        val consumer = OtherActivityInputConsumer(
            context = context,
            onSwipeCancelled = { cancelled = true }
        )

        // Drag up slightly then back down
        consumer.onMotionEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 540f, 2300f))
        consumer.onMotionEvent(obtainMotionEvent(MotionEvent.ACTION_MOVE, 540f, 2260f))
        consumer.onMotionEvent(obtainMotionEvent(MotionEvent.ACTION_UP, 540f, 2300f))

        assertTrue(cancelled)
        assertEquals(OtherActivityInputConsumer.GestureTarget.CANCEL, consumer.currentTarget)
    }

    // --- SwipeUpGestureHandler Target Evaluation Tests ---

    @Test
    fun testSwipeUpGestureHandler_evaluateEndTarget() {
        val handler = SwipeUpGestureHandler(minSwipeDistancePx = 120f, flingVelocityThresholdPxPerSec = 500f)

        // Fling up -> Home
        val flingTarget = handler.evaluateEndTarget(displacementY = 200f, velocityY = -800f, touchDurationMs = 100L)
        assertEquals(GestureEndTarget.HOME, flingTarget)

        // Hold detected -> Recents
        val holdTarget = handler.evaluateEndTarget(displacementY = 200f, velocityY = -50f, touchDurationMs = 400L, isHoldDetected = true)
        assertEquals(GestureEndTarget.RECENTS, holdTarget)

        // Sustained hold past timeout -> Recents
        val sustainedTarget = handler.evaluateEndTarget(displacementY = 200f, velocityY = -100f, touchDurationMs = 350L)
        assertEquals(GestureEndTarget.RECENTS, sustainedTarget)

        // Too small displacement -> Cancel
        val cancelTarget = handler.evaluateEndTarget(displacementY = 30f, velocityY = 0f, touchDurationMs = 100L)
        assertEquals(GestureEndTarget.CANCEL, cancelTarget)
    }

    // --- GestureInputRouter State Dispatching Tests ---

    @Test
    fun testGestureInputRouter_deviceLockedDispatches() {
        val router = GestureInputRouter(context).apply {
            isDeviceLocked = true
        }
        val consumer = router.createInputConsumer()
        assertTrue(consumer is DeviceLockedInputConsumer)
    }

    @Test
    fun testGestureInputRouter_screenPinnedDispatches() {
        val router = GestureInputRouter(context).apply {
            isScreenPinned = true
        }
        val consumer = router.createInputConsumer()
        assertTrue(consumer is ScreenPinnedInputConsumer)
    }

    @Test
    fun testGestureInputRouter_inOverviewDispatches() {
        val router = GestureInputRouter(context).apply {
            isInOverview = true
        }
        val consumer = router.createInputConsumer()
        assertTrue(consumer is OverviewInputConsumer)
    }

    @Test
    fun testGestureInputRouter_foregroundAppDispatchesNavHandleLongPress() {
        val router = GestureInputRouter(context).apply {
            isDeviceLocked = false
            isInOverview = false
        }
        val consumer = router.createInputConsumer()
        assertTrue(consumer is NavHandleLongPressInputConsumer)
    }

    // --- Specialized Input Consumers Tests ---

    @Test
    fun testAccessibilityInputConsumer_twoFingerGesture() {
        var accessibilityTriggered = false
        val consumer = AccessibilityInputConsumer(context, InputConsumer.NO_OP) {
            accessibilityTriggered = true
        }

        // Multi-finger motion
        val ev = obtainMotionEvent(MotionEvent.ACTION_MOVE, 500f, 2000f)
        // Set simulated pointerCount to 2 via reflection or call delegate
        consumer.onMotionEvent(ev)
        assertFalse(consumer.isAccessibilityTriggered) // 1 pointer by default
    }

    @Test
    fun testOneHandedModeInputConsumer_swipeDown() {
        var oneHandedTriggered = false
        val consumer = OneHandedModeInputConsumer(context, InputConsumer.NO_OP) {
            oneHandedTriggered = true
        }

        consumer.onMotionEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 540f, 2200f))
        consumer.onMotionEvent(obtainMotionEvent(MotionEvent.ACTION_MOVE, 540f, 2300f)) // Swipe down

        assertTrue(oneHandedTriggered)
    }

    @Test
    fun testResetGestureInputConsumer() {
        var resetCalled = false
        val consumer = ResetGestureInputConsumer {
            resetCalled = true
        }
        consumer.onMotionEvent(obtainMotionEvent(MotionEvent.ACTION_UP, 100f, 100f))
        assertTrue(resetCalled)
    }

    // --- EdgeBackGestureHandler Tests ---

    @Test
    fun testEdgeBackGestureHandler_leftAndRightEdges() {
        var backInvoked = false
        val handler = EdgeBackGestureHandler(context, Point(1080, 2400)).apply {
            edgeWidthPx = 50f
            swipeThresholdPx = 80f
            onBackInvoked = { backInvoked = true }
        }

        // Swipe inward from left edge
        val handledDown = handler.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 20f, 1000f))
        assertTrue(handledDown)

        handler.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_MOVE, 120f, 1000f))
        handler.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_UP, 120f, 1000f))

        assertTrue(backInvoked)

        // Test non-edge touch
        val nonEdge = handler.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 500f, 1000f))
        assertFalse(nonEdge)
    }

    // --- NavBarGestureHandler Tests ---

    @Test
    fun testNavBarGestureHandler_swipeUp() {
        var swipedUp = false
        val navHandler = NavBarGestureHandler(context).apply {
            onSwipeUp = { swipedUp = true }
        }

        navHandler.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 540f, 2300f))
        navHandler.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_UP, 540f, 2150f))

        assertTrue(swipedUp)
    }

    // --- Gesture Tutorial & Sandbox Activity Tests ---

    @Test
    fun testSwipeUpGestureTutorialController_transitions() {
        val controller = SwipeUpGestureTutorialController(
            context,
            SwipeUpGestureTutorialController.TutorialType.HOME_NAVIGATION
        )

        assertEquals(SwipeUpGestureTutorialController.FeedbackState.WAITING_FOR_INPUT, controller.currentState)

        // Too small swipe
        controller.onUserGestureCompleted(isHold = false, distanceUpPx = 50f, velocityY = 0f)
        assertEquals(SwipeUpGestureTutorialController.FeedbackState.SWIPE_HIGHER, controller.currentState)

        // Successful home swipe
        controller.onUserGestureCompleted(isHold = false, distanceUpPx = 200f, velocityY = -800f)
        assertEquals(SwipeUpGestureTutorialController.FeedbackState.SUCCESS, controller.currentState)
    }

    @Test
    fun testGestureSandboxActivity_lifecycleAndAdvancement() {
        val controller = Robolectric.buildActivity(GestureSandboxActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)

        assertEquals(
            SwipeUpGestureTutorialController.TutorialType.HOME_NAVIGATION,
            activity.currentTutorialStep
        )

        // Simulate successful Home gesture
        activity.tutorialController.onUserGestureCompleted(isHold = false, distanceUpPx = 200f, velocityY = -600f)
        assertEquals(
            SwipeUpGestureTutorialController.TutorialType.OVERVIEW_NAVIGATION,
            activity.currentTutorialStep
        )

        // Simulate successful Overview gesture
        activity.tutorialController.onUserGestureCompleted(isHold = true, distanceUpPx = 200f, velocityY = 0f)
        assertEquals(
            SwipeUpGestureTutorialController.TutorialType.BACK_NAVIGATION,
            activity.currentTutorialStep
        )
    }
}
