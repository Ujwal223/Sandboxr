/*
 * Copyright (C) 2025 The Android Open Source Project
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
import android.graphics.drawable.Drawable
import com.sandboxr.launcher.icons.BaseIconFactory
import com.sandboxr.launcher.icons.BitmapInfo

interface IconThemeFactory {
    fun createController(themeId: String): IconThemeController?
}

class MonoIconThemeController(
    val shouldForceThemeIcon: Boolean = true,
) : IconThemeController {

    override val themeID: String = "monochrome"

    override fun createThemedBitmap(
        icon: Drawable,
        bitmapInfo: BitmapInfo,
        factory: BaseIconFactory,
    ): BitmapInfo {
        return factory.createBadgedIconBitmap(icon, isThemed = true)
    }

    override fun createThemedAdaptiveIcon(
        context: Context,
        icon: Drawable,
        bitmapInfo: BitmapInfo?,
    ): Drawable {
        return IconThemer.applyThemedPalette(context, icon)
    }
}

object MonoIconThemeFactory : IconThemeFactory {
    const val MONO_FACTORY_ID = "mono-icons"
    val MONO_THEME_CONTROLLER = MonoIconThemeController(shouldForceThemeIcon = true)

    override fun createController(themeId: String): IconThemeController? =
        if (themeId == MONO_THEME_CONTROLLER.themeID) MONO_THEME_CONTROLLER else null
}
