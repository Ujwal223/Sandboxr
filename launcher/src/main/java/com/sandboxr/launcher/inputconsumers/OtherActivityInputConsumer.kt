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

package com.sandboxr.launcher.inputconsumers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import com.sandboxr.launcher.inputconsumers.InputConsumer.Companion.TYPE_OTHER_ACTIVITY
import kotlin.math.abs

/**
 * Input consumer active when another activity is in the foreground.
 * Handles swipe up gestures from the navigation bar area, determining whether the user
 * swiped to HOME or swiped up and held to enter RECENTS.
 */
class OtherActivityInputConsumer(
    val context: Context,
    override val displayId: Int = 0,
    var onSwipeToHome: (() -> Unit)? = null,
    var onSwipeAndHoldToRecents: (() -> Unit)? = null,
    var onSwipeCancelled: (() -> Unit)? = null
) : InputConsumer {

    override val type: Int = TYPE_OTHER_ACTIVITY

    enum class GestureTarget {
        NONE,
        HOME,
        RECENTS,
        CANCEL
    }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val mainHandler = Handler(Looper.getMainLooper())
    private var velocityTracker: VelocityTracker? = null

    private var downX: Float = 0f
    private var downY: Float = 0f
    private var lastY: Float = 0f
    private var isGestureStarted: Boolean = false
    private var isHoldTriggered: Boolean = false

    var currentTarget: GestureTarget = GestureTarget.NONE
        private set

    /** Hold delay before transitioning to overview if finger is stationary */
    var holdTimeoutMs: Long = 300L

    /** Distance in pixels needed to qualify as a swipe up gesture */
    var swipeThresholdPx: Float = 120f

    private val holdCheckRunnable = Runnable {
        if (isGestureStarted && !isHoldTriggered) {
            val displacementY = downY - lastY
            if (displacementY >= swipeThresholdPx) {
                isHoldTriggered = true
                currentTarget = GestureTarget.RECENTS
                onSwipeAndHoldToRecents?.invoke()
            }
        }
    }

    override fun onMotionEvent(ev: MotionEvent) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                lastY = ev.rawY
                isGestureStarted = false
                isHoldTriggered = false
                currentTarget = GestureTarget.NONE
            }

            MotionEvent.ACTION_MOVE -> {
                lastY = ev.rawY
                val deltaY = downY - ev.rawY

                if (!isGestureStarted && deltaY > touchSlop) {
                    isGestureStarted = true
                    // Schedule hold check
                    mainHandler.postDelayed(holdCheckRunnable, holdTimeoutMs)
                }

                if (isGestureStarted && !isHoldTriggered) {
                    if (deltaY < touchSlop / 2f) {
                        // User dragged back down, cancel hold
                        mainHandler.removeCallbacks(holdCheckRunnable)
                    } else {
                        // Reset hold timer if user continues dragging up slowly
                        mainHandler.removeCallbacks(holdCheckRunnable)
                        mainHandler.postDelayed(holdCheckRunnable, holdTimeoutMs)
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                mainHandler.removeCallbacks(holdCheckRunnable)
                velocityTracker?.computeCurrentVelocity(1000)
                val yVelocity = velocityTracker?.yVelocity ?: 0f

                if (isHoldTriggered) {
                    // Already triggered recents
                    currentTarget = GestureTarget.RECENTS
                } else if (isGestureStarted) {
                    val deltaY = downY - ev.rawY
                    if (deltaY >= swipeThresholdPx || yVelocity < -500f) {
                        // Fast or sufficient upward fling -> Go Home
                        currentTarget = GestureTarget.HOME
                        onSwipeToHome?.invoke()
                    } else {
                        // Dragged back down or too small -> Cancel
                        currentTarget = GestureTarget.CANCEL
                        onSwipeCancelled?.invoke()
                    }
                }

                cleanup()
            }

            MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(holdCheckRunnable)
                if (isGestureStarted && !isHoldTriggered) {
                    currentTarget = GestureTarget.CANCEL
                    onSwipeCancelled?.invoke()
                }
                cleanup()
            }
        }
    }

    private fun cleanup() {
        velocityTracker?.recycle()
        velocityTracker = null
        isGestureStarted = false
    }

    /**
     * For unit testing: simulate swipe up and hold directly.
     */
    fun triggerHoldForTesting() {
        isHoldTriggered = true
        currentTarget = GestureTarget.RECENTS
        onSwipeAndHoldToRecents?.invoke()
    }

    override fun allowInterceptByParent(): Boolean = !isGestureStarted
}
