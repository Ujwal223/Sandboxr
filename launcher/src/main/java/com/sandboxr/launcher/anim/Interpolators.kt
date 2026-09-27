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

import android.animation.TimeInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import kotlin.math.abs

/**
 * Common interpolators and curve utilities used across Launcher state animations.
 */
object Interpolators {

    @JvmField
    val LINEAR: Interpolator = LinearInterpolator()

    @JvmField
    val ACCELERATE: Interpolator = AccelerateInterpolator()

    @JvmField
    val ACCELERATE_2: Interpolator = AccelerateInterpolator(2f)

    @JvmField
    val DECELERATE: Interpolator = DecelerateInterpolator()

    @JvmField
    val DECELERATE_1_5: Interpolator = DecelerateInterpolator(1.5f)

    @JvmField
    val DECELERATE_1_7: Interpolator = DecelerateInterpolator(1.7f)

    @JvmField
    val DECELERATE_2: Interpolator = DecelerateInterpolator(2f)

    @JvmField
    val ACCELERATE_DECELERATE: Interpolator = AccelerateDecelerateInterpolator()

    @JvmField
    val FAST_OUT_SLOW_IN: Interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)

    @JvmField
    val SLOW_IN_FAST_OUT: Interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)

    @JvmField
    val AGGRESSIVE_EASE: Interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)

    @JvmField
    val EXAGGERATED_EASE: Interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1.4f)

    @JvmField
    val EMPHASIZED: Interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)

    @JvmField
    val EMPHASIZED_DECELERATE: Interpolator = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)

    @JvmField
    val EMPHASIZED_ACCELERATE: Interpolator = PathInterpolator(0.3f, 0f, 0.8f, 0.15f)

    @JvmField
    val ZOOM_OUT: Interpolator = PathInterpolator(0.1f, 0.1f, 0f, 1f)

    @JvmField
    val OVERSHOOT_1_2: Interpolator = OvershootInterpolator(1.2f)

    @JvmField
    val FINAL_FRAME: Interpolator = Interpolator { input -> if (input >= 1f) 1f else 0f }

    @JvmStatic
    fun clampToProgress(interpolator: TimeInterpolator, lowerBound: Float, upperBound: Float): TimeInterpolator {
        require(upperBound >= lowerBound) { "upperBound ($upperBound) must be >= lowerBound ($lowerBound)" }
        return TimeInterpolator { input ->
            when {
                input <= lowerBound -> 0f
                input >= upperBound -> 1f
                else -> {
                    val progress = (input - lowerBound) / (upperBound - lowerBound)
                    interpolator.getInterpolation(progress)
                }
            }
        }
    }

    @JvmStatic
    fun scrollInterpolatorForVelocity(velocityPxPerMs: Float): Interpolator {
        return if (abs(velocityPxPerMs) > 1.5f) {
            AGGRESSIVE_EASE
        } else {
            DECELERATE_1_7
        }
    }
}
