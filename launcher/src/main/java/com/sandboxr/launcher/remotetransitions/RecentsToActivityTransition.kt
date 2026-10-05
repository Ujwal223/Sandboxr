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

package com.sandboxr.launcher.remotetransitions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.Rect
import android.graphics.RectF
import android.os.IBinder
import android.view.SurfaceControl
import android.view.View
import android.view.animation.PathInterpolator
import android.window.TransitionInfo

/**
 * Handles remote transition when launching an activity from an overview recents task card into fullscreen.
 */
class RecentsToActivityTransition(
    val taskStartBounds: RectF,
    val windowTargetBounds: Rect,
    val taskView: View? = null,
    val windowView: View? = null,
    val recentsView: View? = null,
    val durationMs: Long = 350L
) : RemoteAnimationRunner {

    val interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)

    var currentProgress: Float = 0f
        private set

    val currentBounds: RectF = RectF()
    var currentCornerRadius: Float = 0f
        private set

    val startCornerRadius: Float = 24f
    val endCornerRadius: Float = 0f

    private var activeAnimatorSet: AnimatorSet? = null

    fun createAnimator(onAnimationEnd: (() -> Unit)? = null): AnimatorSet {
        val animatorSet = AnimatorSet()
        val morphAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = this@RecentsToActivityTransition.interpolator
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
        activeAnimatorSet = animatorSet
        return animatorSet
    }

    fun updateProgress(progress: Float) {
        currentProgress = progress

        // Interpolate bounds from task card to fullscreen window
        val left = taskStartBounds.left + (windowTargetBounds.left - taskStartBounds.left) * progress
        val top = taskStartBounds.top + (windowTargetBounds.top - taskStartBounds.top) * progress
        val right = taskStartBounds.right + (windowTargetBounds.right - taskStartBounds.right) * progress
        val bottom = taskStartBounds.bottom + (windowTargetBounds.bottom - taskStartBounds.bottom) * progress
        currentBounds.set(left, top, right, bottom)

        // Interpolate corner radius
        currentCornerRadius = startCornerRadius + (endCornerRadius - startCornerRadius) * progress

        // Update target window view
        windowView?.let { win ->
            win.x = left
            win.y = top
            val startW = taskStartBounds.width().coerceAtLeast(1f)
            val startH = taskStartBounds.height().coerceAtLeast(1f)
            val targetW = windowTargetBounds.width().toFloat().coerceAtLeast(1f)
            val targetH = windowTargetBounds.height().toFloat().coerceAtLeast(1f)

            win.scaleX = (startW + (targetW - startW) * progress) / targetW
            win.scaleY = (startH + (targetH - startH) * progress) / targetH
            win.alpha = progress.coerceIn(0f, 1f)
        }

        // Fade out recents view and task card
        recentsView?.let { recents ->
            recents.alpha = (1f - progress).coerceIn(0f, 1f)
        }
        taskView?.let { task ->
            task.alpha = (1f - progress).coerceIn(0f, 1f)
        }
    }

    override fun onAnimationStart(
        transition: IBinder?,
        info: TransitionInfo?,
        transaction: SurfaceControl.Transaction?,
        onFinish: () -> Unit
    ) {
        val anim = createAnimator {
            onFinish()
        }
        anim.start()
    }

    override fun onAnimationCancelled() {
        activeAnimatorSet?.cancel()
        activeAnimatorSet = null
    }
}
