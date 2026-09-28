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

package com.sandboxr.launcher.display

import kotlin.math.max
import kotlin.math.min

/**
 * Orientation-independent size container that always stores dimensions in portrait orientation
 * (i.e. [width] ≤ [height]).
 *
 * This allows device profile comparisons that are rotation-agnostic: the same screen in portrait
 * and landscape produces the same [PortraitSize].
 *
 * @property width  The smaller of the two screen dimensions (portrait width), in dp or px.
 * @property height The larger of the two screen dimensions (portrait height), in dp or px.
 */
data class PortraitSize private constructor(
    @JvmField val width: Int,
    @JvmField val height: Int,
) {
    companion object {
        /**
         * Creates a [PortraitSize] from [w] and [h], reordering them so that [width] ≤ [height].
         */
        @JvmStatic
        fun from(w: Int, h: Int): PortraitSize = PortraitSize(min(w, h), max(w, h))
    }
}
