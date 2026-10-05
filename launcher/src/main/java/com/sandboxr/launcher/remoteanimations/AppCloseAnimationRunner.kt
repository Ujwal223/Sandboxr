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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.view.animation.PathInterpolator

/**
 * Runner that shrinks a closing window surface back to the launcher icon position.
 */
class AppCloseAnimationRunner(
    val windowStartBounds: Rect,
    val iconTargetBounds: RectF,
    val windowView: View? = null,
    val iconView: View? = null,
    val durationMs: Long = 350L
) {

    val interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)

    var currentProgress: Float = 0f
        private set

    val currentBounds: RectF = RectF()
    var currentCornerRadius: Float = 0f
        private set

    val startCornerRadius: Float = 28f
    val endCornerRadius: Float = iconTargetBounds.width() / 4f

    fun createAnimator(onAnimationEnd: (() -> Unit)? = null): AnimatorSet {
        val animatorSet = AnimatorSet()
        val shrinkAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = this@AppCloseAnimationRunner.interpolator
            addUpdateListener { va ->
                val progress = va.animatedValue as Float
                updateProgress(progress)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    updateProgress(1f)
                    onAnimationEnd?.invoke()
                }
            })
        }
        animatorSet.play(shrinkAnimator)
        return animatorSet
    }

    fun updateProgress(progress: Float) {
        currentProgress = progress

        // Interpolate bounds from window to icon target
        val left = windowStartBounds.left + (iconTargetBounds.left - windowStartBounds.left) * progress
        val top = windowStartBounds.top + (iconTargetBounds.top - windowStartBounds.top) * progress
        val right = windowStartBounds.right + (iconTargetBounds.right - windowStartBounds.right) * progress
        val bottom = windowStartBounds.bottom + (iconTargetBounds.bottom - windowStartBounds.bottom) * progress
        currentBounds.set(left, top, right, bottom)

        // Interpolate corner radius
        currentCornerRadius = startCornerRadius + (endCornerRadius - startCornerRadius) * progress

        // Apply scale/position to windowView
        windowView?.let { win ->
            win.x = left
            win.y = top
            val startW = windowStartBounds.width().toFloat().coerceAtLeast(1f)
            val startH = windowStartBounds.height().toFloat().coerceAtLeast(1f)
            val targetW = iconTargetBounds.width()
            val targetH = iconTargetBounds.height()

            win.scaleX = (startW + (targetW - startW) * progress) / startW
            win.scaleY = (startH + (targetH - startH) * progress) / startH
            win.alpha = (1f - progress).coerceIn(0f, 1f)
        }

        // Fade icon in
        iconView?.let { icon ->
            icon.alpha = progress.coerceIn(0f, 1f)
        }
    }
}
