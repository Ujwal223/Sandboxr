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
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.util.FloatProperty
import android.util.IntProperty
import android.view.View
import com.sandboxr.launcher.views.ScrimColors
import com.sandboxr.launcher.views.ScrimView
import java.util.function.Consumer

/**
 * Extension of [PropertySetter] that constructs an [AnimatorSet] for scheduled property changes.
 */
open class AnimatedPropertySetter : PropertySetter() {

    @JvmField
    val anim: AnimatorSet = AnimatorSet()

    @JvmField
    protected var progressAnimator: ValueAnimator? = null

    override fun setViewAlpha(view: View?, alpha: Float, interpolator: TimeInterpolator): Animator {
        if (view == null) return NO_OP
        if (view.alpha == alpha) {
            AlphaUpdateListener.updateVisibility(view)
            return NO_OP
        }
        val a = ObjectAnimator.ofFloat(view, View.ALPHA, alpha)
        a.addListener(AlphaUpdateListener(view))
        a.interpolator = interpolator
        add(a)
        return a
    }

    override fun setScrimColors(view: ScrimView?, scrimColors: ScrimColors, interpolator: TimeInterpolator): Animator {
        if (view == null || view.scrimColors == scrimColors) return NO_OP
        val a = ValueAnimator.ofArgb(view.scrimColors.backgroundColor, scrimColors.backgroundColor)
        a.interpolator = interpolator
        a.addUpdateListener { va ->
            val bg = va.animatedValue as Int
            view.scrimColors = ScrimColors(bg, scrimColors.foregroundColor)
        }
        add(a)
        return a
    }

    override fun <T> setFloat(target: T, property: FloatProperty<T>, value: Float, interpolator: TimeInterpolator): Animator {
        if (property.get(target) == value) return NO_OP
        val a = ObjectAnimator.ofFloat(target, property, value)
        a.interpolator = interpolator
        add(a)
        return a
    }

    override fun <T> setInt(target: T, property: IntProperty<T>, value: Int, interpolator: TimeInterpolator): Animator {
        if (property.get(target) == value) return NO_OP
        val a = ObjectAnimator.ofInt(target, property, value)
        a.interpolator = interpolator
        add(a)
        return a
    }

    override fun <T> setColor(target: T, property: IntProperty<T>, value: Int, interpolator: TimeInterpolator): Animator {
        if (property.get(target) == value) return NO_OP
        val a = ObjectAnimator.ofArgb(target, property, value)
        a.interpolator = interpolator
        add(a)
        return a
    }

    override fun add(animator: Animator) {
        anim.play(animator)
    }

    fun addListener(listener: Animator.AnimatorListener) {
        anim.addListener(listener)
    }

    fun addOnFrameListener(listener: ValueAnimator.AnimatorUpdateListener) {
        getProgressAnimator().addUpdateListener(listener)
    }

    override fun addEndListener(listener: Consumer<Boolean>) {
        getProgressAnimator().addListener(AnimatorListeners.forEndCallback(listener))
    }

    fun addStartListener(listener: Runnable) {
        getProgressAnimator().addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                listener.run()
            }
        })
    }

    private fun getProgressAnimator(): ValueAnimator {
        if (progressAnimator == null) {
            progressAnimator = ValueAnimator.ofFloat(0f, 1f)
        }
        return progressAnimator!!
    }

    override fun buildAnim(): AnimatorSet {
        progressAnimator?.let {
            anim.play(it)
            progressAnimator = null
        }
        return anim
    }
}
