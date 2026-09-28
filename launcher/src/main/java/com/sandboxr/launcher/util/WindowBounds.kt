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

import android.graphics.Insets
import android.graphics.Point
import android.graphics.Rect
import android.view.WindowInsets
import android.view.WindowMetrics
import androidx.annotation.Nullable
import java.util.Objects

/**
 * Utility class that holds information about a window's position and layout within its display.
 *
 * Tracks the full [bounds] of the window, [insets] consumed by system decorations (status bar,
 * nav bar, cutouts), an [availableSize] that reflects usable space, and a [rotationHint] for
 * orientation-aware calculations.
 *
 * @property bounds     Full window rectangle in screen coordinates (pixels).
 * @property insets     System-decoration insets (status bar, nav bar, cutouts) in pixels.
 * @property availableSize Usable content area: bounds minus horizontal and vertical insets.
 * @property rotationHint Current display rotation (e.g. [android.view.Surface.ROTATION_0]).
 */
class WindowBounds @JvmOverloads constructor(
    @JvmField val bounds: Rect,
    @JvmField val insets: Rect,
    @JvmField val rotationHint: Int = -1,
) {

    /** Usable content dimensions: [bounds] minus [insets]. */
    @JvmField
    val availableSize: Point = Point(
        bounds.width() - insets.left - insets.right,
        bounds.height() - insets.top - insets.bottom,
    )

    /**
     * Secondary constructor for test-only scenarios where precise insets are not needed.
     * Insets are approximated as the difference between total and available sizes.
     */
    constructor(
        width: Int,
        height: Int,
        availableWidth: Int,
        availableHeight: Int,
        rotationHint: Int,
    ) : this(
        bounds = Rect(0, 0, width, height),
        insets = Rect(0, 0, width - availableWidth, height - availableHeight),
        rotationHint = rotationHint,
    )

    /** Returns `true` if the window is wider than tall (landscape orientation). */
    val isLandscape: Boolean get() = bounds.width() > bounds.height()

    override fun hashCode(): Int = Objects.hash(bounds, insets)

    override fun equals(@Nullable other: Any?): Boolean {
        if (other !is WindowBounds) return false
        return other.bounds == bounds && other.insets == insets && other.rotationHint == rotationHint
    }

    override fun toString(): String =
        "WindowBounds{bounds=$bounds, insets=$insets, availableSize=$availableSize, rotationHint=$rotationHint}"

    companion object {
        /**
         * Creates a [WindowBounds] from a [WindowMetrics] by extracting the system-bar insets.
         *
         * **Requires API 30+**.
         */
        @Suppress("NewApi")
        @JvmStatic
        fun fromWindowMetrics(wm: WindowMetrics): WindowBounds {
            val rawInsets: Insets = wm.windowInsets.getInsets(WindowInsets.Type.systemBars())
            return WindowBounds(
                bounds = wm.bounds,
                insets = Rect(rawInsets.left, rawInsets.top, rawInsets.right, rawInsets.bottom),
            )
        }
    }
}
