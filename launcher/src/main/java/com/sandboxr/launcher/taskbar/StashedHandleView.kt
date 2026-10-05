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

package com.sandboxr.launcher.taskbar

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.sandboxr.launcher.R

/**
 * Handle rendered when Taskbar is stashed into a slim bottom pill.
 */
class StashedHandleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    @ColorInt private val stashedHandleLightColor: Int =
        ContextCompat.getColor(context, R.color.taskbar_stashed_handle_light_color)
    @ColorInt private val stashedHandleDarkColor: Int =
        ContextCompat.getColor(context, R.color.taskbar_stashed_handle_dark_color)

    val sampledRegion = Rect()
    private val tmpArr = IntArray(2)
    var isRegionDark: Boolean? = null
        private set

    init {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.height / 2f)
            }
        }
        clipToOutline = true
        setBackgroundColor(stashedHandleDarkColor)
    }

    fun updateSampledRegion(bounds: Rect) {
        getLocationOnScreen(tmpArr)
        tmpArr[0] -= Math.round(translationX)
        tmpArr[1] -= Math.round(translationY)
        sampledRegion.set(bounds)
        sampledRegion.offset(tmpArr[0], tmpArr[1])
    }

    fun updateHandleColor(isDark: Boolean, animate: Boolean = true) {
        if (isRegionDark == isDark) return
        isRegionDark = isDark
        val targetColor = if (isDark) stashedHandleLightColor else stashedHandleDarkColor
        if (animate) {
            setBackgroundColor(targetColor)
        } else {
            setBackgroundColor(targetColor)
        }
    }
}
