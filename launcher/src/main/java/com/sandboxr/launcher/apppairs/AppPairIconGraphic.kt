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
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import com.sandboxr.launcher.DeviceProfile
import com.sandboxr.launcher.icons.BitmapInfo
import com.sandboxr.launcher.model.data.AppPairInfo

/**
 * Visual graphic area on an [AppPairIcon] where the dual-app preview is rendered.
 * Hosts the [AppPairIconDrawable] and manages dynamic profile updates, hover scaling, and redrawing.
 */
open class AppPairIconGraphic @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    companion object {
        @JvmStatic
        fun composeDrawable(
            appPairInfo: AppPairInfo,
            p: AppPairIconDrawingParams
        ): AppPairIconDrawable {
            val app1 = runCatching { appPairInfo.getFirstApp() }.getOrNull()
            val app2 = runCatching { appPairInfo.getSecondApp() }.getOrNull()

            val appIcon1: Drawable? = app1?.newIcon(p.context, BitmapInfo.FLAG_THEMED)
            val appIcon2: Drawable? = app2?.newIcon(p.context, BitmapInfo.FLAG_THEMED)

            val memberSize = p.memberIconSize.toInt()
            appIcon1?.setBounds(0, 0, memberSize, memberSize)
            appIcon2?.setBounds(0, 0, memberSize, memberSize)

            val (isApp1Launchable, isApp2Launchable) = runCatching {
                appPairInfo.isLaunchable(p.context)
            }.getOrDefault(Pair(true, true))

            if (!isApp1Launchable && appIcon1 is com.sandboxr.launcher.icons.FastBitmapDrawable) {
                appIcon1.setDisabled(true)
            }
            if (!isApp2Launchable && appIcon2 is com.sandboxr.launcher.icons.FastBitmapDrawable) {
                appIcon2.setDisabled(true)
            }

            val fullIconDrawable = AppPairIconDrawable(p, appIcon1, appIcon2)
            fullIconDrawable.setBounds(0, 0, p.iconSize, p.iconSize)
            return fullIconDrawable
        }
    }

    private lateinit var parentIcon: AppPairIcon
    private lateinit var drawParams: AppPairIconDrawingParams
    lateinit var drawable: AppPairIconDrawable

    fun init(icon: AppPairIcon, container: Int) {
        parentIcon = icon
        drawParams = AppPairIconDrawingParams(context, container)
        drawable = composeDrawable(icon.info, drawParams)

        val lp = (layoutParams as? LayoutParams) ?: LayoutParams(drawParams.iconSize, drawParams.iconSize)
        lp.gravity = Gravity.CENTER_HORIZONTAL
        lp.height = drawParams.iconSize
        lp.width = drawParams.iconSize
        layoutParams = lp
    }

    fun onDeviceProfileChanged(dp: DeviceProfile) {
        if (::drawParams.isInitialized) {
            drawParams.updateOrientation(dp)
            redraw()
        }
    }

    fun onTemporaryContainerChange(newContainer: Int?) {
        if (::drawParams.isInitialized && ::parentIcon.isInitialized) {
            drawParams.updateBgColor(newContainer ?: parentIcon.container)
            redraw()
        }
    }

    fun getIconBounds(outBounds: Rect) {
        if (!::drawParams.isInitialized || !::parentIcon.isInitialized) {
            outBounds.set(0, 0, width, height)
            return
        }
        outBounds.set(0, 0, drawParams.backgroundSize.toInt(), drawParams.backgroundSize.toInt())
        outBounds.offset(
            ((parentIcon.width - drawParams.backgroundSize) / 2).toInt(),
            (parentIcon.paddingTop + drawParams.standardIconPadding + drawParams.outerPadding).toInt()
        )
    }

    fun setHoverScale(scale: Float) {
        if (::drawParams.isInitialized) {
            drawParams.hoverScale = scale
            redraw()
        }
    }

    fun getHoverScale(): Float {
        return if (::drawParams.isInitialized) drawParams.hoverScale else 1f
    }

    fun redraw() {
        if (::parentIcon.isInitialized && ::drawParams.isInitialized) {
            drawable = composeDrawable(parentIcon.info, drawParams)
            invalidate()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (::drawable.isInitialized) {
            drawable.draw(canvas)
        }
    }
}
