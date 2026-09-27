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
import android.animation.ObjectAnimator
import android.util.FloatProperty
import java.util.function.Consumer

/**
 * A mutable float that allows animating between values and tracking active animators.
 */
open class AnimatedFloat @JvmOverloads constructor(
    private val updateCallback: Consumer<Float> = Consumer { },
    initialValue: Float = 0f
) {

    constructor(runnable: Runnable) : this(Consumer { runnable.run() }, 0f)

    @JvmField
    var value: Float = initialValue

    private var valueAnimator: ObjectAnimator? = null
    private var endValue: Float? = null

    fun animateToValue(end: Float): ObjectAnimator {
        return animateToValue(value, end)
    }

    fun animateToValue(start: Float, end: Float): ObjectAnimator {
        cancelAnimation()
        val anim = ObjectAnimator.ofFloat(this, VALUE, start, end)
        valueAnimator = anim
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animator: Animator) {
                if (valueAnimator == animator) {
                    endValue = end
                }
            }

            override fun onAnimationEnd(animator: Animator) {
                if (valueAnimator == animator) {
                    valueAnimator = null
                    endValue = null
                }
            }
        })
        return anim
    }

    fun updateValue(v: Float) {
        if (v != value) {
            value = v
            updateCallback.accept(value)
        }
    }

    fun cancelAnimation() {
        valueAnimator?.let { anim ->
            anim.cancel()
            valueAnimator = null
            endValue = null
        }
    }

    fun finishAnimation() {
        if (valueAnimator?.isRunning == true) {
            valueAnimator?.end()
        }
    }

    fun isAnimating(): Boolean = valueAnimator != null

    fun isAnimatingToValue(targetValue: Float): Boolean =
        isAnimating() && endValue != null && endValue == targetValue

    fun isSettledOnValue(targetValue: Float): Boolean =
        !isAnimating() && value == targetValue

    companion object {
        @JvmField
        val VALUE: FloatProperty<AnimatedFloat> = object : FloatProperty<AnimatedFloat>("value") {
            override fun setValue(obj: AnimatedFloat, value: Float) {
                obj.updateValue(value)
            }

            override fun get(obj: AnimatedFloat): Float = obj.value
        }
    }
}
