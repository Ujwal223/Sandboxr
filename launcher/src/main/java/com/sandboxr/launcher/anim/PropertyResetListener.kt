/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.animation.ObjectAnimator
import android.util.Property

/**
 * An [AnimatorListenerAdapter] that resets the target property to a specified value upon animation end.
 */
open class PropertyResetListener<T, V>(
    private val propertyToReset: Property<T, V>,
    private val resetToValue: V
) : AnimatorListenerAdapter() {

    @Suppress("UNCHECKED_CAST")
    override fun onAnimationEnd(animation: Animator) {
        if (animation is ObjectAnimator) {
            val target = animation.target as? T
            if (target != null) {
                propertyToReset.set(target, resetToValue)
            }
        }
    }
}
