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
import androidx.annotation.CallSuper

/**
 * Extension of [AnimatorListenerAdapter] that only fires [onAnimationSuccess] if the animation
 * completed without being cancelled.
 */
abstract class AnimationSuccessListener : AnimatorListenerAdapter() {

    protected var isCancelled: Boolean = false
        private set

    @CallSuper
    override fun onAnimationCancel(animation: Animator) {
        isCancelled = true
    }

    override fun onAnimationEnd(animation: Animator) {
        if (!isCancelled) {
            onAnimationSuccess(animation)
        }
    }

    abstract fun onAnimationSuccess(animator: Animator)
}
