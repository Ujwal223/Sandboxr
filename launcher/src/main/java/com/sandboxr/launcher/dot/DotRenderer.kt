/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.sandboxr.launcher.dot

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.min

/**
 * Renders the notification badge dot over an icon's compound drawable bounds.
 */
open class DotRenderer(val size: Int = 16) {

    open class DrawParams {
        @JvmField val iconBounds: Rect = Rect()
        @JvmField var color: Int = Color.TRANSPARENT
        @JvmField var appColor: Int = Color.TRANSPARENT
        @JvmField var dotColor: Int = Color.TRANSPARENT
        @JvmField var scale: Float = 0f
    }

    private val mCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    open fun draw(canvas: Canvas, params: DrawParams, count: Int = 0) {
        if (params.scale <= 0f) return

        val dotRadius = (size / 2f) * params.scale
        // Position at top-right of the icon
        val cx = params.iconBounds.right.toFloat() - dotRadius
        val cy = params.iconBounds.top.toFloat() + dotRadius

        val dotColor = if (params.dotColor != Color.TRANSPARENT) {
            params.dotColor
        } else if (params.color != Color.TRANSPARENT) {
            params.color
        } else {
            Color.parseColor("#FF5252") // vibrant notification red fallback
        }

        mCirclePaint.color = dotColor
        canvas.drawCircle(cx, cy, dotRadius, mCirclePaint)

        // Draw notification badge count if provided and large enough
        if (count > 0 && params.scale >= 0.8f && dotRadius >= 10f) {
            mTextPaint.textSize = dotRadius * 1.2f
            val text = if (count > 99) "99+" else count.toString()
            val textY = cy - ((mTextPaint.descent() + mTextPaint.ascent()) / 2f)
            canvas.drawText(text, cx, textY, mTextPaint)
        }
    }
}
