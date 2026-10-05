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

package com.sandboxr.launcher.desktop

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.sandboxr.launcher.R

/**
 * View representing a floating window in desktop mode or during split-to-desktop transitions.
 * Supports repositioning and resizing via touch gestures, and closing/launching animations.
 */
class FloatingDesktopTaskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val cornerRadius: Float =
        resources.getDimension(R.dimen.desktop_windowing_freeform_task_rounded_corner_radius)

    private val imageView: ImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    private var initialX: Float = 0f
    private var initialY: Float = 0f
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var isDragging: Boolean = false

    var currentBounds: RectF = RectF()
        private set

    init {
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(imageView, lp)
        clipToOutline = true
        elevation = 12f
        setBackgroundResource(R.drawable.bg_floating_desktop_select)
    }

    fun init(startBounds: RectF, thumbnail: Bitmap?) {
        currentBounds.set(startBounds)
        x = startBounds.left
        y = startBounds.top
        layoutParams = (layoutParams ?: ViewGroup.MarginLayoutParams(0, 0)).apply {
            width = startBounds.width().toInt()
            height = startBounds.height().toInt()
        }
        thumbnail?.let {
            imageView.setImageBitmap(it)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                initialX = x
                initialY = y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    x = initialX + deltaX
                    y = initialY + deltaY
                    currentBounds.offsetTo(x, y)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Resizes the floating window to new dimensions.
     */
    fun resize(newWidth: Int, newHeight: Int) {
        val lp = layoutParams ?: ViewGroup.MarginLayoutParams(newWidth, newHeight)
        lp.width = newWidth
        lp.height = newHeight
        layoutParams = lp
        currentBounds.right = currentBounds.left + newWidth
        currentBounds.bottom = currentBounds.top + newHeight
        requestLayout()
    }

    /**
     * Creates and runs a closing animation (scale down and fade out).
     */
    fun startClosingAnimation(onEnd: (() -> Unit)? = null): Animator {
        val scaleXAnim = ObjectAnimator.ofFloat(this, View.SCALE_X, scaleX, 0.8f)
        val scaleYAnim = ObjectAnimator.ofFloat(this, View.SCALE_Y, scaleY, 0.8f)
        val fadeAnim = ObjectAnimator.ofFloat(this, View.ALPHA, alpha, 0f)

        val set = AnimatorSet().apply {
            duration = 200
            playTogether(scaleXAnim, scaleYAnim, fadeAnim)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                    (parent as? ViewGroup)?.removeView(this@FloatingDesktopTaskView)
                    onEnd?.invoke()
                }
            })
        }
        set.start()
        return set
    }

    companion object {
        /**
         * Factory method to create and attach a [FloatingDesktopTaskView] to the root container.
         */
        @JvmStatic
        fun create(
            context: Context,
            container: ViewGroup,
            startBounds: RectF,
            thumbnail: Bitmap?
        ): FloatingDesktopTaskView {
            val floatingView = FloatingDesktopTaskView(context)
            floatingView.init(startBounds, thumbnail)
            container.addView(floatingView)
            return floatingView
        }
    }
}
