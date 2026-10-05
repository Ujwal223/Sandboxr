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

package com.sandboxr.launcher.taskbar

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import com.sandboxr.launcher.R

/**
 * Renderer for the taskbar's background pill, corner curves, and glass highlights.
 */
class TaskbarBackgroundRenderer(val context: TaskbarActivityContext) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.taskbar_background)
    }

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = ContextCompat.getColor(context, R.color.taskbar_stroke)
    }

    fun draw(canvas: Canvas, bounds: RectF, cornerRadius: Float) {
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint)
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, strokePaint)
    }
}
