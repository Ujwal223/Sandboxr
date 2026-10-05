/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Looper
import android.util.FloatProperty
import android.util.IntProperty
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.TextView
import com.android.launcher3.Utilities
import kotlin.math.abs

/**
 * Common property animators, interpolator providers, and utilities for Launcher animations.
 */
object LauncherAnimUtils {

    const val SPRING_LOADED_EXIT_DELAY: Int = 500

    const val SUCCESS_TRANSITION_PROGRESS: Float = 0.5f
    const val TABLET_BOTTOM_SHEET_SUCCESS_TRANSITION_PROGRESS: Float = 0.3f

    const val SCALE_INDEX_UNFOLD_ANIMATION: Int = 1
    const val SCALE_INDEX_WORKSPACE_STATE: Int = 2
    const val SCALE_INDEX_REVEAL_ANIM: Int = 3
    const val SCALE_INDEX_WIDGET_TRANSITION: Int = 4
    const val SCALE_INDEX_FOLDER_ANIM: Int = 5

    @JvmField
    val DRAWABLE_ALPHA: IntProperty<Drawable> = object : IntProperty<Drawable>("drawableAlpha") {
        override fun get(drawable: Drawable): Int = drawable.alpha

        override fun setValue(drawable: Drawable, alpha: Int) {
            drawable.alpha = alpha
        }
    }

    @JvmField
    val SCALE_PROPERTY: FloatProperty<View> = getScaleProperty()

    @JvmStatic
    fun getScaleProperty(): FloatProperty<View> = ScaleProperty()

    private class ScaleProperty : FloatProperty<View>("scale") {
        override fun get(view: View): Float = view.scaleX

        override fun setValue(view: View, scale: Float) {
            val handler = view.handler
            if (handler != null && handler.looper != Looper.myLooper()) {
                view.post {
                    view.scaleX = scale
                    view.scaleY = scale
                }
            } else {
                view.scaleX = scale
                view.scaleY = scale
            }
        }
    }

    @JvmStatic
    fun blockedFlingDurationFactor(velocity: Float): Int {
        return Utilities.boundToRange(abs(velocity) / 2f, 2f, 6f).toInt()
    }

    @JvmField
    val LAYOUT_WIDTH: IntProperty<LayoutParams> = object : IntProperty<LayoutParams>("width") {
        override fun get(lp: LayoutParams): Int = lp.width

        override fun setValue(lp: LayoutParams, width: Int) {
            lp.width = width
        }
    }

    @JvmField
    val LAYOUT_HEIGHT: IntProperty<LayoutParams> = object : IntProperty<LayoutParams>("height") {
        override fun get(lp: LayoutParams): Int = lp.height

        override fun setValue(lp: LayoutParams, height: Int) {
            lp.height = height
        }
    }

    @JvmField
    val TEXT_COLOR: IntProperty<TextView> = object : IntProperty<TextView>("textColor") {
        override fun get(view: TextView): Int = view.textColors.defaultColor

        override fun setValue(view: TextView, color: Int) {
            view.setTextColor(color)
        }
    }

    @JvmField
    val HINT_TEXT_COLOR: IntProperty<TextView> = object : IntProperty<TextView>("hintTextColor") {
        override fun get(view: TextView): Int = view.hintTextColors.defaultColor

        override fun setValue(view: TextView, color: Int) {
            view.setHintTextColor(color)
        }
    }

    @JvmField
    val VIEW_TRANSLATE_X: FloatProperty<View> = object : FloatProperty<View>("translationX") {
        override fun setValue(target: View, value: Float) {
            target.translationX = value
        }

        override fun get(target: View): Float = target.translationX
    }

    @JvmField
    val VIEW_TRANSLATE_Y: FloatProperty<View> = object : FloatProperty<View>("translationY") {
        override fun setValue(target: View, value: Float) {
            target.translationY = value
        }

        override fun get(target: View): Float = target.translationY
    }

    @JvmField
    val VIEW_TRANSLATE_Z: FloatProperty<View> = object : FloatProperty<View>("translationZ") {
        override fun setValue(target: View, value: Float) {
            target.translationZ = value
        }

        override fun get(target: View): Float = target.translationZ
    }

    @JvmField
    val VIEW_ALPHA: FloatProperty<View> = object : FloatProperty<View>("alpha") {
        override fun setValue(target: View, value: Float) {
            target.alpha = value
        }

        override fun get(target: View): Float = target.alpha
    }

    @JvmField
    val VIEW_BACKGROUND_COLOR: IntProperty<View> = object : IntProperty<View>("backgroundColor") {
        override fun setValue(target: View, value: Int) {
            target.setBackgroundColor(value)
        }

        override fun get(target: View): Int {
            val bg = target.background
            return if (bg is ColorDrawable) bg.color else Color.TRANSPARENT
        }
    }

    @JvmField
    val ROTATION_DRAWABLE_PERCENT: FloatProperty<ImageView> = object : FloatProperty<ImageView>("drawableRotationPercent") {
        private val MAX_LEVEL = 10000

        override fun setValue(view: ImageView, percent: Float) {
            view.setImageLevel((percent * MAX_LEVEL).toInt())
        }

        override fun get(view: ImageView): Float {
            val drawable = view.drawable ?: return 0f
            return drawable.level.toFloat() / MAX_LEVEL.toFloat()
        }
    }

    @JvmStatic
    fun newSingleUseCancelListener(callback: Runnable): Animator.AnimatorListener {
        return newCancelListener(callback, true)
    }

    @JvmStatic
    fun newCancelListener(callback: Runnable, isSingleUse: Boolean): Animator.AnimatorListener {
        return object : AnimatorListenerAdapter() {
            private var dispatched = false

            override fun onAnimationCancel(animation: Animator) {
                if (!dispatched) {
                    if (isSingleUse) {
                        dispatched = true
                    }
                    callback.run()
                }
            }
        }
    }

    @JvmStatic
    fun getPosProviderForRect(start: RectF, target: RectF): (RectF) -> Float {
        val totalXDiff = abs(start.centerX() - target.centerX())
        val totalYDiff = abs(start.centerY() - target.centerY())
        return if (totalYDiff > totalXDiff) { rect -> rect.centerY() } else { rect -> rect.centerX() }
    }

    open class ClampedProperty<T>(
        private val property: FloatProperty<T>,
        private val minValue: Float,
        private val maxValue: Float
    ) : FloatProperty<T>(property.name + "Clamped") {

        override fun setValue(target: T, value: Float) {
            property.set(target, Utilities.boundToRange(value, minValue, maxValue))
        }

        override fun get(target: T): Float = property.get(target)
    }
}
