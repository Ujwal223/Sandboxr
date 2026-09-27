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
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.util.FloatProperty

/**
 * Utility class to build and track a pending animation set and construct an [AnimatorPlaybackController].
 */
open class PendingAnimation(
    val duration: Long
) : AnimatedPropertySetter() {

    internal val animHolders = ArrayList<AnimatorPlaybackController.Holder>()

    @JvmOverloads
    fun add(anim: Animator, interpolator: TimeInterpolator, springProperty: SpringProperty = SpringProperty.DEFAULT) {
        anim.interpolator = interpolator
        add(anim, springProperty)
    }

    override fun add(animator: Animator) {
        add(animator, SpringProperty.DEFAULT)
    }

    fun add(a: Animator, springProperty: SpringProperty) {
        a.duration = duration
        anim.play(a)
        AnimatorPlaybackController.addAnimationHoldersRecur(a, duration, springProperty, animHolders)
    }

    fun setInterpolator(interpolator: TimeInterpolator) {
        anim.interpolator = interpolator
    }

    fun <T> addFloat(target: T, property: FloatProperty<T>, from: Float, to: Float, interpolator: TimeInterpolator) {
        val a = ObjectAnimator.ofFloat(target, property, from, to)
        a.interpolator = interpolator
        add(a)
    }

    fun addAnimatedFloat(target: AnimatedFloat, from: Float, to: Float, interpolator: TimeInterpolator) {
        val a = target.animateToValue(from, to)
        a.interpolator = interpolator
        add(a)
    }

    override fun buildAnim(): AnimatorSet {
        if (animHolders.isEmpty()) {
            add(ValueAnimator.ofFloat(0f, 1f).setDuration(duration))
        }
        return super.buildAnim()
    }

    fun createPlaybackController(): AnimatorPlaybackController {
        return AnimatorPlaybackController(buildAnim(), duration, animHolders)
    }
}
