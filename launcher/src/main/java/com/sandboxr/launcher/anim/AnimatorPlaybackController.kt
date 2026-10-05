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
import com.android.launcher3.Utilities
import com.sandboxr.launcher.util.window.RefreshRateTracker
import java.util.ArrayList
import java.util.Collections
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Controls the playback fraction, scrubbing, velocity launch, and spring dynamics of an [AnimatorSet].
 * Supports mid-flight interruption with smooth physics-based continuation.
 */
open class AnimatorPlaybackController(
    val target: AnimatorSet,
    val duration: Long,
    childAnims: ArrayList<Holder>
) : ValueAnimator.AnimatorUpdateListener {

    val animationPlayer: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
    private val childAnimations: Array<Holder> = childAnims.toTypedArray()

    var currentFraction: Float = 0f
        protected set

    private var targetCancelled: Boolean = false
    private var endAction: Runnable? = null

    init {
        animationPlayer.interpolator = Interpolators.LINEAR
        animationPlayer.addListener(OnAnimationEndDispatcher())
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

    open fun getInterpolatedProgress(): Float = interpolator.getInterpolation(currentFraction)

    open fun setEndAction(runnable: Runnable?) {
        endAction = runnable
    }

    open fun start() {
        animationPlayer.setFloatValues(currentFraction, 1f)
        animationPlayer.duration = clampDuration(1f - currentFraction)
        animationPlayer.start()
    }

    open fun reverse() {
        animationPlayer.setFloatValues(currentFraction, 0f)
        animationPlayer.duration = clampDuration(currentFraction)
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
        val nextFrameProgress = Utilities.boundToRange(progressFraction + oneFrameProgress, 0f, 1f)

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

                val expectedDurationL = s.getDuration()
                springDuration = max(expectedDurationL, springDuration)
                val expectedDuration = expectedDurationL.toFloat()

                h.mapper = ProgressMapper { _, _ ->
                    if (expectedDuration <= 0f || abs(oneFrameProgress) >= 1f) {
                        1f
                    } else {
                        Utilities.mapToRange(
                            animationPlayer.currentPlayTime.toFloat() / expectedDuration,
                            0f, 1f,
                            abs(oneFrameProgress), 1f,
                            Interpolators.LINEAR
                        )
                    }
                }
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

    open fun forceFinishIfCloseToEnd() {
        if (animationPlayer.isRunning && animationPlayer.animatedFraction > ANIMATION_COMPLETE_THRESHOLD) {
            animationPlayer.end()
        }
    }

    open fun pause() {
        for (h in childAnimations) {
            h.reset()
        }
        animationPlayer.cancel()
    }

    open fun setPlayFraction(fraction: Float) {
        currentFraction = fraction
        if (targetCancelled) {
            return
        }
        val progress = Utilities.boundToRange(fraction, 0f, 1f)
        for (h in childAnimations) {
            h.setProgress(progress)
        }
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        setPlayFraction((animation.animatedValue as Number).toFloat())
    }

    protected open fun clampDuration(fraction: Float): Long {
        val playPos = duration.toFloat() * fraction
        return if (playPos <= 0f) {
            0L
        } else {
            min(playPos.toLong(), duration)
        }
    }

    open fun dispatchOnStart(): AnimatorPlaybackController {
        callListenerCommandRecursively(target) { listener, anim -> listener.onAnimationStart(anim) }
        return this
    }

    open fun dispatchOnCancel(): AnimatorPlaybackController {
        callListenerCommandRecursively(target) { listener, anim -> listener.onAnimationCancel(anim) }
        return this
    }

    open fun dispatchOnEnd(): AnimatorPlaybackController {
        callListenerCommandRecursively(target) { listener, anim -> listener.onAnimationEnd(anim) }
        return this
    }

    open fun dispatchSetInterpolator(interpolator: TimeInterpolator) {
        callAnimatorCommandRecursively(target) { a -> a.interpolator = interpolator }
    }

    private inner class OnAnimationEndDispatcher : AnimationSuccessListener() {
        private var dispatched = false

        override fun onAnimationStart(animation: Animator) {
            super.onAnimationStart(animation)
            dispatched = false
        }

        override fun onAnimationSuccess(animator: Animator) {
            if (!dispatched) {
                dispatchOnEnd()
                endAction?.run()
                dispatched = true
            }
        }
    }

    fun interface ProgressMapper {
        fun getProgress(progress: Float, globalProgress: Float): Float

        companion object {
            @JvmField
            val DEFAULT = ProgressMapper { progress, globalEndProgress ->
                if (progress > globalEndProgress) 1f else (progress / globalEndProgress)
            }
        }
    }

    class Holder(
        val anim: ValueAnimator,
        val globalDuration: Float,
        val springProperty: SpringProperty
    ) {
        val interpolator: TimeInterpolator = anim.interpolator ?: Interpolators.LINEAR
        val globalEndProgress: Float = if (globalDuration > 0f) anim.duration.toFloat() / globalDuration else 1f
        var mapper: ProgressMapper = ProgressMapper.DEFAULT

        fun setProgress(progress: Float) {
            anim.setCurrentFraction(mapper.getProgress(progress, globalEndProgress))
        }

        fun reset() {
            anim.interpolator = interpolator
            mapper = ProgressMapper.DEFAULT
        }
    }

    companion object {
        private const val ANIMATION_COMPLETE_THRESHOLD = 0.95f

        @JvmStatic
        fun wrap(anim: AnimatorSet, duration: Long): AnimatorPlaybackController {
            val childAnims = ArrayList<Holder>()
            addAnimationHoldersRecur(anim, duration, SpringProperty.DEFAULT, childAnims)
            return AnimatorPlaybackController(anim, duration, childAnims)
        }

        @JvmStatic
        fun addAnimationHoldersRecur(
            anim: Animator,
            globalDuration: Long,
            springProperty: SpringProperty,
            out: ArrayList<Holder>
        ) {
            val forceDuration = anim.duration
            val forceInterpolator = anim.interpolator
            if (anim is ValueAnimator) {
                out.add(Holder(anim, globalDuration.toFloat(), springProperty))
            } else if (anim is AnimatorSet) {
                for (child in anim.childAnimations) {
                    if (forceDuration > 0) {
                        child.duration = forceDuration
                    }
                    if (forceInterpolator != null) {
                        child.interpolator = forceInterpolator
                    }
                    addAnimationHoldersRecur(child, globalDuration, springProperty, out)
                }
            } else {
                throw IllegalArgumentException("Unknown animation type: $anim")
            }
        }

        @JvmStatic
        fun callListenerCommandRecursively(anim: Animator, command: (Animator.AnimatorListener, Animator) -> Unit) {
            callAnimatorCommandRecursively(anim) { a ->
                val listeners = a.listeners
                if (listeners != null) {
                    for (listener in ArrayList(listeners)) {
                        command(listener, a)
                    }
                }
            }
        }

        @JvmStatic
        fun callAnimatorCommandRecursively(anim: Animator, command: (Animator) -> Unit) {
            command(anim)
            if (anim is AnimatorSet) {
                for (child in anim.childAnimations) {
                    callAnimatorCommandRecursively(child, command)
                }
            }
        }
    }
}
