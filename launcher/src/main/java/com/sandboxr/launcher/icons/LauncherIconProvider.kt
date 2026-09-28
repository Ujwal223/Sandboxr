/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.sandboxr.launcher.icons

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.graphics.drawable.Drawable
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import com.sandboxr.launcher.graphics.theme.IconThemer
import javax.inject.Inject

/**
 * Extension of [IconProvider] providing themed icon support and calendar/clock dynamic updating.
 */
@LauncherAppSingleton
open class LauncherIconProvider @Inject constructor(
    @ApplicationContext context: Context,
) : IconProvider(context) {

    private var isThemedIconsEnabled: Boolean = false

    fun setThemedIconsEnabled(enabled: Boolean) {
        this.isThemedIconsEnabled = enabled
    }

    override fun getIcon(packageName: String): Drawable? {
        val base = super.getIcon(packageName) ?: return null
        if (isThemedIconsEnabled && context != null) {
            return IconThemer.applyThemedPalette(context, base)
        }
        return base
    }

    override fun getIcon(activityInfo: LauncherActivityInfo, iconDpi: Int): Drawable? {
        val base = super.getIcon(activityInfo, iconDpi) ?: return null
        if (isThemedIconsEnabled && context != null) {
            return IconThemer.applyThemedPalette(context, base)
        }
        return base
    }
}
