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

package com.sandboxr.launcher.shapes

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * High-precision Lamé curve superellipse shape generator.
 * Follows equation: |x/a|^n + |y/b|^n = 1
 * Default n=4.0 produces the modern squircle contour used throughout Sandboxr UI.
 */
class SuperellipseShape(
    val exponent: Float = 4.0f,
    val samplePoints: Int = 72
) {
    fun createPath(width: Float, height: Float, offsetX: Float = 0f, offsetY: Float = 0f): Path {
        val path = Path()
        val a = width / 2f
        val b = height / 2f
        val cx = offsetX + a
        val cy = offsetY + b
        val p = 2f / exponent

        val step = (2.0 * PI / samplePoints).toFloat()
        for (i in 0 until samplePoints) {
            val theta = i * step
            val cosT = cos(theta)
            val sinT = sin(theta)
            val x = cx + a * sign(cosT) * abs(cosT).pow(p)
            val y = cy + b * sign(sinT) * abs(sinT).pow(p)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return path
    }

    fun draw(canvas: Canvas, bounds: RectF, paint: Paint) {
        val path = createPath(bounds.width(), bounds.height(), bounds.left, bounds.top)
        canvas.drawPath(path, paint)
    }

    companion object {
        /** Standardized 100x100 SVG path for a continuous superellipse squircle */
        const val SQUIRCLE_PATH =
            "M 50,0 C 77.6,0 100,22.4 100,50 C 100,77.6 77.6,100 50,100 C 22.4,100 0,77.6 0,50 C 0,22.4 22.4,0 50,0 Z"

        val DEFAULT = SuperellipseShape()
    }
}
