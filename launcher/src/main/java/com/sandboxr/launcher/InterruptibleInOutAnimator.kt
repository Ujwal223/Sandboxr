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

package com.sandboxr.launcher

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.util.Property

/**
 * A convenience class for two-way animations, e.g. a fadeIn/fadeOut animation.
 * Both the 'in' and 'out' animations use the interpolator in the forward direction.
 */
open class InterruptibleInOutAnimator(
    duration: Long,
    fromValue: Float,
    toValue: Float
) {
    private var mOriginalDuration: Long = duration
    private var mOriginalFromValue: Float = fromValue
    private var mOriginalToValue: Float = toValue
    private var mAnimator: ValueAnimator
    private var mValue: Float = fromValue
    private var mFirstRun: Boolean = true
    private var mTag: Any? = null
    var direction: Int = STOPPED
        protected set

    val animator: ValueAnimator
        get() = mAnimator

    init {
        mAnimator = ObjectAnimator.ofFloat(this, VALUE, fromValue, toValue).setDuration(duration)
        mAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                direction = STOPPED
            }
        })
    }

    private fun animate(dir: Int) {
        val currentPlayTime = mAnimator.currentPlayTime
        val targetVal = if (dir == IN) mOriginalToValue else mOriginalFromValue
        val startVal = if (mFirstRun) mOriginalFromValue else mValue

        cancel()
        direction = dir
        val duration = mOriginalDuration - currentPlayTime
        mAnimator.duration = duration.coerceIn(0, mOriginalDuration)
        mAnimator.setFloatValues(startVal, targetVal)
        mAnimator.start()
        mFirstRun = false
    }

    fun cancel() {
        mAnimator.cancel()
        direction = STOPPED
    }

    fun end() {
        mAnimator.end()
        direction = STOPPED
    }

    fun isStopped(): Boolean = direction == STOPPED

    fun animateIn() {
        animate(IN)
    }

    fun animateOut() {
        animate(OUT)
    }

    fun setTag(tag: Any?) {
        mTag = tag
    }

    fun getTag(): Any? = mTag


    companion object {
        const val STOPPED = 0
        const val IN = 1
        const val OUT = 2

        @JvmField
        val VALUE: Property<InterruptibleInOutAnimator, Float> =
            object : Property<InterruptibleInOutAnimator, Float>(Float::class.java, "value") {
                override fun get(anim: InterruptibleInOutAnimator): Float = anim.mValue
                override fun set(anim: InterruptibleInOutAnimator, value: Float) {
                    anim.mValue = value
                }
            }
    }
}
