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
import java.util.function.Consumer

/**
 * Utility class for creating common [Animator.AnimatorListener]s.
 */
object AnimatorListeners {

    const val SUCCESS_TRANSITION_PROGRESS = 0.92f

    @JvmStatic
    fun forSuccessCallback(callback: Runnable): Animator.AnimatorListener {
        return object : AnimationSuccessListener() {
            override fun onAnimationSuccess(animator: Animator) {
                callback.run()
            }
        }
    }

    @JvmStatic
    fun forEndCallback(callback: Consumer<Boolean>): Animator.AnimatorListener {
        return object : AnimatorListenerAdapter() {
            private var listenerCalled = false

            override fun onAnimationCancel(animation: Animator) {
                if (!listenerCalled) {
                    listenerCalled = true
                    callback.accept(false)
                }
            }

            override fun onAnimationEnd(anim: Animator) {
                if (!listenerCalled) {
                    listenerCalled = true
                    val success = if (anim is ValueAnimator) {
                        anim.animatedFraction > SUCCESS_TRANSITION_PROGRESS
                    } else {
                        true
                    }
                    callback.accept(success)
                }
            }
        }
    }

    @JvmStatic
    fun forEndCallback(callback: Runnable): Animator.AnimatorListener {
        return object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                callback.run()
            }
        }
    }
}
