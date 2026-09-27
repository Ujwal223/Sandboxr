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

package com.sandboxr.launcher.util.window

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

/**
 * Utility class to track refresh rate and single frame timing for animations.
 */
interface RefreshRateTracker {

    val singleFrameMs: Int

    companion object {
        const val DEFAULT_FRAME_MS = 16

        @JvmStatic
        fun getSingleFrameMs(context: Context?): Int {
            if (context == null) return DEFAULT_FRAME_MS
            return try {
                val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                val refreshRate = dm?.getDisplay(Display.DEFAULT_DISPLAY)?.refreshRate ?: 60f
                if (refreshRate > 0f) {
                    (1000f / refreshRate).toInt()
                } else {
                    DEFAULT_FRAME_MS
                }
            } catch (e: Exception) {
                DEFAULT_FRAME_MS
            }
        }
    }
}
