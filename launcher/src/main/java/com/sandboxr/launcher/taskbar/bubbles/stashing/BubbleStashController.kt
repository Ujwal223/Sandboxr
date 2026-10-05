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

package com.sandboxr.launcher.taskbar.bubbles.stashing

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import com.sandboxr.launcher.taskbar.bubbles.BubbleBarView

/**
 * Coordinates stashing animations and state for the Bubble Bar.
 */
class BubbleStashController(
    private val bubbleBarView: BubbleBarView,
) {
    var isStashed: Boolean = false
        private set

    fun stash(animate: Boolean = true) {
        isStashed = true
        if (animate) {
            val anim = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(bubbleBarView, View.TRANSLATION_Y, 100f),
                    ObjectAnimator.ofFloat(bubbleBarView, View.ALPHA, 0f),
                )
                duration = 200
            }
            anim.start()
        } else {
            bubbleBarView.translationY = 100f
            bubbleBarView.alpha = 0f
        }
    }

    fun unstash(animate: Boolean = true) {
        isStashed = false
        if (animate) {
            val anim = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(bubbleBarView, View.TRANSLATION_Y, 0f),
                    ObjectAnimator.ofFloat(bubbleBarView, View.ALPHA, 1f),
                )
                duration = 200
            }
            anim.start()
        } else {
            bubbleBarView.translationY = 0f
            bubbleBarView.alpha = 1f
        }
    }
}
