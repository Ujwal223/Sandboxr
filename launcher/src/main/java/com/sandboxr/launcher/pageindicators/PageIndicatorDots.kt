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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.FloatProperty
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.OvershootInterpolator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Animated dot indicator for home screen workspace pages.
 * Draws subtle dots for each screen and an active sliding pill/circle with overshoot bounce physics.
 */
open class PageIndicatorDots @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), PageIndicator {

    private val mPaginationPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private var mDotRadius: Float = 8f
    private var mGapWidth: Float = 14f
    private var mCircleGap: Float = mDotRadius * 2 + mGapWidth

    private var mNumPages: Int = 0
    private var mActivePage: Int = 0
    private var mTotalScroll: Int = 0
    private var mShouldAutoHide: Boolean = false
    private var mIsMoveAnimationQueued: Boolean = false

    var currentPosition: Float = 0f
        set(value) {
            field = value
            invalidate()
            invalidateOutline()
        }

    private var mLastPosition: Int = 0
    private var mFinalPosition: Float = 0f
    private var mIsScrollPaused: Boolean = false
    var isTwoPanels: Boolean = false
    private var mAnimator: ObjectAnimator? = null
    private var mEntryAnimationRadiusFactors: FloatArray? = null

    private val mDelayedPaginationFadeHandler = Handler(Looper.getMainLooper())
    private val mHidePaginationRunnable = Runnable { animatePaginationToAlpha(0) }

    private val mTempRect = RectF()

