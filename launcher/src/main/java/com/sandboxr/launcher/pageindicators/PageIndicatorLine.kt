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

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View

/**
 * Continuous line page indicator with an active sliding indicator segment and auto-hide fading.
 */
open class PageIndicatorLine @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), PageIndicator {

    private val mTrackPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#33FFFFFF")
    }

    private val mActiveLinePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private var mNumPages: Int = 0
    private var mActivePage: Int = 0
    private var mCurrentScrollRatio: Float = 0f
    private var mShouldAutoHide: Boolean = false
    private val mHandler = Handler(Looper.getMainLooper())
    private val mHideRunnable = Runnable { animateAlpha(0f) }

    private val mLineHeight: Float = 4f
    private val mLineCornerRadius: Float = 2f

    override fun setScroll(currentScroll: Int, totalScroll: Int) {
        if (totalScroll > 0) {
            mCurrentScrollRatio = (currentScroll.toFloat() / totalScroll.toFloat()).coerceIn(0f, 1f)
            if (mShouldAutoHide) {
                animateAlpha(1f)
                mHandler.removeCallbacks(mHideRunnable)
                mHandler.postDelayed(mHideRunnable, 1000L)
            }
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

    override fun setShouldAutoHide(shouldAutoHide: Boolean) {
        mShouldAutoHide = shouldAutoHide
        if (shouldAutoHide) {
            mHandler.postDelayed(mHideRunnable, 1000L)
        } else {
            mHandler.removeCallbacks(mHideRunnable)
            alpha = 1f
        }
    }

    override fun setPaintColor(color: Int) {
        mActiveLinePaint.color = color
        mTrackPaint.color = (color and 0x00FFFFFF) or 0x33000000
        invalidate()
    }

    private fun animateAlpha(targetAlpha: Float) {
        ObjectAnimator.ofFloat(this, ALPHA, targetAlpha).setDuration(200L).start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (mLineHeight * 4).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mNumPages <= 1) return

        val w = (width - paddingLeft - paddingRight).toFloat()
        val h = mLineHeight
        val top = (height - h) / 2f
        val bottom = top + h

        // Draw track
        val trackRect = RectF(paddingLeft.toFloat(), top, paddingLeft + w, bottom)
        canvas.drawRoundRect(trackRect, mLineCornerRadius, mLineCornerRadius, mTrackPaint)

        // Draw active segment
        val segmentWidth = w / mNumPages
        val startX = paddingLeft + (mCurrentScrollRatio * (w - segmentWidth))
        val activeRect = RectF(startX, top, startX + segmentWidth, bottom)
        canvas.drawRoundRect(activeRect, mLineCornerRadius, mLineCornerRadius, mActiveLinePaint)
    }
}
