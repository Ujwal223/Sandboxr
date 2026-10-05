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

package com.sandboxr.launcher.remoteanimations

import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Encapsulates geometric and scaling properties used during the app launch animation
 * to morph an app icon into its destination window bounds.
 */
class AnimOpenProperties(
    val windowTargetBounds: Rect,
    val launcherIconBounds: RectF,
    val iconView: View? = null,
    val dragLayerLeft: Int = 0,
    val dragLayerTop: Int = 0,
    val hasSplashScreen: Boolean = false,
    val hasDifferentAppIcon: Boolean = false
) {
    val cropCenterXStart: Int = windowTargetBounds.centerX()
    val cropCenterYStart: Int = windowTargetBounds.centerY()
    val cropWidthStart: Int = launcherIconBounds.width().toInt().coerceAtLeast(108)
    val cropHeightStart: Int = cropWidthStart

    val cropCenterXEnd: Int = windowTargetBounds.centerX()
    val cropCenterYEnd: Int = windowTargetBounds.centerY()
    val cropWidthEnd: Int = windowTargetBounds.width()
    val cropHeightEnd: Int = windowTargetBounds.height()

    // Translates the app icon to the center of the window bounds in screen coordinates
    val dX: Float = (windowTargetBounds.centerX() - dragLayerLeft).toFloat() - launcherIconBounds.centerX()
    val dY: Float = (windowTargetBounds.centerY() - dragLayerTop).toFloat() - launcherIconBounds.centerY()

    val initialAppIconScale: Float = 1f

    val finalAppIconScale: Float = run {
        val smallestSize = min(windowTargetBounds.height(), windowTargetBounds.width()).toFloat()
        val iconWidth = launcherIconBounds.width().coerceAtLeast(1f)
        val iconHeight = launcherIconBounds.height().coerceAtLeast(1f)
        max(smallestSize / iconWidth, smallestSize / iconHeight)
    }

    val iconAlphaStart: Float = if (hasSplashScreen && !hasDifferentAppIcon) 0f else 1f
}
