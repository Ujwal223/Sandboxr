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

package com.sandboxr.launcher.folder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.views.ScrimView

/**
 * Listens to folder animation progress and animates the background ScrimView on DragLayer,
 * providing the dark frosted glass backdrop when a folder is opened.
 */
class FolderScrimAnimationListener(
    private val launcher: Launcher,
    private val isOpening: Boolean
) : AnimatorListenerAdapter(), ValueAnimator.AnimatorUpdateListener {

    private val scrimView: ScrimView? = launcher.getScrimView()
    private val targetAlpha = if (isOpening) 0.65f else 0.0f
    private val initialAlpha = if (isOpening) 0.0f else 0.65f

    override fun onAnimationStart(animation: Animator) {
        if (isOpening) {
            scrimView?.visibility = View.VISIBLE
            scrimView?.alpha = 0f
        }
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        val fraction = animation.animatedFraction
        val currentAlpha = initialAlpha + fraction * (targetAlpha - initialAlpha)
        scrimView?.alpha = currentAlpha
    }

    override fun onAnimationEnd(animation: Animator) {
        if (!isOpening) {
            scrimView?.alpha = 0f
            scrimView?.visibility = View.GONE
        } else {
            scrimView?.alpha = targetAlpha
        }
    }
}
