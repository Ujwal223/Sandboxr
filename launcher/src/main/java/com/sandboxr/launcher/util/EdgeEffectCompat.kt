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

package com.sandboxr.launcher.util

import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.widget.EdgeEffect

/**
 * Extension of [EdgeEffect] to allow backwards compatibility and gesture integration.
 */
open class EdgeEffectCompat(context: Context) : EdgeEffect(context) {

    open fun onPullDistance(deltaDistance: Float, displacement: Float, ev: MotionEvent?): Float {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            onPullDistance(deltaDistance, displacement)
        } else {
            onPull(deltaDistance, displacement)
            0f
        }
    }

    open fun onFlingVelocity(velocity: Int) {
        if (velocity != 0) {
            onAbsorb(velocity)
        }
    }

    open fun onRelease(ev: MotionEvent?) {
        onRelease()
    }
}
