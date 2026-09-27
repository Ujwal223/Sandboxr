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

import android.graphics.Color

/**
 * Data class representing background and foreground colors for scrim layers.
 */
open class ScrimColors @JvmOverloads constructor(
    @JvmField val backgroundColor: Int = Color.TRANSPARENT,
    @JvmField val foregroundColor: Int = Color.TRANSPARENT
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val o = other as? ScrimColors ?: return false
        return backgroundColor == o.backgroundColor && foregroundColor == o.foregroundColor
    }
    override fun hashCode(): Int = 31 * backgroundColor + foregroundColor
    override fun toString(): String = "ScrimColors(bg=$backgroundColor, fg=$foregroundColor)"
}
