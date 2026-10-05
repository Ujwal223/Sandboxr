/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.sandboxr.launcher.dragndrop

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.sandboxr.launcher.views.ActivityContext
import com.sandboxr.launcher.views.BaseDragLayer

/**
 * Visual representation of an item being dragged across the screen.
 * Features Liquid Glass elevation with specular highlight, dynamic shadow rendering,
 * and snappy spring-physics scaling during pickup and drop.
 */
open class DragView : FrameLayout {

    protected val mActivity: ActivityContext
    protected var mDragLayer: BaseDragLayer<*>? = null

    @JvmField protected var mRegistrationX: Int = 0
    @JvmField protected var mRegistrationY: Int = 0

    @JvmField protected var mInitialScale: Float = 1.0f
    @JvmField protected var mScaleOnDrop: Float = 1.0f
    @JvmField protected var mFinalScaleDps: Float = 0f

    @JvmField protected val mTempLoc = IntArray(2)

    private var mContent: View? = null
    private var mDrawable: Drawable? = null
    private var mCurrentAnimator: ValueAnimator? = null

    // Liquid Glass Elevated Shadow & Highlight Paints
    private val mShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#44000000") // Deep shadow under elevated item
        style = Paint.Style.FILL
    }
    private val mGlintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF") // Subtle specular glint
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val mShadowBounds = RectF()
    private val mCornerRadius = 24f

    constructor(
        context: Context,
        drawable: Drawable,
        registrationX: Int,
        registrationY: Int,
        initialScale: Float,
        scaleOnDrop: Float,
        finalScaleDps: Float,
        allowSpringDrawable: Boolean
    ) : super(context) {
        mActivity = ActivityContext.lookupContext(context)
        mDragLayer = mActivity.getDragLayer()
        mDrawable = drawable
        mRegistrationX = registrationX
        mRegistrationY = registrationY
        mInitialScale = initialScale
        mScaleOnDrop = scaleOnDrop
        mFinalScaleDps = finalScaleDps

        val iv = ImageView(context).apply {
            setImageDrawable(drawable)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        mContent = iv
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 120
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 120
        layoutParams = BaseDragLayer.LayoutParams(width, height)
        addView(iv, FrameLayout.LayoutParams(width, height))
        initDragElevation()
    }

    constructor(
        context: Context,
        content: View,
        width: Int,
        height: Int,
        registrationX: Int,
        registrationY: Int,
        initialScale: Float,
        scaleOnDrop: Float,
        finalScaleDps: Float,
        allowSpringDrawable: Boolean
    ) : super(context) {
        mActivity = ActivityContext.lookupContext(context)
        mDragLayer = mActivity.getDragLayer()
        mContent = content
        mRegistrationX = registrationX
        mRegistrationY = registrationY
        mInitialScale = initialScale
        mScaleOnDrop = scaleOnDrop
        mFinalScaleDps = finalScaleDps

        layoutParams = BaseDragLayer.LayoutParams(width, height)
        addView(content, FrameLayout.LayoutParams(width, height))
        initDragElevation()
    }

    private fun initDragElevation() {
        setWillNotDraw(false)
        // Elevate with Android Material elevation and hardware shadow layer
        elevation = 28f
        translationZ = 20f

        // Initial scale-up animation with snappy spring overshoot physics
        scaleX = mInitialScale
        scaleY = mInitialScale
        val targetScale = mInitialScale * 1.12f

        val animator = ValueAnimator.ofFloat(mInitialScale, targetScale).apply {
            duration = 220
            interpolator = OvershootInterpolator(1.4f)
            addUpdateListener { va ->
                val s = va.animatedValue as Float
                scaleX = s
                scaleY = s
            }
        }
        animator.start()
        mCurrentAnimator = animator
    }

    override fun onDraw(canvas: Canvas) {
        // Draw elevated soft shadow pill behind the dragged item
        val w = width.toFloat()
        val h = height.toFloat()
        if (w > 0 && h > 0) {
            mShadowBounds.set(4f, 8f, w - 4f, h + 6f)
            canvas.drawRoundRect(mShadowBounds, mCornerRadius, mCornerRadius, mShadowPaint)
        }
        super.onDraw(canvas)
        // Draw specular edge glint
        if (w > 0 && h > 0) {
            mShadowBounds.set(1f, 1f, w - 1f, h - 1f)
            canvas.drawRoundRect(mShadowBounds, mCornerRadius, mCornerRadius, mGlintPaint)
        }
    }

    /**
     * Updates the position of the DragView in the DragLayer.
     */
    fun move(touchX: Int, touchY: Int) {
        val lp = layoutParams as? BaseDragLayer.LayoutParams ?: return
        lp.x = touchX - mRegistrationX
        lp.y = touchY - mRegistrationY
        lp.customPosition = true
        requestLayout()
    }

    open fun animateTo(toTouchX: Int, toTouchY: Int, onCompleteRunnable: Runnable?, duration: Int) {
        mTempLoc[0] = toTouchX - mRegistrationX
        mTempLoc[1] = toTouchY - mRegistrationY

        cancelAnimation()

        val startX = translationX
        val startY = translationY
        val startScaleX = scaleX
        val startScaleY = scaleY

        val targetX = mTempLoc[0].toFloat()
        val targetY = mTempLoc[1].toFloat()

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            setDuration(duration.toLong().coerceAtLeast(180L))
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener { va ->
                val f = va.animatedFraction
                translationX = startX + (targetX - startX) * f
                translationY = startY + (targetY - startY) * f
                scaleX = startScaleX + (mScaleOnDrop - startScaleX) * f
                scaleY = startScaleY + (mScaleOnDrop - startScaleY) * f
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onCompleteRunnable?.run()
                    remove()
                }
            })
        }
        mCurrentAnimator = anim
        anim.start()
    }

    fun cancelAnimation() {
        mCurrentAnimator?.cancel()
        mCurrentAnimator = null
    }

    fun remove() {
        cancelAnimation()
        if (parent != null) {
            mDragLayer?.removeView(this) ?: (parent as? ViewGroup)?.removeView(this)
        }
    }

    fun getRegistrationX(): Int = mRegistrationX
    fun getRegistrationY(): Int = mRegistrationY
    fun getContent(): View? = mContent
    fun getDrawable(): Drawable? = mDrawable

    open fun animateShift(shiftX: Int, shiftY: Int) {
        translationX += shiftX
        translationY += shiftY
    }
}
