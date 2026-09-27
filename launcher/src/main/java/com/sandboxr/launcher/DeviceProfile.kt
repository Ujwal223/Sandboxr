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

package com.sandboxr.launcher

import android.content.Context
import android.graphics.Rect

/**
 * Encapsulates device display characteristics, grid configuration, and sizing metrics.
 */
open class DeviceProfile(
    @JvmField var widthPx: Int = 1080,
    @JvmField var heightPx: Int = 2400,
    @JvmField var numRows: Int = 5,
    @JvmField var numColumns: Int = 5,
    @JvmField var numShownHotseatIcons: Int = 5,
    @JvmField var iconSizePx: Int = 120,
    @JvmField var iconTextSizePx: Float = 14f,
    @JvmField var isTablet: Boolean = false,
    @JvmField var isMultiWindowMode: Boolean = false,
    @JvmField var isLandscape: Boolean = false
) {
    @JvmField
    var insets = Rect()

    fun updateInsets(newInsets: Rect) {
        insets.set(newInsets)
    }

    open fun isVerticalBarLayout(): Boolean {
        return isLandscape && !isTablet
    }

    open fun getAbsoluteOpenFolderBounds(): Rect {
        val left = insets.left + 40
        val top = insets.top + 100
        val right = widthPx - insets.right - 40
        val bottom = heightPx - insets.bottom - 100
        return Rect(left, top, right, bottom)
    }

    fun interface OnDeviceProfileChangeListener {
        fun onDeviceProfileChanged(dp: DeviceProfile)
    }

    companion object {
        @JvmField
        val DEFAULT_DEVICE_PROFILE = DeviceProfile()

        @JvmStatic
        fun fromContext(context: Context): DeviceProfile {
            val dm = context.resources.displayMetrics
            val isLandscape = dm.widthPixels > dm.heightPixels
            return DeviceProfile(
                widthPx = dm.widthPixels,
                heightPx = dm.heightPixels,
                isLandscape = isLandscape
            )
        }
    }
}
