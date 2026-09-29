/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.sandboxr.launcher.allapps

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.annotation.ColorInt

/**
 * A transparent overlay view that draws a gradient scrim below the [FloatingHeaderView]
 * to visually separate the floating header from the app list behind it when it floats.
 *
 * The scrim uses a subtle top-to-bottom alpha gradient from the header background color
 * to transparent, creating a smooth floating effect with the Liquid Glass aesthetic.
 */
class FloatingMaskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint()
    private var gradient: LinearGradient? = null

    @ColorInt
    private var maskColor: Int = 0xFF0D0D14.toInt()

    private var visibilityAnim: ValueAnimator? = null
    private var floatingProgress: Float = 0f

    init {
        setWillNotDraw(false)
        // start invisible
        alpha = 0f
    }

    /**
     * Sets the color used for the top-most pixel of the gradient mask.
     */
    fun setMaskColor(@ColorInt color: Int) {
        maskColor = color
        gradient = null
        invalidate()
    }

    /**
     * Updates the floating progress (0f = docked / hidden, 1f = floating / fully shown).
     */
    fun setFloatingProgress(progress: Float, animate: Boolean = false) {
        val clamped = progress.coerceIn(0f, 1f)
        if (animate) {
            visibilityAnim?.cancel()
            visibilityAnim = ValueAnimator.ofFloat(floatingProgress, clamped).apply {
                duration = 150
                interpolator = AccelerateInterpolator()
                addUpdateListener {
                    floatingProgress = it.animatedValue as Float
                    this@FloatingMaskView.alpha = floatingProgress
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        floatingProgress = clamped
                        this@FloatingMaskView.alpha = floatingProgress
                    }
                })
                start()
            }
        } else {
            visibilityAnim?.cancel()
            floatingProgress = clamped
            this.alpha = floatingProgress
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gradient = null
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return

        if (gradient == null) {
            // Build top-opaque to bottom-transparent gradient for floating mask
            val topColor = maskColor
            val bottomColor = topColor and 0x00FFFFFF // fully transparent, same RGB
            gradient = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(topColor, topColor, bottomColor),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}
