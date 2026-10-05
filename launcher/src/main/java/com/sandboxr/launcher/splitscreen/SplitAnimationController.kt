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

package com.sandboxr.launcher.splitscreen

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Rect
import android.graphics.RectF
import android.view.View

/**
 * Controller managing animations for initiating, confirming, and aborting split screen.
 */
class SplitAnimationController(
    val splitSelectStateController: SplitSelectStateController
) {

    /**
     * Creates an animator that transitions a staged task thumbnail from its original
     * position in overview to its staged split bounds.
     */
    fun createStagedRectAnimator(
        targetView: View,
        startBounds: Rect,
        endBounds: Rect,
        timings: SplitAnimationTimings = SplitAnimationTimings.PHONE_OVERVIEW_TO_SPLIT
    ): AnimatorSet {
        val animatorSet = AnimatorSet()

        val startX = startBounds.left.toFloat()
        val endX = endBounds.left.toFloat()
        val startY = startBounds.top.toFloat()
        val endY = endBounds.top.toFloat()

        val startScaleX = 1.0f
        val endScaleX = if (startBounds.width() > 0) endBounds.width().toFloat() / startBounds.width() else 1.0f
        val startScaleY = 1.0f
        val endScaleY = if (startBounds.height() > 0) endBounds.height().toFloat() / startBounds.height() else 1.0f

        val translateXAnim = ObjectAnimator.ofFloat(targetView, View.X, startX, endX).apply {
            duration = timings.duration.toLong()
            interpolator = timings.stagedRectXInterpolator
        }

        val translateYAnim = ObjectAnimator.ofFloat(targetView, View.Y, startY, endY).apply {
            duration = timings.duration.toLong()
            interpolator = timings.stagedRectYInterpolator
        }

        val scaleXAnim = ObjectAnimator.ofFloat(targetView, View.SCALE_X, startScaleX, endScaleX).apply {
            duration = timings.duration.toLong()
            interpolator = timings.stagedRectScaleXInterpolator
        }

        val scaleYAnim = ObjectAnimator.ofFloat(targetView, View.SCALE_Y, startScaleY, endScaleY).apply {
            duration = timings.duration.toLong()
            interpolator = timings.stagedRectScaleYInterpolator
        }

        animatorSet.playTogether(translateXAnim, translateYAnim, scaleXAnim, scaleYAnim)
        return animatorSet
    }

    /**
     * Animates split instructions view or toast entering overview.
     */
    fun createInstructionsFadeInAnimator(
        instructionsView: View,
        timings: SplitAnimationTimings = SplitAnimationTimings.PHONE_OVERVIEW_TO_SPLIT
    ): Animator {
        return ObjectAnimator.ofFloat(instructionsView, View.ALPHA, 0f, 1f).apply {
            duration = timings.duration.toLong()
            interpolator = timings.gridSlidePrimaryInterpolator
        }
    }

    /**
     * Animates aborting split screen selection and restoring normal state.
     */
    fun createAbortAnimator(
        floatingView: View,
        originalView: View?
    ): Animator {
        val animatorSet = AnimatorSet()
        val fadeAnim = ObjectAnimator.ofFloat(floatingView, View.ALPHA, floatingView.alpha, 0f).apply {
            duration = SplitAnimationTimings.ABORT_DURATION.toLong()
        }
        animatorSet.play(fadeAnim)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                floatingView.visibility = View.GONE
                originalView?.visibility = View.VISIBLE
            }
        })
        return animatorSet
    }
}
