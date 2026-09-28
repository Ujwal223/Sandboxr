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
import com.sandboxr.launcher.LauncherPrefs
import com.sandboxr.launcher.LauncherPrefs.Companion.backedUpItem
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import com.sandboxr.launcher.graphics.theme.MonoIconThemeFactory.MONO_FACTORY_ID
import com.sandboxr.launcher.graphics.theme.MonoIconThemeFactory.MONO_THEME_CONTROLLER
import com.sandboxr.launcher.util.ListenableRef
import com.sandboxr.launcher.util.MutableListenableRef
import com.sandboxr.launcher.util.SafeCloseable
import java.util.concurrent.Executor
import javax.inject.Inject

/**
 * Manages launcher icon theme preference state and persistence.
 */
@LauncherAppSingleton
class ThemePreference @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: LauncherPrefs,
) : ListenableRef<ThemePreference.ThemeValue?> {

    private val themePref = MutableListenableRef<ThemeValue?>(null)

    init {
        val savedValue = prefs.get(THEME_ID)
        val parsed = parsePrefValue(savedValue)
        themePref.dispatchValue(parsed)
    }

    override val value: ThemeValue?
        get() = themePref.value

    override fun forEach(executor: Executor, action: (ThemeValue?) -> Unit): SafeCloseable {
        return themePref.forEach(executor, action)
    }

    fun setValue(value: ThemeValue?) {
        synchronized(themePref) {
            prefs.put(THEME_ID, value?.toString() ?: "")
            themePref.dispatchValue(value)
        }
    }

    data class ThemeValue(val factoryId: String, val themeId: String) {
        override fun toString(): String = "$factoryId:$themeId"
    }

    companion object {
        private const val KEY_ICON_THEME = "icon_theme_id"
        private val THEME_ID = backedUpItem(KEY_ICON_THEME, "")

        val MONO_THEME_VALUE = ThemeValue(MONO_FACTORY_ID, MONO_THEME_CONTROLLER.themeID)

        fun parsePrefValue(value: String): ThemeValue? {
            if (value.isEmpty()) return null
            val colonIndex = value.indexOf(':')
            if (colonIndex == -1) return null
            return ThemeValue(value.substring(0, colonIndex), value.substring(colonIndex + 1))
        }
    }
}
