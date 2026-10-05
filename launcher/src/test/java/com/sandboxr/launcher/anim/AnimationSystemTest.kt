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

package com.sandboxr.launcher.anim

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.dynamicanimation.animation.SpringForce
import com.sandboxr.launcher.InterruptibleInOutAnimator
import com.sandboxr.launcher.LauncherAnimUtils
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnimationSystemTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun testSpringAnimationBuilderParamsAndInterpolation() {
        val builder = SpringAnimationBuilder(context)
            .setStartValue(0f)
            .setEndValue(100f)
            .setStiffness(SpringForce.STIFFNESS_LOW)
            .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
            .setMinimumVisibleChange(1f)
            .setStartVelocity(50f)
            .computeParams()

        val duration = builder.getDuration()
        assertTrue("Spring duration should be positive", duration > 0)

        val valAtStart = builder.getInterpolatedValue(0f)
        assertEquals(0f, valAtStart, 1.0f)

        val valAtEnd = builder.getInterpolatedValue(1f)
        assertEquals(100f, valAtEnd, 1.0f)

        val testView = View(context)
        val animator = builder.build(testView, LauncherAnimUtils.VIEW_TRANSLATE_X)
        assertNotNull(animator)
        assertEquals(duration, animator.duration)
    }

    @Test
    fun testAnimatorPlaybackControllerScrubbingAndHolders() {
        val testView = View(context)
        val anim1 = ObjectAnimator.ofFloat(testView, View.ALPHA, 0f, 1f).setDuration(400)
        val anim2 = ObjectAnimator.ofFloat(testView, View.TRANSLATION_Y, 0f, 200f).setDuration(400)

        val animSet = AnimatorSet()
        animSet.playTogether(anim1, anim2)
        val controller = AnimatorPlaybackController.wrap(animSet, 400)

        assertEquals(400L, controller.duration)
        assertEquals(0f, controller.progressFraction, 0.001f)

        controller.setPlayFraction(0.5f)
        assertEquals(0.5f, controller.progressFraction, 0.001f)
        assertEquals(0.5f, testView.alpha, 0.05f)
        assertEquals(100f, testView.translationY, 5.0f)

        controller.setPlayFraction(1.0f)
        assertEquals(1.0f, controller.progressFraction, 0.001f)
        assertEquals(1.0f, testView.alpha, 0.05f)
        assertEquals(200f, testView.translationY, 5.0f)
    }

    @Test
    fun testAnimatorPlaybackControllerMidFlightInterruptionWithVelocity() {
        val testView = View(context)
        val anim = ObjectAnimator.ofFloat(testView, View.TRANSLATION_Y, 0f, 500f).setDuration(400)
        val pending = PendingAnimation(400)
        pending.add(anim, SpringProperty(SpringProperty.FLAG_CAN_SPRING_ON_END))
        val controller = pending.createPlaybackController()

        // Scrub to 35% mid-flight
        controller.setPlayFraction(0.35f)
        assertEquals(0.35f, controller.progressFraction, 0.001f)

        // Pause / interrupt mid-flight
        controller.pause()

        // Launch with velocity towards end
        controller.startWithVelocity(
            context = context,
            goingToEnd = true,
            velocityPxPerMs = 2.0f,
            endDistance = 500f,
            animationDuration = 300L
        )

        // Player should be running with valid duration
        assertTrue(controller.animationPlayer.duration > 0)
        assertNotNull(controller.animationPlayer.interpolator)
    }

    @Test
    fun testAnimatorPlaybackControllerEndAction() {
        val testView = View(context)
        val anim = ObjectAnimator.ofFloat(testView, View.ALPHA, 0f, 1f).setDuration(50)
        val animSet = AnimatorSet()
        animSet.play(anim)
        val controller = AnimatorPlaybackController.wrap(animSet, 50)

        val endActionCalled = AtomicBoolean(false)
        controller.setEndAction { endActionCalled.set(true) }

        controller.start()
        controller.animationPlayer.end()

        assertTrue("End action should be triggered", endActionCalled.get())
    }

    @Test
    fun testInterruptibleInOutAnimatorMidFlightInterruption() {
        val inOut = InterruptibleInOutAnimator(600L, 0f, 1f)
        assertTrue(inOut.isStopped())

        inOut.animateIn()
        assertEquals(InterruptibleInOutAnimator.IN, inOut.direction)

        // Simulate mid-flight at 250ms
        inOut.animator.currentPlayTime = 250L

        // Reverse to OUT mid-flight
        inOut.animateOut()
        assertEquals(InterruptibleInOutAnimator.OUT, inOut.direction)
        assertTrue("Remaining duration should be less than or equal to original", inOut.animator.duration <= 600L)

        inOut.cancel()
        assertTrue(inOut.isStopped())
    }

    @Test
    fun testInterpolatorsSuite() {
        assertEquals(0f, Interpolators.LINEAR.getInterpolation(0f), 0.001f)
        assertEquals(1f, Interpolators.LINEAR.getInterpolation(1f), 0.001f)

        assertEquals(1f, Interpolators.INSTANT.getInterpolation(0f), 0.001f)
        assertEquals(1f, Interpolators.INSTANT.getInterpolation(0.5f), 0.001f)

        assertEquals(0f, Interpolators.FINAL_FRAME.getInterpolation(0.5f), 0.001f)
        assertEquals(1f, Interpolators.FINAL_FRAME.getInterpolation(1.0f), 0.001f)

        // Clamped interpolator
        val clamped = Interpolators.clampToProgress(Interpolators.LINEAR, 0.2f, 0.8f)
        assertEquals(0f, clamped.getInterpolation(0.1f), 0.001f)
        assertEquals(1f, clamped.getInterpolation(0.9f), 0.001f)
        assertEquals(0.5f, clamped.getInterpolation(0.5f), 0.001f)

        // Velocity-based scroll interpolator
        assertEquals(Interpolators.AGGRESSIVE_EASE, Interpolators.scrollInterpolatorForVelocity(2.0f))
        assertEquals(Interpolators.DECELERATE_1_7, Interpolators.scrollInterpolatorForVelocity(0.8f))

        // Log interpolators
        val logAcc = LogAccelerateInterpolator(100, 0)
        assertEquals(0f, logAcc.getInterpolation(0f), 0.05f)
        assertEquals(1f, logAcc.getInterpolation(1f), 0.001f)

        val logDec = LogDecelerateInterpolator(100, 0)
        assertEquals(0f, logDec.getInterpolation(0f), 0.001f)
        assertEquals(1f, logDec.getInterpolation(1f), 0.001f)
        assertTrue(logDec.getInterpolation(0.3f) > logAcc.getInterpolation(0.3f))
    }

    @Test
    fun testLauncherAnimUtilsPropertiesAndClamping() {
        val testView = View(context)

        LauncherAnimUtils.VIEW_ALPHA.set(testView, 0.75f)
        assertEquals(0.75f, testView.alpha, 0.001f)

        LauncherAnimUtils.VIEW_TRANSLATE_X.set(testView, 42f)
        assertEquals(42f, testView.translationX, 0.001f)

        LauncherAnimUtils.VIEW_TRANSLATE_Y.set(testView, 84f)
        assertEquals(84f, testView.translationY, 0.001f)

        LauncherAnimUtils.VIEW_TRANSLATE_Z.set(testView, 12f)
        assertEquals(12f, testView.translationZ, 0.001f)

        LauncherAnimUtils.VIEW_BACKGROUND_COLOR.set(testView, Color.RED)
        val bg = testView.background as ColorDrawable
        assertEquals(Color.RED, bg.color)

        // Clamped property
        val clampedAlpha = LauncherAnimUtils.ClampedProperty(LauncherAnimUtils.VIEW_ALPHA, 0.2f, 0.8f)
        clampedAlpha.set(testView, 0.0f)
        assertEquals(0.2f, testView.alpha, 0.001f)

        clampedAlpha.set(testView, 1.0f)
        assertEquals(0.8f, testView.alpha, 0.001f)

        // Blocked fling duration factor
        assertEquals(2, LauncherAnimUtils.blockedFlingDurationFactor(1.0f))
        assertEquals(6, LauncherAnimUtils.blockedFlingDurationFactor(20.0f))

        // Cancel listener
        val cancelCount = AtomicInteger(0)
        val listener = LauncherAnimUtils.newSingleUseCancelListener { cancelCount.incrementAndGet() }
        val dummyAnim = ValueAnimator.ofFloat(0f, 1f)
        listener.onAnimationCancel(dummyAnim)
        listener.onAnimationCancel(dummyAnim)
        assertEquals(1, cancelCount.get())

        // Rect provider
        val startRect = RectF(0f, 0f, 100f, 100f)
        val targetRect = RectF(0f, 200f, 100f, 300f)
        val provider = LauncherAnimUtils.getPosProviderForRect(startRect, targetRect)
        assertEquals(250f, provider(targetRect), 0.001f)
    }

    @Test
    fun testRoundedRectRevealOutlineProvider() {
        val startRect = Rect(0, 0, 50, 50)
        val endRect = Rect(0, 0, 200, 100)
        val provider = RoundedRectRevealOutlineProvider(25f, 10f, startRect, endRect)

        assertFalse(provider.shouldRemoveElevationDuringAnimation())

        provider.setProgress(0f)
        assertEquals(25f, provider.radius, 0.001f)
        val outRect = Rect()
        provider.getOutline(outRect)
        assertEquals(startRect, outRect)

        provider.setProgress(1f)
        assertEquals(10f, provider.radius, 0.001f)
        provider.getOutline(outRect)
        assertEquals(endRect, outRect)

        provider.setProgress(0.5f)
        assertEquals(17.5f, provider.radius, 0.001f)
        provider.getOutline(outRect)
        assertEquals(125, outRect.right)
        assertEquals(75, outRect.bottom)
    }

    @Test
    fun testPropertyListBuilderAndPropertyResetListener() {
        val testView = View(context)
        val builder = PropertyListBuilder()
            .translationX(10f)
            .translationY(20f)
            .translationZ(5f)
            .scale(1.5f)
            .alpha(0.8f)

        val animator = builder.build(testView)
        assertNotNull(animator)

        val resetListener = PropertyResetListener(LauncherAnimUtils.VIEW_ALPHA, 1.0f)
        val objectAnim = ObjectAnimator.ofFloat(testView, View.ALPHA, 0.5f)
        testView.alpha = 0.5f
        resetListener.onAnimationEnd(objectAnim)
        assertEquals(1.0f, testView.alpha, 0.001f)
    }
}
