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
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import com.sandboxr.launcher.views.BaseDragLayer

/**
 * Top-level ViewGroup hosting TaskbarView, StashedHandleView, Nav buttons, and Bubbles.
 */
class TaskbarDragLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : BaseDragLayer<TaskbarActivityContext>(context, attrs) {

    private val backgroundBounds = RectF()
    var taskbarBackgroundAlpha: Float = 1f
        set(value) {
            field = value
            invalidate()
        }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        backgroundBounds.set(l.toFloat(), t.toFloat(), r.toFloat(), b.toFloat())
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return super.onInterceptTouchEvent(ev)
    }
}
