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

package com.sandboxr.launcher.pageindicators

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Animated dot indicator for home screen workspace pages.
 * Draws subtle dots for each screen and an active sliding pill/circle.
 */
open class PageIndicatorDots @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), PageIndicator {

    private val mCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#44FFFFFF")
    }

    private val mActiveDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private var mNumPages: Int = 0
    private var mActivePage: Int = 0
    private var mCurrentScrollRatio: Float = 0f

    private val mDotRadius: Float = 8f
    private val mDotGap: Float = 18f
    private val mActivePillWidth: Float = 24f

    override fun setScroll(currentScroll: Int, totalScroll: Int) {
        if (totalScroll > 0) {
            mCurrentScrollRatio = (currentScroll.toFloat() / totalScroll.toFloat()).coerceIn(0f, 1f)
            invalidate()
        }
    }

    override fun setActiveMarker(activePage: Int) {
        if (mActivePage != activePage) {
            mActivePage = activePage
            invalidate()
        }
    }

    override fun setMarkersCount(numMarkers: Int) {
        if (mNumPages != numMarkers) {
            mNumPages = numMarkers
            requestLayout()
            invalidate()
        }
    }

    override fun setPaintColor(color: Int) {
        mActiveDotPaint.color = color
        mCirclePaint.color = (color and 0x00FFFFFF) or 0x44000000
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalWidth = if (mNumPages <= 1) {
            0
        } else {
            val dotsWidth = mNumPages * (mDotRadius * 2) + (mNumPages - 1) * mDotGap + (mActivePillWidth - mDotRadius * 2)
            dotsWidth.toInt() + paddingLeft + paddingRight
        }
        val totalHeight = (mDotRadius * 4).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSize(totalWidth, widthMeasureSpec),
            resolveSize(totalHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mNumPages <= 1) return

        val totalDotsWidth = mNumPages * (mDotRadius * 2) + (mNumPages - 1) * mDotGap
        var startX = (width - totalDotsWidth) / 2f + mDotRadius
        val centerY = height / 2f

        for (i in 0 until mNumPages) {
            canvas.drawCircle(startX, centerY, mDotRadius, mCirclePaint)
            startX += mDotRadius * 2 + mDotGap
        }

        // Draw active indicator pill
        val activeCenterOffset = mActivePage * (mDotRadius * 2 + mDotGap)
        val activeX = (width - totalDotsWidth) / 2f + mDotRadius + activeCenterOffset
        val pillRect = RectF(
            activeX - mActivePillWidth / 2f,
            centerY - mDotRadius,
            activeX + mActivePillWidth / 2f,
            centerY + mDotRadius
        )
        canvas.drawRoundRect(pillRect, mDotRadius, mDotRadius, mActiveDotPaint)
    }
}