    init {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                if (mNumPages > 1) {
                    outline.setRoundRect(
                        0,
                        (height / 2f - mDotRadius).toInt(),
                        width,
                        (height / 2f + mDotRadius).toInt(),
                        mDotRadius
                    )
                }
            }
        }
    }

    override fun setScroll(currentScroll: Int, totalScroll: Int) {
        if (currentScroll == 0 && totalScroll == 0) {
            CURRENT_POSITION.set(this, mActivePage.toFloat())
            return
        }
        if (mNumPages <= 1 || mIsScrollPaused) return

        if (mShouldAutoHide) {
            animatePaginationToAlpha(255)
        }

        mTotalScroll = totalScroll
        val scrollPerPage = totalScroll.toFloat() / (mNumPages - 1).coerceAtLeast(1)
        val position = (currentScroll / scrollPerPage).coerceIn(0f, (mNumPages - 1).toFloat())

        if (mIsMoveAnimationQueued) {
            currentPosition = position
            if (abs(mActivePage - position) <= 1f) {
                animateToPosition(mActivePage.toFloat())
            }
        } else if (mAnimator == null) {
            if (abs(mLastPosition - position) > 1f) {
                mLastPosition = position.roundToInt()
            }
            mFinalPosition = mLastPosition + if (position > mLastPosition) 1f else -1f
            CURRENT_POSITION.set(this, position)
        }

        val delta = abs(position.toInt() - position)
        if (mShouldAutoHide && !mIsMoveAnimationQueued && (delta < 0.1f || delta > 0.9f)) {
            hideAfterDelay()
        }
    }

    override fun setActiveMarker(activePage: Int) {
        var page = activePage
        if (isTwoPanels) {
            page /= 2
        }

        if (mActivePage != page) {
            mLastPosition = mActivePage
            mActivePage = page

            if (abs(mActivePage - currentPosition) <= 1f) {
                animateToPosition(page.toFloat())
            } else {
                mFinalPosition = page.toFloat()
                mIsMoveAnimationQueued = true
            }
        }
    }

    override fun setMarkersCount(numMarkers: Int) {
        mNumPages = numMarkers
        if (mNumPages > 0 && mNumPages <= mActivePage) {
            mActivePage = mNumPages - 1
            CURRENT_POSITION.set(this, mActivePage.toFloat())
        }
        requestLayout()
        invalidate()
    }

    override fun setPauseScroll(pause: Boolean, isTwoPanels: Boolean) {
        this.isTwoPanels = isTwoPanels
        if (mIsScrollPaused && !pause) {
            CURRENT_POSITION.set(this, mActivePage.toFloat())
        }
        mIsScrollPaused = pause
    }

    override fun setShouldAutoHide(shouldAutoHide: Boolean) {
        mShouldAutoHide = shouldAutoHide
        if (shouldAutoHide && mPaginationPaint.alpha > 0) {
            hideAfterDelay()
        } else if (!shouldAutoHide) {
            mDelayedPaginationFadeHandler.removeCallbacksAndMessages(null)
            animatePaginationToAlpha(255)
        }
    }

    override fun setPaintColor(color: Int) {
        mPaginationPaint.color = color
        invalidate()
    }

    private fun hideAfterDelay() {
        mDelayedPaginationFadeHandler.removeCallbacksAndMessages(null)
        mDelayedPaginationFadeHandler.postDelayed(mHidePaginationRunnable, 1000L)
    }

    private fun animatePaginationToAlpha(alpha: Int) {
        ObjectAnimator.ofInt(this, "paginationAlpha", alpha).apply {
            duration = 160L
            addUpdateListener {
                mPaginationPaint.alpha = it.animatedValue as Int
                invalidate()
            }
        }.start()
    }

    fun animateToPosition(position: Float) {
        mFinalPosition = position
        mAnimator?.cancel()
        val anim = ObjectAnimator.ofFloat(this, CURRENT_POSITION, position)
        mAnimator = anim
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mAnimator = null
                mLastPosition = position.roundToInt()
            }
        })
        val remainingScroll = min(1f, abs(position - currentPosition))
        anim.duration = (ANIMATION_DURATION * remainingScroll).toLong()
        anim.interpolator = OvershootInterpolator(ENTER_ANIMATION_OVERSHOOT_TENSION)
        mIsMoveAnimationQueued = false
        anim.start()
    }

    fun stopAllAnimations() {
        mAnimator?.cancel()
        mAnimator = null
        mFinalPosition = mActivePage.toFloat()
        CURRENT_POSITION.set(this, mFinalPosition)
    }

    fun prepareEntryAnimation() {
        mEntryAnimationRadiusFactors = FloatArray(mNumPages) { 0f }
        invalidate()
    }

    fun playEntryAnimation() {
        val count = mEntryAnimationRadiusFactors?.size ?: return
        val animSet = AnimatorSet()
        val interpolator = OvershootInterpolator(ENTER_ANIMATION_OVERSHOOT_TENSION)

        for (i in 0 until count) {
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 350L
                startDelay = (200 + i * 80).toLong()
                this.interpolator = interpolator
                addUpdateListener {
                    mEntryAnimationRadiusFactors?.set(i, it.animatedValue as Float)
                    invalidate()
                }
            }
            animSet.play(anim)
        }

        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mEntryAnimationRadiusFactors = null
                invalidateOutline()
                invalidate()
            }
        })
        animSet.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = if (mNumPages <= 1) {
            0
        } else {
            val totalDotsWidth = (mNumPages - 1) * mGapWidth + (1 + mNumPages) * mDotRadius * 2
            totalDotsWidth.toInt() + paddingLeft + paddingRight
        }
        val height = (mDotRadius * 4).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mNumPages < 2) return

        val totalDotsWidth = (mNumPages - 1) * mGapWidth + (1 + mNumPages) * mDotRadius * 2
        var x = (width - totalDotsWidth) / 2f + mDotRadius
        val y = height / 2f
        val alpha = mPaginationPaint.alpha

        val entryFactors = mEntryAnimationRadiusFactors
        if (entryFactors != null) {
            for (i in 0 until mNumPages) {
                val factor = entryFactors.getOrElse(i) { 1f }
                if (i == mActivePage) {
                    mPaginationPaint.alpha = alpha
                    canvas.drawCircle(x, y, mDotRadius * factor * 1.3f, mPaginationPaint)
                } else {
                    mPaginationPaint.alpha = (alpha * 0.45f).toInt()
                    canvas.drawCircle(x, y, mDotRadius * factor, mPaginationPaint)
                }
                x += mCircleGap
            }
            return
        }

        val nonActiveAlpha = (alpha * 0.45f).toInt()
        val diameter = 2 * mDotRadius
        mTempRect.top = y - mDotRadius
        mTempRect.bottom = y + mDotRadius
        mTempRect.left = x - mDotRadius

        val progress = abs(currentPosition - mLastPosition) / max(1f, abs(mFinalPosition - mLastPosition))
        val bounceAdjustment = max(progress - 1f, 0f) * diameter
        val alphaAdjustment = (min(progress, 1f) * (alpha - nonActiveAlpha)).toInt()

        for (i in 0 until mNumPages) {
            val dotAlpha = when (i) {
                mLastPosition -> alpha - alphaAdjustment
                mFinalPosition.toInt() -> nonActiveAlpha + alphaAdjustment
                else -> nonActiveAlpha
            }
            mPaginationPaint.alpha = dotAlpha

            val stretch = diameter * when (i) {
                mLastPosition -> 1f - progress
                mFinalPosition.toInt() -> progress
                else -> 0f
            }
            mTempRect.right = mTempRect.left + diameter + stretch

            if (bounceAdjustment > 0f) {
                if (mFinalPosition.toInt() == i) {
                    if (mLastPosition < mFinalPosition) {
                        mTempRect.left -= bounceAdjustment
                    } else {
                        mTempRect.right += bounceAdjustment
                    }
                }
            }

            canvas.drawRoundRect(mTempRect, mDotRadius, mDotRadius, mPaginationPaint)
            mTempRect.left = mTempRect.right + mGapWidth
        }
    }

    companion object {
        private const val ANIMATION_DURATION = 200L
        private const val ENTER_ANIMATION_OVERSHOOT_TENSION = 4.9f

        @JvmField
        val CURRENT_POSITION: FloatProperty<PageIndicatorDots> =
            object : FloatProperty<PageIndicatorDots>("current_position") {
                override fun get(obj: PageIndicatorDots): Float = obj.currentPosition
                override fun setValue(obj: PageIndicatorDots, value: Float) {
                    obj.currentPosition = value
                }
            }
    }
}
