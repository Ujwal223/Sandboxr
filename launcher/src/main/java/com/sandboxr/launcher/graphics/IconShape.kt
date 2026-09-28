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

package com.sandboxr.launcher.graphics

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.view.View
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import com.sandboxr.launcher.util.DaggerSingletonObject
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Encapsulates the geometric shape masking and animations for launcher icons.
 * Supports Circle, Rounded Square, Squircle (Superellipse), and TearDrop.
 */
@LauncherAppSingleton
class IconShape @Inject constructor(
    @ApplicationContext context: Context? = null,
) {
    var delegate: ShapeDelegate = Circle()
        private set

    var normalizationScale: Float = 0.92f
        private set

    init {
        if (context != null) {
            pickBestShape(context)
        }
    }

    fun pickBestShape(context: Context) {
        val res = context.resources
        val maskResId = Resources.getSystem().getIdentifier("config_icon_mask", "string", "android")
        val maskStr = if (maskResId != 0) res.getString(maskResId) else ""

        delegate = when {
            maskStr.contains("squircle", ignoreCase = true) -> Squircle()
            maskStr.contains("teardrop", ignoreCase = true) -> TearDrop()
            maskStr.contains("rounded", ignoreCase = true) -> RoundedSquare(0.4f)
            else -> Circle()
        }
    }

    interface ShapeDelegate {
        fun drawShape(canvas: Canvas, offsetX: Float, offsetY: Float, radius: Float, paint: Paint)
        fun addToPath(path: Path, offsetX: Float, offsetY: Float, radius: Float)
        fun getPath(radius: Float): Path {
            val p = Path()
            addToPath(p, 0f, 0f, radius)
            return p
        }
        fun createRevealAnimator(
            target: View,
            startRect: Rect,
            endRect: Rect,
            endRadius: Float,
            isReversed: Boolean,
        ): ValueAnimator {
            val anim = if (isReversed) ValueAnimator.ofFloat(1f, 0f) else ValueAnimator.ofFloat(0f, 1f)
            anim.addUpdateListener { va ->
                val progress = va.animatedValue as Float
                target.scaleX = (1 - progress) + progress * (endRect.width().toFloat() / startRect.width().toFloat())
                target.scaleY = (1 - progress) + progress * (endRect.height().toFloat() / startRect.height().toFloat())
            }
            return anim
        }
    }

    class Circle : ShapeDelegate {
        override fun drawShape(canvas: Canvas, offsetX: Float, offsetY: Float, radius: Float, paint: Paint) {
            canvas.drawCircle(radius + offsetX, radius + offsetY, radius, paint)
        }

        override fun addToPath(path: Path, offsetX: Float, offsetY: Float, radius: Float) {
            path.addCircle(radius + offsetX, radius + offsetY, radius, Path.Direction.CW)
        }
    }

    class RoundedSquare(val radiusRatio: Float = 0.35f) : ShapeDelegate {
        override fun drawShape(canvas: Canvas, offsetX: Float, offsetY: Float, radius: Float, paint: Paint) {
            val cx = radius + offsetX
            val cy = radius + offsetY
            val cr = radius * radiusRatio
            canvas.drawRoundRect(cx - radius, cy - radius, cx + radius, cy + radius, cr, cr, paint)
        }

        override fun addToPath(path: Path, offsetX: Float, offsetY: Float, radius: Float) {
            val cx = radius + offsetX
            val cy = radius + offsetY
            val cr = radius * radiusRatio
            path.addRoundRect(cx - radius, cy - radius, cx + radius, cy + radius, cr, cr, Path.Direction.CW)
        }
    }

    /**
     * Parametric Continuous Superellipse (Lamé curve) Squircle:
     * |x/a|^n + |y/b|^n = 1 (n=4)
     */
    class Squircle(val exponent: Float = 4.0f) : ShapeDelegate {
        override fun drawShape(canvas: Canvas, offsetX: Float, offsetY: Float, radius: Float, paint: Paint) {
            val path = Path()
            addToPath(path, offsetX, offsetY, radius)
            canvas.drawPath(path, paint)
        }

        override fun addToPath(path: Path, offsetX: Float, offsetY: Float, radius: Float) {
            val cx = radius + offsetX
            val cy = radius + offsetY
            val p = 2.0f / exponent
            val steps = 48
            val dt = (2.0 * PI) / steps

            for (i in 0..steps) {
                val t = i * dt
                val cosT = cos(t).toFloat()
                val sinT = sin(t).toFloat()
                val x = cx + radius * sign(cosT) * abs(cosT).pow(p)
                val y = cy + radius * sign(sinT) * abs(sinT).pow(p)

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()
        }
    }

    class TearDrop(val radiusRatio: Float = 0.3f) : ShapeDelegate {
        override fun drawShape(canvas: Canvas, offsetX: Float, offsetY: Float, radius: Float, paint: Paint) {
            val path = Path()
            addToPath(path, offsetX, offsetY, radius)
            canvas.drawPath(path, paint)
        }

        override fun addToPath(path: Path, offsetX: Float, offsetY: Float, radius: Float) {
            val cx = radius + offsetX
            val cy = radius + offsetY
            val r1 = radius
            val r2 = radius * radiusRatio
            val radii = floatArrayOf(r1, r1, r1, r1, r2, r2, r1, r1)
            path.addRoundRect(cx - r1, cy - r1, cx + r1, cy + r1, radii, Path.Direction.CW)
        }
    }

    companion object {
        @JvmField
        val INSTANCE = DaggerSingletonObject { IconShape(it.applicationContext()) }
    }
}
