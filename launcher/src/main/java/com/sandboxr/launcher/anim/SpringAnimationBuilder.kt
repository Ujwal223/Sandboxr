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
import android.animation.ValueAnimator
import android.content.Context
import android.util.FloatProperty
import androidx.dynamicanimation.animation.SpringForce
import com.sandboxr.launcher.util.window.RefreshRateTracker
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility class to build an animator following the curve of an underdamped spring.
 */
open class SpringAnimationBuilder(private val context: Context) {

    private var startValue: Float = 0f
    private var endValue: Float = 1f
    private var velocity: Float = 0f

    private var stiffness: Float = SpringForce.STIFFNESS_MEDIUM
    private var dampingRatio: Float = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
    private var minVisibleChange: Float = 1f

    private var beta: Double = 0.0
    private var gamma: Double = 0.0
    private var a: Double = 0.0
    private var b: Double = 0.0
    private var va: Double = 0.0
    private var vb: Double = 0.0

    private var valueThreshold: Double = 0.0
    private var velocityThreshold: Double = 0.0
    private var durationSeconds: Float = 0f

    fun setEndValue(value: Float): SpringAnimationBuilder {
        endValue = value
        return this
    }

    fun setStartValue(value: Float): SpringAnimationBuilder {
        startValue = value
        return this
    }

    fun setValues(vararg values: Float): SpringAnimationBuilder {
        if (values.size > 1) {
            startValue = values[0]
            endValue = values[values.size - 1]
        } else if (values.isNotEmpty()) {
            endValue = values[0]
        }
        return this
    }

    fun setStiffness(stiffness: Float): SpringAnimationBuilder {
        require(stiffness > 0f) { "Spring stiffness must be positive." }
        this.stiffness = stiffness
        return this
    }

    fun setDampingRatio(dampingRatio: Float): SpringAnimationBuilder {
        require(dampingRatio in 0.0001f..0.9999f) { "Damping ratio must be between 0 and 1." }
        this.dampingRatio = dampingRatio
        return this
    }

    fun setMinimumVisibleChange(minimumVisibleChange: Float): SpringAnimationBuilder {
        require(minimumVisibleChange > 0f) { "Minimum visible change must be positive." }
        minVisibleChange = minimumVisibleChange
        return this
    }

    fun setStartVelocity(startVelocity: Float): SpringAnimationBuilder {
        velocity = startVelocity
        return this
    }

    fun computeParams(): SpringAnimationBuilder {
        val singleFrameMs = RefreshRateTracker.getSingleFrameMs(context)
        val naturalFreq = sqrt(stiffness.toDouble())
        val dampedFreq = naturalFreq * sqrt(1.0 - dampingRatio * dampingRatio)

        beta = 2.0 * dampingRatio * naturalFreq
        gamma = dampedFreq
        a = (startValue - endValue).toDouble()
        b = (beta * a / (2.0 * gamma)) + (velocity.toDouble() / gamma)

        va = a * beta / 2.0 - b * gamma
        vb = a * gamma + beta * b / 2.0

        valueThreshold = (minVisibleChange * THRESHOLD_MULTIPLIER).toDouble()
        velocityThreshold = valueThreshold * 1000.0 / singleFrameMs

        var duration = atan2(-a, b) / gamma
        val piByG = Math.PI / gamma
        while (duration < 0.0 || Math.abs(exponentialComponent(duration) * cosSinV(duration)) >= velocityThreshold) {
            duration += piByG
        }

        var edgeTime = max(0.0, duration - piByG / 2.0)
        val minDiff = singleFrameMs / 2000.0

        do {
            if ((duration - edgeTime) < minDiff) {
                break
            }
            val mid = (edgeTime + duration) / 2.0
            if (isAtEquilibrium(mid)) {
                duration = mid
            } else {
                edgeTime = mid
            }
        } while (true)

        durationSeconds = duration.toFloat()
        return this
    }

    fun getDuration(): Long = (1000.0 * durationSeconds).toLong()

    fun getInterpolatedValue(fraction: Float): Float {
        return getValue(durationSeconds * fraction)
    }

    private fun getValue(time: Float): Float {
        val v = (exponentialComponent(time.toDouble()) * cosSinX(time.toDouble())) + endValue
        return if (v.isNaN()) endValue else v.toFloat()
    }

    private fun isAtEquilibrium(t: Double): Boolean {
        val ec = exponentialComponent(t)
        if (Math.abs(ec * cosSinX(t)) >= valueThreshold) {
            return false
        }
        return Math.abs(ec * cosSinV(t)) < velocityThreshold
    }

    private fun exponentialComponent(t: Double): Double = exp(-beta * t / 2.0)

    private fun cosSinX(t: Double): Double = cosSin(t, a, b)

    private fun cosSinV(t: Double): Double = cosSin(t, va, vb)

    private fun cosSin(t: Double, cosFactor: Double, sinFactor: Double): Double {
        val angle = t * gamma
        return cosFactor * cos(angle) + sinFactor * sin(angle)
    }

    fun <T> build(target: T, property: FloatProperty<T>): ValueAnimator {
        computeParams()

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = getDuration()
        animator.interpolator = Interpolators.LINEAR
        animator.addUpdateListener { anim ->
            var value = getInterpolatedValue(anim.animatedFraction)
            if (value.isNaN()) {
                value = endValue
            }
            property.set(target, value)
        }
        animator.addListener(object : AnimationSuccessListener() {
            override fun onAnimationSuccess(animator: Animator) {
                property.set(target, endValue)
            }
        })
        return animator
    }

    companion object {
        private const val THRESHOLD_MULTIPLIER = 0.65f
    }
}
