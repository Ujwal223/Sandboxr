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
import android.animation.TimeInterpolator
import android.util.FloatProperty
import android.util.IntProperty
import android.view.View
import com.sandboxr.launcher.views.ScrimColors
import com.sandboxr.launcher.views.ScrimView
import java.util.function.Consumer

/**
 * Abstract class for setting properties on views and models, with or without animations.
 */
abstract class PropertySetter {

    open fun setViewAlpha(view: View?, alpha: Float, interpolator: TimeInterpolator): Animator {
        view?.let {
            it.alpha = alpha
            AlphaUpdateListener.updateVisibility(it)
        }
        return NO_OP
    }

    open fun setScrimColors(view: ScrimView?, scrimColors: ScrimColors, interpolator: TimeInterpolator): Animator {
        view?.let {
            it.scrimColors = scrimColors
        }
        return NO_OP
    }

    open fun <T> setFloat(target: T, property: FloatProperty<T>, value: Float, interpolator: TimeInterpolator): Animator {
        property.set(target, value)
        return NO_OP
    }

    open fun <T> setInt(target: T, property: IntProperty<T>, value: Int, interpolator: TimeInterpolator): Animator {
        property.set(target, value)
        return NO_OP
    }

    open fun <T> setColor(target: T, property: IntProperty<T>, value: Int, interpolator: TimeInterpolator): Animator {
        property.set(target, value)
        return NO_OP
    }

    abstract fun add(animator: Animator)

    open fun addEndListener(listener: Consumer<Boolean>) {
        listener.accept(true)
    }

    open fun buildAnim(): AnimatorSet = NO_OP

    companion object {
        @JvmField
        val NO_OP: AnimatorSet = AnimatorSet()

        @JvmField
        val NO_ANIM_PROPERTY_SETTER: PropertySetter = object : PropertySetter() {
            override fun add(animator: Animator) {
                animator.duration = 0
                animator.start()
                animator.end()
            }
        }
    }
}
