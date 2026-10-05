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

import android.animation.ValueAnimator
import android.graphics.Rect
import android.graphics.RectF
import android.view.animation.OvershootInterpolator

/**
 * Runner that applies spring dynamics to transitions (e.g. overshooting and settling)
 * for fluid tactile response.
 */
class SpringAnimRunner(
    val startBounds: RectF,
    val targetBounds: Rect,
    val dampingRatio: Float = 0.85f,
    val stiffness: Float = 400f
) {

    private val overshootInterpolator = OvershootInterpolator(1.2f)

    var currentProgress: Float = 0f
        private set

    val currentBounds: RectF = RectF()

    fun createAnimator(durationMs: Long = 450L, onUpdate: ((Float) -> Unit)? = null): ValueAnimator {
        return ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = overshootInterpolator
            addUpdateListener { va ->
                val progress = va.animatedValue as Float
                currentProgress = progress
                val left = startBounds.left + (targetBounds.left - startBounds.left) * progress
                val top = startBounds.top + (targetBounds.top - startBounds.top) * progress
                val right = startBounds.right + (targetBounds.right - startBounds.right) * progress
                val bottom = startBounds.bottom + (targetBounds.bottom - startBounds.bottom) * progress
                currentBounds.set(left, top, right, bottom)
                onUpdate?.invoke(progress)
            }
        }
    }
}
