/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.sandboxr.launcher

import android.animation.TimeInterpolator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.sandboxr.launcher.DropTarget.DragObject
import com.sandboxr.launcher.dragndrop.DragController
import com.sandboxr.launcher.dragndrop.DragController.DragListener
import com.sandboxr.launcher.dragndrop.DragOptions

/**
 * Top container bar hosting drop targets (Delete, Uninstall, App Info) with animated slide reveal
 * and responsive centering over the workspace.
 */
open class DropTargetBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle), DragListener, Insettable {

    private var dropTargets: Array<ButtonDropTarget> = emptyArray()
    private val tempTargets = arrayOfNulls<ButtonDropTarget>(4)

    private var currentAnimation: ViewPropertyAnimator? = null
    var isBarVisible: Boolean = false
        private set

    private var deferOnDragEnd: Boolean = false

    companion object {
        const val DEFAULT_DRAG_FADE_DURATION: Long = 175L
        val DEFAULT_INTERPOLATOR: TimeInterpolator = DecelerateInterpolator()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        val targets = ArrayList<ButtonDropTarget>()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is ButtonDropTarget) {
                child.setDropTargetBar(this)
                targets.add(child)
            }
        }
        dropTargets = targets.toTypedArray()
    }

    fun getDropTargets(): Array<ButtonDropTarget> = dropTargets

    fun setup(dragController: DragController) {
        dragController.addDragListener(this)
        for (target in dropTargets) {
            dragController.addDragListener(target)
            dragController.addDropTarget(target)
        }
    }

    override fun setInsets(insets: Rect) {
        val lp = (layoutParams as? FrameLayout.LayoutParams) ?: FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        lp.leftMargin = insets.left
        lp.topMargin = insets.top
        lp.rightMargin = insets.right
        lp.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
        layoutParams = lp
    }

    fun deferOnDragEnd() {
        deferOnDragEnd = true
    }

    fun animateToVisibility(visible: Boolean) {
        if (isBarVisible == visible) return
        isBarVisible = visible

        currentAnimation?.cancel()
        currentAnimation = null

        val targetAlpha = if (visible) 1.0f else 0.0f
        val targetTranslationY = if (visible) 0.0f else -height.toFloat().coerceAtLeast(100f)

        if (visible) {
            visibility = View.VISIBLE
            alpha = 0.0f
            translationY = -height.toFloat().coerceAtLeast(100f)
        }

        currentAnimation = animate()
            .alpha(targetAlpha)
            .translationY(targetTranslationY)
            .setInterpolator(DEFAULT_INTERPOLATOR)
            .setDuration(DEFAULT_DRAG_FADE_DURATION)
            .withEndAction {
                if (!isBarVisible) {
                    visibility = View.INVISIBLE
                }
                currentAnimation = null
            }
        currentAnimation?.start()
    }

    override fun onDragStart(dragObject: DragObject?, options: DragOptions?) {
        animateToVisibility(true)
    }

    override fun onDragEnd() {
        if (!deferOnDragEnd) {
            animateToVisibility(false)
        } else {
            deferOnDragEnd = false
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec).coerceAtLeast(
            (48 * resources.displayMetrics.density).toInt()
        )
        val heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)

        val visibleCount = getVisibleButtons(tempTargets)
        val childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST)

        for (i in 0 until visibleCount) {
            val button = tempTargets[i] ?: continue
            button.measure(childWidthSpec, heightSpec)
        }

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val visibleCount = getVisibleButtons(tempTargets)
        if (visibleCount == 0) return

        val width = right - left
        val height = bottom - top
        val center = width / 2
        val gap = (16 * resources.displayMetrics.density).toInt()

        if (visibleCount == 1) {
            val button = tempTargets[0] ?: return
            val bWidth = button.measuredWidth
            val bHeight = button.measuredHeight
            val bTop = (height - bHeight) / 2
            button.layout(center - bWidth / 2, bTop, center + bWidth / 2, bTop + bHeight)
        } else if (visibleCount == 2) {
            val leftButton = tempTargets[0] ?: return
            val rightButton = tempTargets[1] ?: return

            val lbWidth = leftButton.measuredWidth
            val rbWidth = rightButton.measuredWidth
            val lbHeight = leftButton.measuredHeight
            val rbHeight = rightButton.measuredHeight

            val lbTop = (height - lbHeight) / 2
            val rbTop = (height - rbHeight) / 2

            val totalWidth = lbWidth + gap + rbWidth
            val start = center - totalWidth / 2

            leftButton.layout(start, lbTop, start + lbWidth, lbTop + lbHeight)
            val rightStart = start + lbWidth + gap
            rightButton.layout(rightStart, rbTop, rightStart + rbWidth, rbTop + rbHeight)
        }
    }

    private fun getVisibleButtons(outButtons: Array<ButtonDropTarget?>): Int {
        var count = 0
        for (target in dropTargets) {
            if (target.visibility != View.GONE) {
                outButtons[count] = target
                count++
            }
        }
        return count
    }
}
