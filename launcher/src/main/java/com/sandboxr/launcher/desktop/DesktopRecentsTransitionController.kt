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
import android.animation.ObjectAnimator
import android.content.Context
import android.view.View

/**
 * Coordinates recents-to-desktop and desktop-to-recents transition animations and lifecycle callbacks.
 */
class DesktopRecentsTransitionController(
    val context: Context,
    val desktopVisibilityController: DesktopVisibilityController
) {

    var isDesktopLaunchOngoing: Boolean = false
        private set

    /**
     * Launches desktop workspace from recents overview.
     */
    fun launchDesktopFromRecents(
        desktopTaskView: DesktopTaskView,
        animated: Boolean,
        taskIdToReorderToFront: Int? = null,
        callback: ((Boolean) -> Unit)? = null
    ) {
        if (isDesktopLaunchOngoing) {
            callback?.invoke(false)
            return
        }

        isDesktopLaunchOngoing = true
        val displayId = desktopTaskView.desktopTask?.displayId ?: 0
        desktopVisibilityController.setActiveDesk(displayId, desktopTaskView.deskId)

        if (animated) {
            val anim = ObjectAnimator.ofFloat(desktopTaskView, View.ALPHA, desktopTaskView.alpha, 0f).apply {
                duration = 250
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        isDesktopLaunchOngoing = false
                        callback?.invoke(true)
                    }
                })
            }
            anim.start()
        } else {
            desktopTaskView.alpha = 0f
            isDesktopLaunchOngoing = false
            callback?.invoke(true)
        }
    }

    /**
     * Moves a running task into desktop freeform mode.
     */
    fun moveToDesktop(taskId: Int, callback: (() -> Unit)? = null) {
        val displayId = try {
            context.display?.displayId ?: 0
        } catch (_: Throwable) {
            0
        }
        val currentDesk = desktopVisibilityController.getActiveDeskId(displayId)
        val desk = if (currentDesk != DesktopVisibilityController.INACTIVE_DESK_ID) currentDesk else 1
        desktopVisibilityController.setActiveDesk(displayId, desk)
        callback?.invoke()
    }
}
