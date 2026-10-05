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

package com.sandboxr.launcher.taskbar

import android.graphics.Rect
import android.view.View

/**
 * Controls the stashed handle view's alpha, size, position, and interaction.
 */
class StashedHandleViewController(
    val activity: TaskbarActivityContext,
    val stashedHandleView: StashedHandleView,
) {
    private lateinit var controllers: TaskbarControllers
    val stashedHandleBounds = Rect()

    var isStashedHandleVisible: Boolean = false
        private set

    fun init(controllers: TaskbarControllers) {
        this.controllers = controllers
        stashedHandleView.setOnClickListener {
            controllers.taskbarStashController.updateAndAnimateTransientTaskbar(false)
        }
    }

    fun setIsStashed(isStashed: Boolean, animate: Boolean = true) {
        isStashedHandleVisible = isStashed
        val targetAlpha = if (isStashed) 1f else 0f
        if (animate) {
            stashedHandleView.animate()
                .alpha(targetAlpha)
                .setDuration(200)
                .withStartAction {
                    if (isStashed) stashedHandleView.visibility = View.VISIBLE
                }
                .withEndAction {
                    if (!isStashed) stashedHandleView.visibility = View.GONE
                }
                .start()
        } else {
            stashedHandleView.alpha = targetAlpha
            stashedHandleView.visibility = if (isStashed) View.VISIBLE else View.GONE
        }
    }

    fun onLayout(width: Int, height: Int) {
        val handleWidth = activity.resources.getDimensionPixelSize(com.sandboxr.launcher.R.dimen.taskbar_stashed_handle_width)
        val handleHeight = activity.resources.getDimensionPixelSize(com.sandboxr.launcher.R.dimen.taskbar_stashed_handle_height)
        val left = (width - handleWidth) / 2
        val right = left + handleWidth
        val bottom = height - 8
        val top = bottom - handleHeight
        stashedHandleBounds.set(left, top, right, bottom)
        stashedHandleView.updateSampledRegion(stashedHandleBounds)
    }
}
