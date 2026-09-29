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

package com.sandboxr.launcher.views

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextUtils
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.sandboxr.launcher.FastScrollRecyclerView

/**
 * Fast scroller view displaying a thumb track along the right edge and an animated
 * letter overlay popup while scrubbing.
 */
class RecyclerViewFastScroller @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private val minWidth = (24 * density).toInt()
    private val maxWidth = (32 * density).toInt()
    private val thumbHeight = (56 * density).toInt()
    private val thumbWidth = (4 * density).toInt()
    private val thumbExpandedWidth = (8 * density).toInt()

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9080A0FF") // Glass accent blue
        style = Paint.Style.FILL
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1FFFFFFF") // Translucent subtle track
        style = Paint.Style.FILL
    }

    private val thumbBounds = RectF()
    private var thumbOffsetY: Int = -1
    private var isDragging: Boolean = false
    private var lastTouchY: Float = 0f

    private var recyclerView: FastScrollRecyclerView? = null
    private var popupView: TextView? = null
    private var popupVisible: Boolean = false
    private var currentSectionName: CharSequence = ""

    init {
        isFocusable = false
    }

    fun setRecyclerView(rv: FastScrollRecyclerView) {
        recyclerView = rv
    }

    fun setPopupView(view: TextView?) {
        popupView = view
        popupView?.alpha = 0f
    }

    fun getThumbHeight(): Int = thumbHeight

    fun isDraggingThumb(): Boolean = isDragging

    fun setThumbOffsetY(y: Int) {
        if (thumbOffsetY != y) {
            thumbOffsetY = y
            invalidate()
        }
    }

    fun getThumbOffsetY(): Int = thumbOffsetY

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (thumbOffsetY < 0) return

        val rv = recyclerView ?: return
        val top = rv.getScrollBarTop()
        val trackHeight = rv.getScrollbarTrackHeight()
        val right = width.toFloat() - (4 * density)
        val currentThumbWidth = if (isDragging) thumbExpandedWidth.toFloat() else thumbWidth.toFloat()
        val left = right - currentThumbWidth

        // Draw track
        canvas.drawRoundRect(
            left + (currentThumbWidth / 4),
            top.toFloat(),
            right - (currentThumbWidth / 4),
            (top + trackHeight).toFloat(),
            currentThumbWidth,
            currentThumbWidth,
            trackPaint
        )

        // Draw thumb
        val thumbTop = top + thumbOffsetY.toFloat()
        val thumbBottom = thumbTop + thumbHeight
        thumbBounds.set(left, thumbTop, right, thumbBottom)
        val cornerRadius = currentThumbWidth / 2
        canvas.drawRoundRect(thumbBounds, cornerRadius, cornerRadius, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rv = recyclerView ?: return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = event.y
                val top = rv.getScrollBarTop()
                val trackHeight = rv.getScrollbarTrackHeight()

                // Check if touch is within scrollbar bounds
                val boundedY = maxOf(top.toFloat(), minOf((top + trackHeight).toFloat(), event.y))
                val touchFraction = if (trackHeight > 0) (boundedY - top) / trackHeight else 0f

                isDragging = true
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                parent?.requestDisallowInterceptTouchEvent(true)
                updateScrollPosition(touchFraction, event.y)
                animatePopupVisibility(true)
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val top = rv.getScrollBarTop()
                    val trackHeight = rv.getScrollbarTrackHeight()
                    val boundedY = maxOf(top.toFloat(), minOf((top + trackHeight).toFloat(), event.y))
                    val touchFraction = if (trackHeight > 0) (boundedY - top) / trackHeight else 0f
                    updateScrollPosition(touchFraction, event.y)
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    rv.onFastScrollCompleted()
                    animatePopupVisibility(false)
                    invalidate()
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateScrollPosition(touchFraction: Float, touchY: Float) {
        val rv = recyclerView ?: return
        val sectionName = rv.scrollToPositionAtProgress(touchFraction)
        if (!TextUtils.equals(sectionName, currentSectionName)) {
            currentSectionName = sectionName
            popupView?.text = sectionName
            if (sectionName.isNotEmpty()) {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }

        // Position popup alongside thumb
        popupView?.let { popup ->
            val popupHeight = popup.height.toFloat()
            val targetY = touchY - (popupHeight / 2)
            popup.translationY = maxOf(0f, minOf(height.toFloat() - popupHeight, targetY))
        }
    }

    private fun animatePopupVisibility(visible: Boolean) {
        if (popupVisible != visible) {
            popupVisible = visible
            popupView?.animate()?.cancel()
            popupView?.animate()
                ?.alpha(if (visible) 1f else 0f)
                ?.scaleX(if (visible) 1f else 0.8f)
                ?.scaleY(if (visible) 1f else 0.8f)
                ?.setDuration(150)
                ?.start()
        }
    }
}
