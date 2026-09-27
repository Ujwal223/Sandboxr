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

package com.sandboxr.launcher.anim

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup

/**
 * A convenience class to update a view's visibility state following an alpha animation.
 */
open class AlphaUpdateListener(private val view: View?) : AnimatorListenerAdapter(), ValueAnimator.AnimatorUpdateListener {

    override fun onAnimationUpdate(animation: ValueAnimator) {
        updateVisibility(view)
    }

    override fun onAnimationEnd(animator: Animator) {
        updateVisibility(view)
    }

    override fun onAnimationStart(animator: Animator) {
        view?.visibility = View.VISIBLE
    }

    companion object {
        const val ALPHA_CUTOFF_THRESHOLD = 0.01f

        @JvmStatic
        fun updateVisibility(view: View?) {
            updateVisibility(view, View.INVISIBLE)
        }

        @JvmStatic
        fun updateVisibility(view: View?, hiddenVisibility: Int) {
            if (view == null) return
            if (view.alpha < ALPHA_CUTOFF_THRESHOLD && view.visibility != hiddenVisibility) {
                view.visibility = hiddenVisibility
            } else if (view.alpha > ALPHA_CUTOFF_THRESHOLD && view.visibility != View.VISIBLE) {
                if (view is ViewGroup) {
                    val oldFocusability = view.descendantFocusability
                    view.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    view.visibility = View.VISIBLE
                    view.descendantFocusability = oldFocusability
                } else {
                    view.visibility = View.VISIBLE
                }
            }
        }
    }
}
