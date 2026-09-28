/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.launcher3.graphics

import android.content.Context
import com.android.launcher3.dagger.ApplicationContext
import com.android.launcher3.dagger.LauncherAppSingleton
import com.android.launcher3.icons.IconShape
import com.android.launcher3.util.DaggerSingletonTracker
import com.android.launcher3.util.ListenableRef
import com.android.launcher3.util.MutableListenableRef
import com.android.launcher3.util.SafeCloseable
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

/**
 * Stub ThemeManager: satisfies the Dagger graph and ModelInitializer dependency.
 * The full implementation with icon shape theming will be wired in a later phase.
 */
@LauncherAppSingleton
class ThemeManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val lifecycleTracker: DaggerSingletonTracker,
) {
    companion object {
        @JvmField
        val INSTANCE = com.sandboxr.launcher.util.DaggerSingletonObject {
            ThemeManager(it.applicationContext(), DaggerSingletonTracker())
        }
    }

    private val _iconShapeData = MutableListenableRef<IconShape?>(null)
    val iconShapeData: ListenableRef<IconShape?> = _iconShapeData.asListenable()

    fun getFileShapeData(): IconShape? = null
    fun isIconThemeEnabled(): Boolean = false

    private val listeners = CopyOnWriteArrayList<ThemeChangeListener>()

    fun addChangeListener(listener: ThemeChangeListener): SafeCloseable {
        listeners.add(listener)
        return SafeCloseable { listeners.remove(listener) }
    }

    fun removeChangeListener(listener: ThemeChangeListener) {
        listeners.remove(listener)
    }

    /** Notifies all registered listeners that the theme changed. */
    fun notifyThemeChanged() {
        listeners.forEach { it.onThemeChanged() }
    }

    /** Callback interface for theme change events. */
    fun interface ThemeChangeListener {
        fun onThemeChanged()
    }
}
