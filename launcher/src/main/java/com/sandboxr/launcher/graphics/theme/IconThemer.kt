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

package com.sandboxr.launcher.graphics.theme

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

/**
 * Material You Dynamic Icon Theming engine for Sandboxr Launcher.
 * Transforms standard application icons and adaptive icons into monochrome
 * themed icons based on device Monet dynamic palette.
 */
object IconThemer {

    data class ThemedPalette(
        @ColorInt val foregroundColor: Int,
        @ColorInt val backgroundColor: Int,
    )

    /**
     * Resolves the current Material You dynamic colors for icons.
     * Uses Android 12+ system resources (system_accent1_100, system_neutral1_900)
     * with graceful fallback to Sandboxr cyber/dark palette.
     */
    @JvmStatic
    fun resolveThemedPalette(context: Context): ThemedPalette {
        val isDarkMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        val res = context.resources
        val primaryColorResId = res.getIdentifier("system_accent1_100", "color", "android")
        val bgColorResId = res.getIdentifier("system_neutral1_900", "color", "android")

        val fgColor = if (primaryColorResId != 0) {
            try {
                context.getColor(primaryColorResId)
            } catch (e: Exception) {
                if (isDarkMode) Color.parseColor("#00E5FF") else Color.parseColor("#006064")
            }
        } else {
            if (isDarkMode) Color.parseColor("#00E5FF") else Color.parseColor("#006064")
        }

        val bgColor = if (bgColorResId != 0) {
            try {
                context.getColor(bgColorResId)
            } catch (e: Exception) {
                if (isDarkMode) Color.parseColor("#12121A") else Color.parseColor("#ECEFF1")
            }
        } else {
            if (isDarkMode) Color.parseColor("#12121A") else Color.parseColor("#ECEFF1")
        }

        return ThemedPalette(foregroundColor = fgColor, backgroundColor = bgColor)
    }

    /**
     * Applies Material You palette to the given drawable.
     * For [AdaptiveIconDrawable], replaces the background with the themed background
     * and tints the foreground (or monochrome layer if present).
     */
    @JvmStatic
    fun applyThemedPalette(context: Context, drawable: Drawable): Drawable {
        val palette = resolveThemedPalette(context)
        val bgDrawable = ColorDrawable(palette.backgroundColor)

        if (drawable is AdaptiveIconDrawable) {
            // Android 13+ supports getMonochrome()
            var fg = drawable.foreground
            try {
                val monoMethod = AdaptiveIconDrawable::class.java.getMethod("getMonochrome")
                val monoDrawable = monoMethod.invoke(drawable) as? Drawable
                if (monoDrawable != null) {
                    fg = monoDrawable
                }
            } catch (ignored: Exception) {
                // Pre-Android 13 or method not accessible
            }

            val tintedFg = fg?.mutate()?.apply {
                colorFilter = android.graphics.PorterDuffColorFilter(palette.foregroundColor, PorterDuff.Mode.SRC_IN)
            }

            return AdaptiveIconDrawable(bgDrawable, tintedFg)
        }

        // Standard drawable: tint with foreground color
        val tinted = drawable.mutate().apply {
            colorFilter = android.graphics.PorterDuffColorFilter(palette.foregroundColor, PorterDuff.Mode.SRC_IN)
        }
        return AdaptiveIconDrawable(bgDrawable, tinted)
    }
}
