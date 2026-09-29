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

package com.sandboxr.launcher.views

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * Scrim view used to draw background tinting / dimming during state transitions.
 */
open class ScrimView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var scrimColors: ScrimColors = ScrimColors()
        set(value) {
            field = value
            setBackgroundColor(value.backgroundColor)
            invalidate()
        }

    var scrimHeaderScale: Float = 1f
        set(value) {
            field = value
            invalidate()
        }

    var drawingController: Any? = null
        set(value) {
            field = value
            invalidate()
        }

    var progress: Float = 0f
        set(value) {
            field = value
            alpha = value.coerceIn(0f, 1f)
            invalidate()
        }
}
