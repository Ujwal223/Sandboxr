/*
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

package com.sandboxr.launcher.splitscreen

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.sandboxr.launcher.R

/**
 * Custom view placed between two split-screen tasks in overview or preview.
 * Displays a center pill / handle and handles dragging to adjust the split ratio.
 */
class SplitDividerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnDividerDragListener {
        fun onDividerDragged(ratio: Float)
        fun onDividerDragFinished(ratio: Float)
    }

    var isHorizontalSplit: Boolean = false
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var splitRatio: Float = 0.5f
        set(value) {
            field = value.coerceIn(0.2f, 0.8f)
            invalidate()
        }

    var dragListener: OnDividerDragListener? = null

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.materialColorOutline)
        style = Paint.Style.FILL
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.materialColorSurfaceContainerHighest)
        style = Paint.Style.FILL
    }

    private val handleBounds = RectF()
    private var initialTouchPos: Float = 0f
    private var initialRatio: Float = 0.5f
    private var isDragging: Boolean = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw background bar
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Draw center rounded pill handle
        val handleThickness = resources.getDimension(R.dimen.split_divider_handle_thickness)
        val handleLength = resources.getDimension(R.dimen.split_divider_handle_length)
        val handleRadius = resources.getDimension(R.dimen.split_divider_handle_radius)

        if (isHorizontalSplit) {
            // Divider is vertical (splitting left and right tasks)
            val centerX = width / 2f
            val centerY = height / 2f
            handleBounds.set(
                centerX - handleThickness / 2f,
                centerY - handleLength / 2f,
                centerX + handleThickness / 2f,
                centerY + handleLength / 2f
            )
        } else {
            // Divider is horizontal (splitting top and bottom tasks)
            val centerX = width / 2f
            val centerY = height / 2f
            handleBounds.set(
                centerX - handleLength / 2f,
                centerY - handleThickness / 2f,
                centerX + handleLength / 2f,
                centerY + handleThickness / 2f
            )
        }

        canvas.drawRoundRect(handleBounds, handleRadius, handleRadius, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                initialTouchPos = if (isHorizontalSplit) event.rawX else event.rawY
                initialRatio = splitRatio
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val currentPos = if (isHorizontalSplit) event.rawX else event.rawY
                    val delta = currentPos - initialTouchPos
                    val parentSize = if (isHorizontalSplit) (parent as? View)?.width ?: width else (parent as? View)?.height ?: height
                    if (parentSize > 0) {
                        val deltaRatio = delta / parentSize.toFloat()
                        splitRatio = (initialRatio + deltaRatio).coerceIn(0.2f, 0.8f)
                        dragListener?.onDividerDragged(splitRatio)
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    dragListener?.onDividerDragFinished(splitRatio)
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
