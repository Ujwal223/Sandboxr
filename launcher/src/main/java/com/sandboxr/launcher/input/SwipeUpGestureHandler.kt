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

import com.sandboxr.launcher.input.GestureState.GestureEndTarget
import kotlin.math.abs

/**
 * Helper class that evaluates touch displacement, velocity, and duration to determine
 * whether a gesture targets HOME, RECENTS, or CANCEL.
 */
class SwipeUpGestureHandler(
    val minSwipeDistancePx: Float = 120f,
    val flingVelocityThresholdPxPerSec: Float = 500f,
    val minHoldDurationMs: Long = 250L
) {

    /**
     * Evaluates a gesture upon touch release (UP event).
     *
     * @param displacementY Net upward movement (positive means upward).
     * @param velocityY Velocity along the Y-axis (negative means upward fling in Android screen coords).
     * @param touchDurationMs Total duration the finger was down.
     * @param isHoldDetected Whether a stationary hold was registered prior to release.
     */
    fun evaluateEndTarget(
        displacementY: Float,
        velocityY: Float,
        touchDurationMs: Long,
        isHoldDetected: Boolean = false
    ): GestureEndTarget {
        if (isHoldDetected && displacementY >= minSwipeDistancePx) {
            return GestureEndTarget.RECENTS
        }

        val isUpwardFling = velocityY < -flingVelocityThresholdPxPerSec
        val isSufficientDistance = displacementY >= minSwipeDistancePx

        return when {
            // Rapid upward fling -> Home
            isUpwardFling -> GestureEndTarget.HOME
            // Sustained hold past threshold -> Recents
            touchDurationMs >= minHoldDurationMs && isSufficientDistance && abs(velocityY) < flingVelocityThresholdPxPerSec -> {
                GestureEndTarget.RECENTS
            }
            // Standard upward swipe without hold -> Home
            isSufficientDistance -> GestureEndTarget.HOME
            // Failed distance and velocity requirements -> Cancel
            else -> GestureEndTarget.CANCEL
        }
    }
}
