/*
 * Copyright (C) 2017 The Android Open Source Project
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

/**
 * Utility methods for overscroll damping and resistance curves.
 */
object OverScroll {

    const val OVERSCROLL_DAMP_FACTOR: Float = 0.07f

    /**
     * This curve determines how the effect of scrolling over the limits of the page diminishes
     * as the user pulls further and further from the bounds.
     */
    private fun overScrollInfluenceCurve(f: Float): Float {
        val t = f - 1.0f
        return t * t * t + 1.0f
    }

    /**
     * @param amount The original amount overscrolled.
     * @param max The maximum amount that the View can overscroll.
     * @return The dampened overscroll amount.
     */
    @JvmStatic
    fun dampedScroll(amount: Float, max: Int): Int {
        if (amount == 0f || max == 0) return 0

        var f = amount / max.toFloat()
        f = (f / Math.abs(f)) * overScrollInfluenceCurve(Math.abs(f))

        // Clamp this factor, f, to -1 <= f <= 1
        if (Math.abs(f) >= 1f) {
            f /= Math.abs(f)
        }

        return Math.round(OVERSCROLL_DAMP_FACTOR * f * max.toFloat())
    }
}
