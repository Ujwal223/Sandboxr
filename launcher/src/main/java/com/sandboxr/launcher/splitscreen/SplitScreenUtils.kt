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

package com.sandboxr.launcher.splitscreen

import android.content.Context
import android.graphics.Rect
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.STAGE_POSITION_BOTTOM_OR_RIGHT
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.STAGE_POSITION_TOP_OR_LEFT
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.STAGE_POSITION_UNDEFINED
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.StagePosition

/**
 * Utility functions for split screen bounds calculations, staging positions,
 * and multi-window geometry.
 */
object SplitScreenUtils {

    const val DEFAULT_DIVIDER_SIZE_PX = 48

    /**
     * Determines whether split screen splits horizontally (left/right) or vertically (top/bottom).
     * Typically landscape or tablet split splits horizontally (left/right), while portrait splits vertically.
     */
    @JvmStatic
    fun isLeftRightSplit(isLandscape: Boolean, isTablet: Boolean): Boolean {
        return isLandscape || isTablet
    }

    /**
     * Calculates the bounds for the primary (top or left) and secondary (bottom or right) stages
     * given the total display bounds, divider size, split ratio (e.g. 0.5f), and orientation.
     */
    @JvmStatic
    fun calculateSplitBounds(
        displayBounds: Rect,
        dividerSize: Int = DEFAULT_DIVIDER_SIZE_PX,
        splitRatio: Float = 0.5f,
        isLeftRight: Boolean = false,
        outPrimaryBounds: Rect,
        outSecondaryBounds: Rect,
        outDividerBounds: Rect? = null
    ) {
        val clampedRatio = splitRatio.coerceIn(0.2f, 0.8f)

        if (isLeftRight) {
            val availableWidth = displayBounds.width() - dividerSize
            val primaryWidth = (availableWidth * clampedRatio).toInt()

            outPrimaryBounds.set(
                displayBounds.left,
                displayBounds.top,
                displayBounds.left + primaryWidth,
                displayBounds.bottom
            )

            val dividerLeft = outPrimaryBounds.right
            val dividerRight = dividerLeft + dividerSize
            outDividerBounds?.set(dividerLeft, displayBounds.top, dividerRight, displayBounds.bottom)

            outSecondaryBounds.set(
                dividerRight,
                displayBounds.top,
                displayBounds.right,
                displayBounds.bottom
            )
        } else {
            val availableHeight = displayBounds.height() - dividerSize
            val primaryHeight = (availableHeight * clampedRatio).toInt()

            outPrimaryBounds.set(
                displayBounds.left,
                displayBounds.top,
                displayBounds.right,
                displayBounds.top + primaryHeight
            )

            val dividerTop = outPrimaryBounds.bottom
            val dividerBottom = dividerTop + dividerSize
            outDividerBounds?.set(displayBounds.left, dividerTop, displayBounds.right, dividerBottom)

            outSecondaryBounds.set(
                displayBounds.left,
                dividerBottom,
                displayBounds.right,
                displayBounds.bottom
            )
        }
    }

    /**
     * Given a stage position, returns the opposite stage position.
     */
    @JvmStatic
    @StagePosition
    fun getOppositeStagePosition(@StagePosition stagePosition: Int): Int {
        return when (stagePosition) {
            STAGE_POSITION_TOP_OR_LEFT -> STAGE_POSITION_BOTTOM_OR_RIGHT
            STAGE_POSITION_BOTTOM_OR_RIGHT -> STAGE_POSITION_TOP_OR_LEFT
            else -> STAGE_POSITION_UNDEFINED
        }
    }

    /**
     * Returns true if the device profile supports launching apps into split screen.
     */
    @JvmStatic
    fun isSplitScreenSupported(context: Context): Boolean {
        // Multi-window support is enabled by default on Android N+
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        return activityManager?.isLowRamDevice == false
    }
}
