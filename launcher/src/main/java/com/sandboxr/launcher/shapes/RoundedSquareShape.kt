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

/**
 * Geometric shape generator for rounded squares with configurable corner radius ratio.
 */
class RoundedSquareShape(
    val radiusRatio: Float = 0.33f
) {
    fun createPath(width: Float, height: Float, offsetX: Float = 0f, offsetY: Float = 0f): Path {
        val path = Path()
        val rect = RectF(offsetX, offsetY, offsetX + width, offsetY + height)
        val radius = minOf(width, height) * radiusRatio
        path.addRoundRect(rect, radius, radius, Path.Direction.CW)
        return path
    }

    fun draw(canvas: Canvas, bounds: RectF, paint: Paint) {
        val radius = minOf(bounds.width(), bounds.height()) * radiusRatio
        canvas.drawRoundRect(bounds, radius, radius, paint)
    }

    companion object {
        const val SQUARE_PATH =
            "M53.689 0.82 L53.689 .82 C67.434 .82 74.306 .82 79.758 2.978 87.649 6.103 93.897 12.351 97.022 20.242 99.18 25.694 99.18 32.566 99.18 46.311 V53.689 C99.18 67.434 99.18 74.306 97.022 79.758 93.897 87.649 87.649 93.897 79.758 97.022 74.306 99.18 67.434 99.18 53.689 99.18 H46.311 C32.566 99.18 25.694 99.18 20.242 97.022 12.351 93.897 6.103 87.649 2.978 79.758 .82 74.306 .82 67.434 .82 53.689 L.82 46.311 C.82 32.566 .82 25.694 2.978 20.242 6.103 12.351 12.351 6.103 20.242 2.978 25.694 .82 32.566 .82 46.311 .82Z"

        val DEFAULT = RoundedSquareShape()
    }
}
