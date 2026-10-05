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

package com.sandboxr.launcher.recents.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.android.systemui.shared.recents.model.Task
import com.android.systemui.shared.recents.model.ThumbnailData
import com.sandboxr.launcher.R
import com.sandboxr.launcher.task.thumbnail.TaskThumbnailView
import kotlin.math.abs

/**
 * Task card view in recents overview representing an app snapshot.
 */
open class TaskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private var mSnapshotView: TaskThumbnailView? = null
    private var mIconView: IconAppChipView? = null
    private var mDismissButton: View? = null

    var task: Task? = null
        private set

    var onTaskLaunchCallback: ((Task) -> Unit)? = null
    var onTaskDismissCallback: ((TaskView) -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var isDragging = false
    private var velocityTracker: VelocityTracker? = null

    val taskCornerRadius: Float =
        resources.getDimension(R.dimen.task_corner_radius)

    init {
        isFocusable = true
        isClickable = true
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, taskCornerRadius)
            }
        }
        setOnClickListener {
            val currentTask = task
            if (currentTask != null) {
                onTaskLaunchCallback?.invoke(currentTask)
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mSnapshotView = findViewById(R.id.snapshot)
        mIconView = findViewById(R.id.icon)
        mDismissButton = findViewById(R.id.task_dismiss_button)

        mSnapshotView?.cornerRadius = taskCornerRadius

        mDismissButton?.setOnClickListener {
            animateDismiss(dismissUp = true)
        }
    }

    open fun bind(task: Task) {
        this.task = task
        mIconView?.setTitle(task.title ?: task.key.getPackageName())
        mIconView?.setIcon(task.icon)
        mSnapshotView?.setThumbnail(task.thumbnail)
        mDismissButton?.isVisible = true
        mDismissButton?.alpha = 1f
        translationY = 0f
        alpha = 1f
    }

    fun updateThumbnail(thumbnailData: ThumbnailData?) {
        task?.thumbnail = thumbnailData
        mSnapshotView?.setThumbnail(thumbnailData)
    }

    fun getThumbnailView(): TaskThumbnailView? = mSnapshotView
    fun getIconView(): IconAppChipView? = mIconView

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                isDragging = false
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(ev)
                val dy = ev.rawY - downY
                val dx = ev.rawX - downX
                if (abs(dy) > touchSlop && abs(dy) > abs(dx) * 1.2f) {
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                velocityTracker?.recycle()
                velocityTracker = null
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        velocityTracker?.addMovement(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val dy = ev.rawY - downY
                translationY = dy
                val fraction = 1f - (abs(dy) / (height * 0.85f).coerceAtLeast(1f))
                alpha = fraction.coerceIn(0.2f, 1f)
                return true
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.computeCurrentVelocity(1000)
                val vy = velocityTracker?.yVelocity ?: 0f
                val dy = translationY
                val threshold = height * 0.28f

                if (dy < -threshold || vy < -1200f) {
                    animateDismiss(dismissUp = true)
                } else if (dy > threshold || vy > 1200f) {
                    animateDismiss(dismissUp = false)
                } else {
                    animateReset()
                }

                velocityTracker?.recycle()
                velocityTracker = null
                isDragging = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                animateReset()
                velocityTracker?.recycle()
                velocityTracker = null
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(ev)
    }

    fun animateDismiss(dismissUp: Boolean = true) {
        val targetY = if (dismissUp) -height.toFloat() * 1.3f else height.toFloat() * 1.3f
        val animY = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, targetY)
        val animAlpha = ObjectAnimator.ofFloat(this, View.ALPHA, 0f)

        animY.duration = 220
        animAlpha.duration = 200
        animY.interpolator = DecelerateInterpolator()

        animY.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onTaskDismissCallback?.invoke(this@TaskView)
            }
        })

        animY.start()
        animAlpha.start()
    }

    fun animateReset() {
        val animY = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f)
        val animAlpha = ObjectAnimator.ofFloat(this, View.ALPHA, 1f)
        animY.duration = 200
        animAlpha.duration = 200
        animY.interpolator = DecelerateInterpolator()
        animY.start()
        animAlpha.start()
    }
}
