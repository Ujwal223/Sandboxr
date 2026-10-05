/*
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

package com.sandboxr.launcher.cuebar.ui.utils

import android.graphics.Color
import androidx.core.graphics.ColorUtils

object AiColorUtils {
    /**
     * Boosts color saturation/chroma for animated ambient lighting borders.
     */
    fun boostChroma(color: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        if (hsl[1] <= 0.05f) {
            return color
        }
        hsl[1] = (hsl[1] * 1.3f).coerceAtMost(1f)
        return ColorUtils.HSLToColor(hsl)
    }
}
