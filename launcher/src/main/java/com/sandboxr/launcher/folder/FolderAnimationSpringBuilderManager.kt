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

package com.sandboxr.launcher.folder

import android.animation.TimeInterpolator
import android.view.animation.Interpolator
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Manages spring physics builders and damped oscillation interpolators
 * tailored for SANDBOXR Liquid Glass folder transitions.
 */
class FolderAnimationSpringBuilderManager {

    companion object {
        const val STIFFNESS_DEFAULT = 600f
        const val DAMPING_RATIO_DEFAULT = 0.82f

        /**
         * Creates a physics-based spring damped interpolator for folder scale and reveal.
         */
        @JvmStatic
        fun createFolderSpringInterpolator(
            dampingRatio: Float = DAMPING_RATIO_DEFAULT,
            frequency: Float = 14f
        ): TimeInterpolator {
            return TimeInterpolator { input ->
                if (input <= 0f) return@TimeInterpolator 0f
                if (input >= 1f) return@TimeInterpolator 1f
                val decay = exp((-dampingRatio * frequency * input).toDouble())
                val oscillation = cos((frequency * input).toDouble())
                (1.0 - decay * oscillation).toFloat()
            }
        }

        /**
         * Smooth deceleration curve for subtle alpha fades.
         */
        @JvmStatic
        val FOLDER_FADE_INTERPOLATOR: TimeInterpolator = TimeInterpolator { input ->
            1f - (1f - input) * (1f - input)
        }
    }
}
