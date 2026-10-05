/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.sandboxr.launcher.apppairs

import android.content.Context
import android.graphics.Color
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.DeviceProfile
import com.sandboxr.launcher.InvariantDeviceProfile
import com.sandboxr.launcher.views.ActivityContext

/**
 * Geometric, visual, and layout calculation parameters for rendering dual-app App Pair icons.
 * Calculates split orientation, radii, channel spacing, member icon scaling, and Liquid Glass tints.
 */
open class AppPairIconDrawingParams(val context: Context, container: Int) {

    companion object {
        const val STANDARD_ICON_PADDING = 1 / 24f
        const val STANDARD_ICON_SHRINK = 1 - STANDARD_ICON_PADDING * 2
        const val OUTER_PADDING_SCALE = 1 / 30f * STANDARD_ICON_SHRINK
        const val INNER_PADDING_SCALE = 1 / 24f * STANDARD_ICON_SHRINK
        const val CENTER_CHANNEL_SCALE = 1 / 30f * STANDARD_ICON_SHRINK
        const val BIG_RADIUS_SCALE = 1 / 5f * STANDARD_ICON_SHRINK
        const val SMALL_RADIUS_SCALE = 1 / 15f * STANDARD_ICON_SHRINK
        const val MEMBER_ICON_SCALE = 11 / 30f * STANDARD_ICON_SHRINK
    }

    val iconSize: Int
    val standardIconPadding: Float
    val outerPadding: Float
    val backgroundSize: Float
    val centerChannelSize: Float
    val bigRadius: Float
    val smallRadius: Float
    val innerPadding: Float
    val memberIconSize: Float

    var isLeftRightSplit: Boolean = true
    var bgColor: Int = Color.argb(48, 255, 255, 255)
    var hoverScale: Float = 1f

    init {
        val dp: DeviceProfile = runCatching {
            val actCtx: ActivityContext = ActivityContext.lookupContext(context)
            actCtx.getDeviceProfile()
        }.getOrElse {
            InvariantDeviceProfile.INSTANCE(context).getDeviceProfile(context)
        }

        iconSize = if (dp.iconSizePx > 0) dp.iconSizePx else (48 * context.resources.displayMetrics.density).toInt()
        standardIconPadding = iconSize * STANDARD_ICON_PADDING
        outerPadding = iconSize * OUTER_PADDING_SCALE
        backgroundSize = iconSize * STANDARD_ICON_SHRINK - (outerPadding * 2)
        centerChannelSize = iconSize * CENTER_CHANNEL_SCALE
        bigRadius = iconSize * BIG_RADIUS_SCALE
        smallRadius = iconSize * SMALL_RADIUS_SCALE
        innerPadding = iconSize * INNER_PADDING_SCALE
        memberIconSize = iconSize * MEMBER_ICON_SCALE

        updateOrientation(dp)
        updateBgColor(container)
    }

    /**
     * Checks device profile orientation and updates split orientation.
     */
    fun updateOrientation(dp: DeviceProfile) {
        isLeftRightSplit = dp.isLandscape || dp.isTablet
    }

    /**
     * Updates background paint color based on parent container.
     */
    fun updateBgColor(container: Int) {
        bgColor = if (container == BubbleTextView.DISPLAY_FOLDER) {
            Color.argb(72, 255, 255, 255)
        } else {
            Color.argb(48, 255, 255, 255)
        }
    }
}
