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

package com.sandboxr.launcher.graphics

import android.content.Context
import com.sandboxr.launcher.LauncherPrefs
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import com.sandboxr.launcher.graphics.theme.IconThemer
import com.sandboxr.launcher.graphics.theme.ThemePreference
import com.sandboxr.launcher.util.DaggerSingletonObject
import com.sandboxr.launcher.util.ListenableRef
import com.sandboxr.launcher.util.MutableListenableRef
import com.sandboxr.launcher.util.SafeCloseable
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

/**
 * Central manager coordinate launcher icon shapes, dynamic Material You theming,
 * and theme change listener callbacks.
 */
@LauncherAppSingleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: LauncherPrefs,
    val themePreference: ThemePreference,
) {
    private val _iconShapeData = MutableListenableRef(IconShape(context))
    val iconShapeData: ListenableRef<IconShape> = _iconShapeData.asListenable()

    private val listeners = CopyOnWriteArrayList<ThemeChangeListener>()

    val isIconThemeEnabled: Boolean
        get() = themePreference.value != null

    val currentPalette: IconThemer.ThemedPalette
        get() = IconThemer.resolveThemedPalette(context)

    fun addChangeListener(listener: ThemeChangeListener): SafeCloseable {
        listeners.add(listener)
        return SafeCloseable { listeners.remove(listener) }
    }

    fun removeChangeListener(listener: ThemeChangeListener) {
        listeners.remove(listener)
    }

    fun notifyThemeChanged() {
        listeners.forEach { it.onThemeChanged() }
    }

    fun setIconShape(shapeName: String) {
        val shape = IconShape(context)
        _iconShapeData.dispatchValue(shape)
        notifyThemeChanged()
    }

    fun interface ThemeChangeListener {
        fun onThemeChanged()
    }

    companion object {
        @JvmField
        val INSTANCE = DaggerSingletonObject {
            val appCtx = it.applicationContext()
            val prefs = LauncherPrefs.get(appCtx)
            val themePref = ThemePreference(appCtx, prefs)
            ThemeManager(appCtx, prefs, themePref)
        }
    }
}
