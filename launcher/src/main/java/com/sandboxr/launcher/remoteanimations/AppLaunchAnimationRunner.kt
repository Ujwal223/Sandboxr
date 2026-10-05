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
 * Runner that morphs an icon into an opening window surface during app launch.
 */
class AppLaunchAnimationRunner(
    val iconBounds: RectF,
    val windowTargetBounds: Rect,
    val iconView: View? = null,
    val windowView: View? = null,
    val durationMs: Long = 400L
) {

    val interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)

    var currentProgress: Float = 0f
        private set

    val currentBounds: RectF = RectF()
    var currentCornerRadius: Float = 0f
        private set

    val startCornerRadius: Float = iconBounds.width() / 4f
    val endCornerRadius: Float = 28f

    fun createAnimator(onAnimationEnd: (() -> Unit)? = null): AnimatorSet {
        val animatorSet = AnimatorSet()
        val morphAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = this@AppLaunchAnimationRunner.interpolator
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
        animatorSet.play(morphAnimator)
        return animatorSet
    }

    fun updateProgress(progress: Float) {
        currentProgress = progress

        // Interpolate bounds from icon to window target
        val left = iconBounds.left + (windowTargetBounds.left - iconBounds.left) * progress
        val top = iconBounds.top + (windowTargetBounds.top - iconBounds.top) * progress
        val right = iconBounds.right + (windowTargetBounds.right - iconBounds.right) * progress
        val bottom = iconBounds.bottom + (windowTargetBounds.bottom - iconBounds.bottom) * progress
        currentBounds.set(left, top, right, bottom)

        // Interpolate corner radius
        currentCornerRadius = startCornerRadius + (endCornerRadius - startCornerRadius) * progress

        // Apply to windowView if present
        windowView?.let { win ->
            win.x = left
            win.y = top
            val startW = iconBounds.width()
            val startH = iconBounds.height()
            val targetW = windowTargetBounds.width().toFloat().coerceAtLeast(1f)
            val targetH = windowTargetBounds.height().toFloat().coerceAtLeast(1f)

            win.scaleX = (startW + (targetW - startW) * progress) / targetW
            win.scaleY = (startH + (targetH - startH) * progress) / targetH
            win.alpha = progress.coerceIn(0f, 1f)
        }

        // Cross-fade icon out
        iconView?.let { icon ->
            icon.alpha = (1f - progress * 1.5f).coerceIn(0f, 1f)
        }
    }
}
