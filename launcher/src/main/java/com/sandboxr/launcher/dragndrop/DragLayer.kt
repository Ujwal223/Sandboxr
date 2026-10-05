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
import android.graphics.Rect
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.Workspace
import com.sandboxr.launcher.views.BaseDragLayer

/**
 * Top-level DragLayer ViewGroup coordinating touch interception, drag surfaces,
 * and spring-physics drop animations for [Launcher].
 */
open class DragLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseDragLayer<Launcher>(context, attrs) {

    companion object {
        const val ANIMATION_END_DISAPPEAR = 0
        const val ANIMATION_END_REMAIN_VISIBLE = 2
    }

    private var mDragController: DragController? = null
    private var mWorkspace: Workspace<*>? = null

    private var mDropAnim: Animator? = null
    private var mDropView: DragView? = null

    fun setup(dragController: DragController, workspace: Workspace<*>?) {
        mDragController = dragController
        mWorkspace = workspace
        recreateControllers()
    }

    override fun recreateControllers() {
        super.recreateControllers()
        mDragController?.let { mControllers.add(it) }
    }

    fun animateViewIntoPosition(
        dragView: DragView,
        pos: IntArray,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        animationEndStyle: Int,
        onCompleteRunnable: Runnable?,
        duration: Int
    ) {
        val to = Rect(pos[0], pos[1], pos[0] + dragView.measuredWidth, pos[1] + dragView.measuredHeight)
        animateView(
            view = dragView,
            to = to,
            scaleX = scaleX,
            scaleY = scaleY,
            alpha = alpha,
            duration = duration,
            interpolator = OvershootInterpolator(1.2f),
            onComplete = onCompleteRunnable,
            animationEndStyle = animationEndStyle
        )
    }

    fun animateView(
        view: DragView,
        to: Rect,
        scaleX: Float,
        scaleY: Float,
        alpha: Float,
        duration: Int,
        interpolator: Interpolator?,
        onComplete: Runnable?,
        animationEndStyle: Int = ANIMATION_END_DISAPPEAR
    ) {
        mDropView = view
        val startX = view.translationX
        val startY = view.translationY
        val startScaleX = view.scaleX
        val startScaleY = view.scaleY
        val startAlpha = view.alpha

        val targetX = to.left.toFloat()
        val targetY = to.top.toFloat()

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            setDuration(duration.toLong().coerceAtLeast(180L))
            setInterpolator(interpolator ?: OvershootInterpolator(1.2f))
            addUpdateListener { va ->
                val f = va.animatedFraction
                view.translationX = startX + (targetX - startX) * f
                view.translationY = startY + (targetY - startY) * f
                view.scaleX = startScaleX + (scaleX - startScaleX) * f
                view.scaleY = startScaleY + (scaleY - startScaleY) * f
                view.alpha = startAlpha + (alpha - startAlpha) * f
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (animationEndStyle == ANIMATION_END_DISAPPEAR) {
                        clearAnimatedView()
                    }
                    onComplete?.run()
                }
            })
        }
        playDropAnimation(view, anim, animationEndStyle)
    }

    fun playDropAnimation(view: DragView, animator: Animator, animationEndStyle: Int) {
        mDropAnim?.cancel()
        mDropAnim = animator
        mDropView = view
        animator.start()
    }

    fun getAnimatedView(): View? = mDropView

    fun clearAnimatedView(): DragView? {
        val animView = mDropView
        mDropAnim?.cancel()
        mDropAnim = null
        if (animView != null) {
            animView.remove()
            mDropView = null
        }
        return animView
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // If DragController is currently dragging, prioritize routing to drag controller
        if (mDragController?.isDragging == true) {
            if (mDragController?.onControllerTouchEvent(ev) == true) {
                return true
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return mDragController?.dispatchKeyEvent(event) == true || super.dispatchKeyEvent(event)
    }
}
