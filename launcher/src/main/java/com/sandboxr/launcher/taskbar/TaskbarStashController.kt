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

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View

/**
 * State machine and animation coordinator between TaskbarViewController and StashedHandleViewController.
 */
class TaskbarStashController(
    val activity: TaskbarActivityContext,
) {
    companion object {
        const val FLAG_IN_APP = 1 shl 0
        const val FLAG_STASHED_IN_APP_SYSUI = 1 shl 1
        const val FLAG_STASHED_IN_APP_SETUP = 1 shl 2
        const val FLAG_STASHED_IME = 1 shl 3
        const val FLAG_STASHED_PINNING = 1 shl 4
    }

    private lateinit var controllers: TaskbarControllers
    private var stateFlags: Int = 0

    var isStashed: Boolean = false
        private set

    val isInApp: Boolean
        get() = hasFlag(FLAG_IN_APP)

    fun init(controllers: TaskbarControllers) {
        this.controllers = controllers
        applyState(animate = false)
    }

    fun hasFlag(flag: Int): Boolean = (stateFlags and flag) != 0

    fun updateStateForFlag(flag: Int, enabled: Boolean) {
        stateFlags = if (enabled) {
            stateFlags or flag
        } else {
            stateFlags and flag.inv()
        }
    }

    fun applyState(animate: Boolean = true) {
        val shouldStash = !activity.sharedState.isPinned && (
            hasFlag(FLAG_STASHED_IN_APP_SYSUI) ||
            hasFlag(FLAG_STASHED_IN_APP_SETUP) ||
            hasFlag(FLAG_STASHED_IME) ||
            (hasFlag(FLAG_IN_APP) && !activity.sharedState.isPinned)
        )
        animateToStashState(shouldStash, animate)
    }

    fun updateAndAnimateTransientTaskbar(stash: Boolean) {
        if (activity.sharedState.isPinned) return
        animateToStashState(stash, animate = true)
    }

    fun onPinningStateChanged(isPinned: Boolean) {
        if (isPinned) {
            animateToStashState(stash = false, animate = true)
        } else {
            applyState(animate = true)
        }
    }

    private fun animateToStashState(stash: Boolean, animate: Boolean) {
        isStashed = stash
        val taskbarView = controllers.taskbarViewController.taskbarView
        val stashedHandleView = controllers.stashedHandleViewController.stashedHandleView

        val targetTaskbarAlpha = if (stash) 0f else 1f
        val targetTaskbarY = if (stash) taskbarView.height.toFloat() else 0f
        val targetHandleAlpha = if (stash) 1f else 0f

        if (animate) {
            val animSet = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(taskbarView, View.ALPHA, targetTaskbarAlpha),
                    ObjectAnimator.ofFloat(taskbarView, View.TRANSLATION_Y, targetTaskbarY),
                    ObjectAnimator.ofFloat(stashedHandleView, View.ALPHA, targetHandleAlpha),
                )
                duration = 250
            }
            if (!stash) {
                taskbarView.visibility = View.VISIBLE
            }
            animSet.start()
        } else {
            taskbarView.alpha = targetTaskbarAlpha
            taskbarView.translationY = targetTaskbarY
            stashedHandleView.alpha = targetHandleAlpha
            taskbarView.visibility = if (stash) View.GONE else View.VISIBLE
            stashedHandleView.visibility = if (stash) View.VISIBLE else View.GONE
        }
        controllers.stashedHandleViewController.setIsStashed(stash, animate)
    }
}
