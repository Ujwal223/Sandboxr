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

import android.content.Context
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlin.math.max
import kotlin.math.min

/**
 * Given a property to animate and a target value with starting velocity, applies fling friction
 * until reaching target, then triggers a spring force to settle at final position.
 */
open class FlingSpringAnim<K>(
    val target: K,
    val context: Context,
    val property: FloatPropertyCompat<K>,
    startPosition: Float,
    targetPosition: Float,
    startVelocityPxPerS: Float,
    minVisChange: Float,
    minValue: Float,
    maxValue: Float,
    damping: Float,
    stiffness: Float,
    onEndListener: DynamicAnimation.OnAnimationEndListener?
) {

    private val flingAnim: FlingAnimation
    private var springAnim: SpringAnimation? = null
    private val skipFlingAnim: Boolean
    var currentTargetPosition: Float = targetPosition
        private set

    init {
        val friction = 1.5f
        flingAnim = FlingAnimation(target, property)
            .setFriction(friction)
            .setMinimumVisibleChange(minVisChange)
            .setStartVelocity(startVelocityPxPerS)
            .setMinValue(minValue)
            .setMaxValue(maxValue)

        skipFlingAnim = (startPosition <= minValue && startVelocityPxPerS < 0) ||
                (startPosition >= maxValue && startVelocityPxPerS > 0)

        flingAnim.addEndListener { _, _, value, velocity ->
            val sAnim = SpringAnimation(target, property)
                .setStartValue(value)
                .setStartVelocity(velocity)
                .setSpring(
                    SpringForce(currentTargetPosition)
                        .setStiffness(stiffness)
                        .setDampingRatio(damping)
                )
            if (onEndListener != null) {
                sAnim.addEndListener(onEndListener)
            }
            springAnim = sAnim
            sAnim.animateToFinalPosition(currentTargetPosition)
        }
    }

    fun updatePosition(startPosition: Float, targetPosition: Float) {
        flingAnim.setMinValue(min(startPosition, targetPosition))
            .setMaxValue(max(startPosition, targetPosition))
        currentTargetPosition = targetPosition
        springAnim?.animateToFinalPosition(currentTargetPosition)
    }

    fun start() {
        flingAnim.start()
        if (skipFlingAnim) {
            flingAnim.cancel()
        }
    }

    fun end() {
        flingAnim.cancel()
        springAnim?.let {
            if (it.canSkipToEnd()) {
                it.skipToEnd()
            }
        }
    }
}
