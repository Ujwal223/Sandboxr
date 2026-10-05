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

import android.animation.TimeInterpolator
import kotlin.math.pow

open class LogDecelerateInterpolator @JvmOverloads constructor(
    val base: Int = 100,
    val drift: Int = 0
) : TimeInterpolator {

    private val logScale: Float = 1f / computeLog(1f, base, drift)

    override fun getInterpolation(t: Float): Float {
        return if (t.compareTo(1f) == 0) {
            1f
        } else {
            computeLog(t, base, drift) * logScale
        }
    }

    companion object {
        @JvmStatic
        fun computeLog(t: Float, base: Int, drift: Int): Float {
            return (-base.toDouble().pow(-t.toDouble()) + 1.0 + (drift * t)).toFloat()
        }
    }
}
