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

import android.graphics.Point
import android.view.DisplayCutout
import android.view.Surface
import androidx.annotation.Nullable
import java.util.Objects

/**
 * Immutable snapshot of a display's key properties, normalized to [Surface.ROTATION_0] for
 * orientation-independent comparisons.
 *
 * @property size     Display dimensions in pixels, in the current [rotation] coordinate space.
 * @property rotation One of [Surface.ROTATION_0], [ROTATION_90], [ROTATION_180], [ROTATION_270].
 * @property cutout   The display cutout descriptor; defaults to an empty cutout when null.
 */
class CachedDisplayInfo @JvmOverloads constructor(
    @JvmField val size: Point = Point(0, 0),
    @JvmField val rotation: Int = Surface.ROTATION_0,
    @Nullable cutout: DisplayCutout? = null,
) {
    @JvmField
    val cutout: DisplayCutout = cutout ?: NO_CUTOUT

    /**
     * Returns a copy of this info with [size] and [cutout] rotated to [Surface.ROTATION_0].
     * If already at rotation 0, returns `this` unchanged.
     */
    fun normalize(): CachedDisplayInfo {
        if (rotation == Surface.ROTATION_0) return this
        val newSize = Point(size)
        rotateSize(newSize, deltaRotation(rotation, Surface.ROTATION_0))
        return CachedDisplayInfo(newSize, Surface.ROTATION_0, null)
    }

    override fun hashCode(): Int = Objects.hash(size, rotation, cutout)

    override fun equals(other: Any?): Boolean {
        if (other !is CachedDisplayInfo) return false
        return other.size == size && other.rotation == rotation && other.cutout == cutout
    }

    override fun toString(): String =
        "CachedDisplayInfo{size=$size, rotation=$rotation, cutout=$cutout}"

    companion object {
        private val NO_CUTOUT: DisplayCutout by lazy {
            DisplayCutout(
                android.graphics.Insets.NONE,
                null, null, null, null,
            )
        }

        /**
         * Returns the clockwise angular delta (in 90° steps, 0–3) needed to rotate from
         * [fromRotation] to [toRotation].
         *
         * Example: deltaRotation(ROTATION_90, ROTATION_0) == 3 (= 270° clockwise).
         */
        @JvmStatic
        fun deltaRotation(fromRotation: Int, toRotation: Int): Int =
            Math.floorMod(toRotation - fromRotation, 4)

        /**
         * Rotates [size] in-place by [delta] 90° clockwise steps (as returned by [deltaRotation]).
         *
         * An odd [delta] swaps width and height (portrait ↔ landscape).
         */
        @JvmStatic
        fun rotateSize(size: Point, delta: Int) {
            if (delta % 2 != 0) {
                val tmp = size.x
                size.x = size.y
                size.y = tmp
            }
        }
    }
}
