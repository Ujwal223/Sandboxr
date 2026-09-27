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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import com.sandboxr.launcher.util.window.RefreshRateTracker
import java.util.ArrayList
import java.util.Collections
import kotlin.math.abs
import kotlin.math.max

/**
 * Controls the playback fraction, scrubbing, velocity launch, and spring dynamics of an [AnimatorSet].
 */
open class AnimatorPlaybackController constructor(
    val target: AnimatorSet,
    val duration: Long,
    childAnims: ArrayList<Holder>
) : ValueAnimator.AnimatorUpdateListener {

    private val animationPlayer: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
    private val childAnimations: Array<Holder> = childAnims.toTypedArray()

    var currentFraction: Float = 0f
        protected set

    private var targetCancelled: Boolean = false

    init {
        animationPlayer.interpolator = Interpolators.LINEAR
        animationPlayer.addUpdateListener(this)

        target.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator) {
                targetCancelled = true
            }

            override fun onAnimationEnd(animation: Animator) {
                targetCancelled = false
            }

            override fun onAnimationStart(animation: Animator) {
                targetCancelled = false
            }
        })
    }

    val interpolator: TimeInterpolator
        get() = target.interpolator ?: Interpolators.LINEAR

    val progressFraction: Float
        get() = currentFraction

    open fun start() {
        animationPlayer.setFloatValues(currentFraction, 1f)
        animationPlayer.duration = (duration * (1f - currentFraction)).toLong().coerceAtLeast(0)
        animationPlayer.start()
    }

    open fun reverse() {
        animationPlayer.setFloatValues(currentFraction, 0f)
        animationPlayer.duration = (duration * currentFraction).toLong().coerceAtLeast(0)
        animationPlayer.start()
    }

    open fun startWithVelocity(
        context: Context,
        goingToEnd: Boolean,
        velocityPxPerMs: Float,
        endDistance: Float,
        animationDuration: Long
    ) {
        val distanceInverse = 1f / abs(endDistance).coerceAtLeast(1f)
        val velocityProgressPerMs = velocityPxPerMs * distanceInverse
        val singleFrameMs = RefreshRateTracker.getSingleFrameMs(context).toFloat()
        val oneFrameProgress = velocityProgressPerMs * singleFrameMs
        val nextFrameProgress = (progressFraction + oneFrameProgress).coerceIn(0f, 1f)

        val springFlag = if (goingToEnd) SpringProperty.FLAG_CAN_SPRING_ON_END else SpringProperty.FLAG_CAN_SPRING_ON_START
        var springDuration = animationDuration

        for (h in childAnimations) {
            if ((h.springProperty.flags and springFlag) != 0) {
                val s = SpringAnimationBuilder(context)
                    .setStartValue(currentFraction)
                    .setEndValue(if (goingToEnd) 1f else 0f)
                    .setStartVelocity(velocityProgressPerMs)
                    .setMinimumVisibleChange(distanceInverse)
                    .setDampingRatio(h.springProperty.dampingRatio)
                    .setStiffness(h.springProperty.stiffness)
                    .computeParams()

                val expectedDuration = s.getDuration()
                springDuration = max(expectedDuration, springDuration)

                h.anim.interpolator = TimeInterpolator { f -> s.getInterpolatedValue(f) }
            }
        }

        animationPlayer.setFloatValues(nextFrameProgress, if (goingToEnd) 1f else 0f)
        if (springDuration <= animationDuration) {
            animationPlayer.duration = animationDuration
            animationPlayer.interpolator = Interpolators.scrollInterpolatorForVelocity(velocityPxPerMs)
        } else {
            animationPlayer.duration = springDuration
            val cutOff = animationDuration.toFloat() / springDuration.toFloat()
            animationPlayer.interpolator = Interpolators.clampToProgress(
                Interpolators.scrollInterpolatorForVelocity(velocityPxPerMs),
                0f,
                cutOff
            )
        }
        animationPlayer.start()
    }

    open fun pause() {
        for (h in childAnimations) {
            h.reset()
        }
        animationPlayer.cancel()
    }

    open fun setPlayFraction(fraction: Float) {
        currentFraction = fraction.coerceIn(0f, 1f)
        for (h in childAnimations) {
            h.setProgress(currentFraction)
        }
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        setPlayFraction(animation.animatedValue as Float)
    }

    fun dispatchOnStart() {
        callListenerCommandRecursively(target) { listener -> listener.onAnimationStart(target) }
    }

    fun dispatchOnCancel() {
        callListenerCommandRecursively(target) { listener -> listener.onAnimationCancel(target) }
    }

    fun dispatchOnEnd() {
        callListenerCommandRecursively(target) { listener -> listener.onAnimationEnd(target) }
    }

    class Holder(
        val anim: ValueAnimator,
        val springProperty: SpringProperty
    ) {
        fun setProgress(progress: Float) {
            anim.setCurrentFraction(progress)
        }

        fun reset() {
            // no-op reset
        }
    }

    companion object {
        @JvmStatic
        fun wrap(anim: AnimatorSet, duration: Long): AnimatorPlaybackController {
            val childAnims = ArrayList<Holder>()
            addAnimationHoldersRecur(anim, duration, SpringProperty.DEFAULT, childAnims)
            return AnimatorPlaybackController(anim, duration, childAnims)
        }

        @JvmStatic
        fun addAnimationHoldersRecur(
            anim: Animator,
            totalDuration: Long,
            springProperty: SpringProperty,
            out: ArrayList<Holder>
        ) {
            if (anim is ValueAnimator) {
                out.add(Holder(anim, springProperty))
            } else if (anim is AnimatorSet) {
                for (child in anim.childAnimations) {
                    addAnimationHoldersRecur(child, totalDuration, springProperty, out)
                }
            }
        }

        @JvmStatic
        fun callListenerCommandRecursively(anim: Animator, command: (Animator.AnimatorListener) -> Unit) {
            val listeners = anim.listeners
            if (listeners != null) {
                for (listener in ArrayList(listeners)) {
                    command(listener)
                }
            }
            if (anim is AnimatorSet) {
                for (child in anim.childAnimations) {
                    callListenerCommandRecursively(child, command)
                }
            }
        }
    }
}
