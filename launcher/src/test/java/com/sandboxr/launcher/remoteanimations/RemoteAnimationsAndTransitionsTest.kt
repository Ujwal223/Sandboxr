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

package com.sandboxr.launcher.remoteanimations

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.os.Binder
import android.os.IBinder
import android.view.Surface
import android.view.View
import android.window.IRemoteTransition
import android.window.IRemoteTransitionFinishedCallback
import android.window.TransitionInfo
import android.window.WindowAnimationState
import android.window.WindowContainerTransaction
import com.sandboxr.launcher.remotetransitions.ActivityToRecentsTransition
import com.sandboxr.launcher.remotetransitions.AnimationResult
import com.sandboxr.launcher.remotetransitions.IRemoteTransitionEx.toWeakRef
import com.sandboxr.launcher.remotetransitions.LauncherTransition
import com.sandboxr.launcher.remotetransitions.RecentsToActivityTransition
import com.sandboxr.launcher.remotetransitions.RemoteAnimationRunner
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RemoteAnimationsAndTransitionsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun testAppLaunchAnimationRunner_progressInterpolation() {
        val iconBounds = RectF(100f, 200f, 200f, 300f) // 100x100
        val windowBounds = Rect(0, 0, 1080, 2400)
        val iconView = View(context)
        val windowView = View(context)

        val runner = AppLaunchAnimationRunner(
            iconBounds = iconBounds,
            windowTargetBounds = windowBounds,
            iconView = iconView,
            windowView = windowView,
            durationMs = 400L
        )

        assertEquals(0f, runner.currentProgress, 0.001f)
        assertEquals(25f, runner.startCornerRadius, 0.001f) // 100 / 4
        assertEquals(28f, runner.endCornerRadius, 0.001f)

        // Test at 0%
        runner.updateProgress(0f)
        assertEquals(100f, runner.currentBounds.left, 0.01f)
        assertEquals(200f, runner.currentBounds.top, 0.01f)
        assertEquals(200f, runner.currentBounds.right, 0.01f)
        assertEquals(300f, runner.currentBounds.bottom, 0.01f)
        assertEquals(25f, runner.currentCornerRadius, 0.01f)
        assertEquals(1f, iconView.alpha, 0.01f)
        assertEquals(0f, windowView.alpha, 0.01f)

        // Test at 50%
        runner.updateProgress(0.5f)
        assertEquals(50f, runner.currentBounds.left, 0.01f)
        assertEquals(100f, runner.currentBounds.top, 0.01f)
        assertEquals(640f, runner.currentBounds.right, 0.01f)
        assertEquals(1350f, runner.currentBounds.bottom, 0.01f)
        assertEquals(26.5f, runner.currentCornerRadius, 0.01f)
        assertEquals(0.25f, iconView.alpha, 0.01f) // 1 - 0.5 * 1.5 = 0.25
        assertEquals(0.5f, windowView.alpha, 0.01f)

        // Test at 100%
        runner.updateProgress(1f)
        assertEquals(0f, runner.currentBounds.left, 0.01f)
        assertEquals(0f, runner.currentBounds.top, 0.01f)
        assertEquals(1080f, runner.currentBounds.right, 0.01f)
        assertEquals(2400f, runner.currentBounds.bottom, 0.01f)
        assertEquals(28f, runner.currentCornerRadius, 0.01f)
        assertEquals(0f, iconView.alpha, 0.01f)
        assertEquals(1f, windowView.alpha, 0.01f)
        assertEquals(1f, windowView.scaleX, 0.01f)
        assertEquals(1f, windowView.scaleY, 0.01f)
    }

    @Test
    fun testAppLaunchAnimationRunner_animatorExecution() {
        val iconBounds = RectF(100f, 100f, 200f, 200f)
        val windowBounds = Rect(0, 0, 1000, 2000)
        val runner = AppLaunchAnimationRunner(iconBounds, windowBounds, durationMs = 200L)

        val finished = AtomicBoolean(false)
        val animator = runner.createAnimator {
            finished.set(true)
        }
        assertNotNull(animator)

        animator.start()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue(finished.get())
        assertEquals(1f, runner.currentProgress, 0.001f)
    }

    @Test
    fun testAppCloseAnimationRunner_progressInterpolation() {
        val windowStart = Rect(0, 0, 1080, 2400)
        val iconTarget = RectF(200f, 400f, 300f, 500f) // 100x100
        val windowView = View(context)
        val iconView = View(context)

        val runner = AppCloseAnimationRunner(
            windowStartBounds = windowStart,
            iconTargetBounds = iconTarget,
            windowView = windowView,
            iconView = iconView,
            durationMs = 350L
        )

        assertEquals(28f, runner.startCornerRadius, 0.001f)
        assertEquals(25f, runner.endCornerRadius, 0.001f) // 100 / 4

        // 0%
        runner.updateProgress(0f)
        assertEquals(0f, runner.currentBounds.left, 0.01f)
        assertEquals(0f, runner.currentBounds.top, 0.01f)
        assertEquals(1080f, runner.currentBounds.right, 0.01f)
        assertEquals(2400f, runner.currentBounds.bottom, 0.01f)
        assertEquals(28f, runner.currentCornerRadius, 0.01f)
        assertEquals(1f, windowView.alpha, 0.01f)
        assertEquals(0f, iconView.alpha, 0.01f)

        // 50%
        runner.updateProgress(0.5f)
        assertEquals(100f, runner.currentBounds.left, 0.01f)
        assertEquals(200f, runner.currentBounds.top, 0.01f)
        assertEquals(690f, runner.currentBounds.right, 0.01f)
        assertEquals(1450f, runner.currentBounds.bottom, 0.01f)
        assertEquals(26.5f, runner.currentCornerRadius, 0.01f)
        assertEquals(0.5f, windowView.alpha, 0.01f)
        assertEquals(0.5f, iconView.alpha, 0.01f)

        // 100%
        runner.updateProgress(1f)
        assertEquals(200f, runner.currentBounds.left, 0.01f)
        assertEquals(400f, runner.currentBounds.top, 0.01f)
        assertEquals(300f, runner.currentBounds.right, 0.01f)
        assertEquals(500f, runner.currentBounds.bottom, 0.01f)
        assertEquals(25f, runner.currentCornerRadius, 0.01f)
        assertEquals(0f, windowView.alpha, 0.01f)
        assertEquals(1f, iconView.alpha, 0.01f)
    }

    @Test
    fun testAppCloseAnimationRunner_animatorExecution() {
        val windowStart = Rect(0, 0, 1000, 2000)
        val iconTarget = RectF(100f, 100f, 200f, 200f)
        val runner = AppCloseAnimationRunner(windowStart, iconTarget, durationMs = 150L)

        val finished = AtomicBoolean(false)
        val animator = runner.createAnimator {
            finished.set(true)
        }
        animator.start()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue(finished.get())
        assertEquals(1f, runner.currentProgress, 0.001f)
    }

    @Test
    fun testAnimOpenProperties_calculations() {
        val windowBounds = Rect(0, 0, 1000, 2000)
        val iconBounds = RectF(100f, 200f, 300f, 400f) // 200x200, center = (200, 300)

        val props = AnimOpenProperties(
            windowTargetBounds = windowBounds,
            launcherIconBounds = iconBounds,
            dragLayerLeft = 0,
            dragLayerTop = 0,
            hasSplashScreen = false,
            hasDifferentAppIcon = false
        )

        assertEquals(500, props.cropCenterXStart)
        assertEquals(1000, props.cropCenterYStart)
        assertEquals(200, props.cropWidthStart)
        assertEquals(200, props.cropHeightStart)
        assertEquals(1000, props.cropWidthEnd)
        assertEquals(2000, props.cropHeightEnd)

        // dX: window centerX (500) - icon centerX (200) = 300
        assertEquals(300f, props.dX, 0.01f)
        // dY: window centerY (1000) - icon centerY (300) = 700
        assertEquals(700f, props.dY, 0.01f)

        // finalAppIconScale: smallestSize = 1000, icon size = 200 => 1000/200 = 5.0
        assertEquals(5f, props.finalAppIconScale, 0.01f)
        assertEquals(1f, props.iconAlphaStart, 0.01f)

        // Splash screen test
        val splashProps = AnimOpenProperties(
            windowTargetBounds = windowBounds,
            launcherIconBounds = iconBounds,
            hasSplashScreen = true,
            hasDifferentAppIcon = false
        )
        assertEquals(0f, splashProps.iconAlphaStart, 0.01f)
    }

    @Test
    fun testStartingWindowListener() {
        val listener = StartingWindowListener()
        assertEquals(Color.TRANSPARENT, listener.backgroundColor)
        assertFalse(listener.hasSplashScreen)
        assertFalse(listener.hasDifferentAppIcon)

        listener.onStartingWindowDrawn(Color.RED, splashScreen = true, differentAppIcon = true)
        assertEquals(Color.RED, listener.backgroundColor)
        assertTrue(listener.hasSplashScreen)
        assertTrue(listener.hasDifferentAppIcon)

        listener.reset()
        assertEquals(Color.TRANSPARENT, listener.backgroundColor)
        assertFalse(listener.hasSplashScreen)
        assertFalse(listener.hasDifferentAppIcon)
    }

    @Test
    fun testRemoteAnimationCoordinateTransfer() {
        val transfer = RemoteAnimationCoordinateTransfer(
            displayWidth = 1000,
            displayHeight = 2000,
            displayRotation = Surface.ROTATION_0
        )

        val srcRect = RectF(100f, 200f, 300f, 500f)
        val resultRect = RectF()

        // Transfer with same rotation (ROTATION_0) -> identity
        transfer.transferRectToLauncher(srcRect, Surface.ROTATION_0, resultRect)
        assertEquals(srcRect, resultRect)

        // Transfer with ROTATION_90
        transfer.transferRectToLauncher(srcRect, Surface.ROTATION_90, resultRect)
        // ROTATION_90 delta = (0 - 1 + 4) % 4 = 3 (ROTATION_270 bounds transform)
        // bounds.left = parentHeight - origBottom = 2000 - 500 = 1500
        // bounds.top = origLeft = 100
        // bounds.right = parentHeight - origTop = 2000 - 200 = 1800
        // bounds.bottom = origRight = 300
        assertEquals(1500f, resultRect.left, 0.01f)
        assertEquals(100f, resultRect.top, 0.01f)
        assertEquals(1800f, resultRect.right, 0.01f)
        assertEquals(300f, resultRect.bottom, 0.01f)

        // Test transferRectToTarget with ROTATION_0
        transfer.transferRectToTarget(srcRect, Surface.ROTATION_0, resultRect)
        assertEquals(srcRect, resultRect)
    }

    @Test
    fun testSpringAnimRunner() {
        val start = RectF(100f, 100f, 200f, 200f)
        val target = Rect(0, 0, 1000, 2000)
        val runner = SpringAnimRunner(start, target)

        var lastProgress = 0f
        val animator = runner.createAnimator(durationMs = 200L) { progress ->
            lastProgress = progress
        }
        assertNotNull(animator)

        animator.start()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue(runner.currentProgress > 0.9f)
        assertTrue(lastProgress > 0.9f)
    }

    @Test
    fun testLauncherTransition_lifecycle() {
        val started = AtomicBoolean(false)
        val merged = AtomicBoolean(false)
        val cancelled = AtomicBoolean(false)
        val finished = AtomicBoolean(false)

        val runner = object : RemoteAnimationRunner {
            override fun onAnimationStart(
                transition: IBinder?,
                info: TransitionInfo?,
                transaction: android.view.SurfaceControl.Transaction?,
                onFinish: () -> Unit
            ) {
                started.set(true)
                onFinish()
            }

            override fun onAnimationMerged(
                transition: IBinder?,
                info: TransitionInfo?,
                transaction: android.view.SurfaceControl.Transaction?,
                mergeTarget: IBinder?
            ) {
                merged.set(true)
            }

            override fun onAnimationCancelled() {
                cancelled.set(true)
            }
        }

        val launcherTransition = LauncherTransition(runner)

        val token = Binder()
        val info = TransitionInfo(1, 0)
        val finishCallback = object : IRemoteTransitionFinishedCallback.Stub() {
            override fun onTransitionFinished(
                wct: WindowContainerTransaction?,
                sct: android.view.SurfaceControl.Transaction?
            ) {
                finished.set(true)
            }
        }

        // Test startAnimation
        launcherTransition.startAnimation(token, info, null, finishCallback)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertTrue(started.get())
        assertTrue(finished.get())

        // Test mergeAnimation
        val mergeFinished = AtomicBoolean(false)
        val mergeFinishCallback = object : IRemoteTransitionFinishedCallback.Stub() {
            override fun onTransitionFinished(
                wct: WindowContainerTransaction?,
                sct: android.view.SurfaceControl.Transaction?
            ) {
                mergeFinished.set(true)
            }
        }
        launcherTransition.mergeAnimation(token, info, null, token, mergeFinishCallback)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertTrue(merged.get())
        assertTrue(mergeFinished.get())

        // Test onTransitionConsumed
        launcherTransition.onTransitionConsumed(token, false)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertTrue(cancelled.get())
    }

    @Test
    fun testIRemoteTransitionEx_weakRef() {
        val started = AtomicBoolean(false)
        val finished = AtomicBoolean(false)

        val remoteTransition = object : IRemoteTransition.Stub() {
            override fun startAnimation(
                token: IBinder?,
                info: TransitionInfo?,
                t: android.view.SurfaceControl.Transaction?,
                finishCallback: IRemoteTransitionFinishedCallback?
            ) {
                started.set(true)
                finishCallback?.onTransitionFinished(null, null)
            }

            override fun mergeAnimation(
                token: IBinder?,
                info: TransitionInfo?,
                t: android.view.SurfaceControl.Transaction?,
                mergeTarget: IBinder?,
                finishCallback: IRemoteTransitionFinishedCallback?
            ) {}

            override fun onTransitionConsumed(token: IBinder?, aborted: Boolean) {}

            override fun takeOverAnimation(
                token: IBinder?,
                info: TransitionInfo?,
                t: android.view.SurfaceControl.Transaction?,
                finishCallback: IRemoteTransitionFinishedCallback?,
                states: Array<out WindowAnimationState>?
            ) {}
        }

        val weakRef = remoteTransition.toWeakRef()
        assertNotNull(weakRef)

        val callback = object : IRemoteTransitionFinishedCallback.Stub() {
            override fun onTransitionFinished(
                wct: WindowContainerTransaction?,
                sct: android.view.SurfaceControl.Transaction?
            ) {
                finished.set(true)
            }
        }

        weakRef.startAnimation(Binder(), TransitionInfo(), null, callback)
        assertTrue(started.get())
        assertTrue(finished.get())
    }

    @Test
    fun testRecentsToActivityTransition() {
        val taskStart = RectF(200f, 400f, 600f, 1200f) // 400x800
        val windowTarget = Rect(0, 0, 1080, 2400)
        val windowView = View(context)
        val recentsView = View(context)
        val taskView = View(context)

        val transition = RecentsToActivityTransition(
            taskStartBounds = taskStart,
            windowTargetBounds = windowTarget,
            windowView = windowView,
            recentsView = recentsView,
            taskView = taskView,
            durationMs = 300L
        )

        assertEquals(24f, transition.startCornerRadius, 0.001f)
        assertEquals(0f, transition.endCornerRadius, 0.001f)

        // 0%
        transition.updateProgress(0f)
        assertEquals(200f, transition.currentBounds.left, 0.01f)
        assertEquals(400f, transition.currentBounds.top, 0.01f)
        assertEquals(600f, transition.currentBounds.right, 0.01f)
        assertEquals(1200f, transition.currentBounds.bottom, 0.01f)
        assertEquals(24f, transition.currentCornerRadius, 0.01f)
        assertEquals(0f, windowView.alpha, 0.01f)
        assertEquals(1f, recentsView.alpha, 0.01f)
        assertEquals(1f, taskView.alpha, 0.01f)

        // 100%
        transition.updateProgress(1f)
        assertEquals(0f, transition.currentBounds.left, 0.01f)
        assertEquals(0f, transition.currentBounds.top, 0.01f)
        assertEquals(1080f, transition.currentBounds.right, 0.01f)
        assertEquals(2400f, transition.currentBounds.bottom, 0.01f)
        assertEquals(0f, transition.currentCornerRadius, 0.01f)
        assertEquals(1f, windowView.alpha, 0.01f)
        assertEquals(0f, recentsView.alpha, 0.01f)
        assertEquals(0f, taskView.alpha, 0.01f)

        // Start animation lifecycle
        val finished = AtomicBoolean(false)
        transition.onAnimationStart(null, null, null) {
            finished.set(true)
        }
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertTrue(finished.get())

        // Test cancel
        val anim = transition.createAnimator()
        anim.start()
        transition.onAnimationCancelled()
        assertFalse(anim.isRunning)
    }

    @Test
    fun testActivityToRecentsTransition() {
        val windowStart = Rect(0, 0, 1080, 2400)
        val taskTarget = RectF(200f, 400f, 600f, 1200f) // 400x800
        val windowView = View(context)
        val recentsView = View(context)
        val taskView = View(context)

        val transition = ActivityToRecentsTransition(
            windowStartBounds = windowStart,
            taskTargetBounds = taskTarget,
            windowView = windowView,
            recentsView = recentsView,
            taskView = taskView,
            durationMs = 300L
        )

        assertEquals(0f, transition.startCornerRadius, 0.001f)
        assertEquals(24f, transition.endCornerRadius, 0.001f)

        // 0%
        transition.updateProgress(0f)
        assertEquals(0f, transition.currentBounds.left, 0.01f)
        assertEquals(0f, transition.currentBounds.top, 0.01f)
        assertEquals(1080f, transition.currentBounds.right, 0.01f)
        assertEquals(2400f, transition.currentBounds.bottom, 0.01f)
        assertEquals(0f, transition.currentCornerRadius, 0.01f)
        assertEquals(1f, windowView.alpha, 0.01f)
        assertEquals(0f, recentsView.alpha, 0.01f)
        assertEquals(0f, taskView.alpha, 0.01f)

        // 100%
        transition.updateProgress(1f)
        assertEquals(200f, transition.currentBounds.left, 0.01f)
        assertEquals(400f, transition.currentBounds.top, 0.01f)
        assertEquals(600f, transition.currentBounds.right, 0.01f)
        assertEquals(1200f, transition.currentBounds.bottom, 0.01f)
        assertEquals(24f, transition.currentCornerRadius, 0.01f)
        assertEquals(0f, windowView.alpha, 0.01f)
        assertEquals(1f, recentsView.alpha, 0.01f)
        assertEquals(1f, taskView.alpha, 0.01f)

        // Start animation lifecycle
        val finished = AtomicBoolean(false)
        transition.onAnimationStart(null, null, null) {
            finished.set(true)
        }
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertTrue(finished.get())

        // Test cancel
        val anim = transition.createAnimator()
        anim.start()
        transition.onAnimationCancelled()
        assertFalse(anim.isRunning)
    }

    @Test
    fun testAnimationResult_lifecycle() {
        val syncRan = AtomicBoolean(false)
        val asyncRan = AtomicBoolean(false)
        val completeRan = AtomicBoolean(false)

        val directExecutor = java.util.concurrent.Executor { it.run() }
        val animResult = AnimationResult(
            syncFinishRunnable = { syncRan.set(true) },
            asyncFinishRunnable = { asyncRan.set(true) },
            bgExecutor = directExecutor,
            mainExecutor = directExecutor
        )

        val animSet = android.animation.AnimatorSet().apply {
            play(android.animation.ValueAnimator.ofFloat(0f, 1f).setDuration(50L))
        }

        animResult.setAnimation(
            animation = animSet,
            context = context,
            onCompleteCallback = { completeRan.set(true) }
        )

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue(syncRan.get())
        assertTrue(asyncRan.get())
        assertTrue(completeRan.get())
    }
}

