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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/** Scrim view behind taskbar or when taskbar is in overview. */
class TaskbarScrimView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val scrimPaint = Paint().apply {
        color = Color.BLACK
        alpha = 0
    }

    var scrimAlpha: Float = 0f
        set(value) {
            field = value
            scrimPaint.alpha = (value * 255).toInt().coerceIn(0, 255)
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        if (scrimPaint.alpha > 0) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        }
    }
}

/** Controller for TaskbarScrimView. */
class TaskbarScrimViewController(
    val activity: TaskbarActivityContext,
    val scrimView: TaskbarScrimView,
) {
    fun init(controllers: TaskbarControllers) {}

    fun setScrimAlpha(alpha: Float) {
        scrimView.scrimAlpha = alpha
    }
}
