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

package com.sandboxr.launcher.util

import android.view.View

/**
 * Utility class to manage various window flags to control system UI (status bar & nav bar).
 */
open class SystemUiController(private val targetView: View) {

    private val states = kotlin.IntArray(5)

    fun updateUiState(uiState: Int, isLight: Boolean) {
        val flags = if (isLight) {
            FLAG_LIGHT_NAV or FLAG_LIGHT_STATUS
        } else {
            FLAG_DARK_NAV or FLAG_DARK_STATUS
        }
        updateUiState(uiState, flags)
    }

    @Suppress("DEPRECATION")
    fun updateUiState(uiState: Int, flags: Int) {
        if (uiState < 0 || uiState >= states.size) return
        if (states[uiState] == flags) return

        states[uiState] = flags
        val oldFlags = targetView.systemUiVisibility
        var newFlags = oldFlags
        for (stateFlag in states) {
            newFlags = getSysUiVisibilityFlags(stateFlag, newFlags)
        }
        if (newFlags != oldFlags) {
            targetView.systemUiVisibility = newFlags
        }
    }

    @Suppress("DEPRECATION")
    fun getBaseSysuiVisibility(): Int {
        return getSysUiVisibilityFlags(states[UI_STATE_BASE_WINDOW], targetView.systemUiVisibility)
    }

    @Suppress("DEPRECATION")
    private fun getSysUiVisibilityFlags(stateFlag: Int, currentVisibility: Int): Int {
        var result = currentVisibility
        if ((stateFlag and FLAG_LIGHT_NAV) != 0) {
            result = result or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else if ((stateFlag and FLAG_DARK_NAV) != 0) {
            result = result and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }

        if ((stateFlag and FLAG_LIGHT_STATUS) != 0) {
            result = result or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else if ((stateFlag and FLAG_DARK_STATUS) != 0) {
            result = result and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
        return result
    }

    companion object {
        const val UI_STATE_BASE_WINDOW = 0
        const val UI_STATE_SCRIM_VIEW = 1
        const val UI_STATE_WIDGET_BOTTOM_SHEET = 2
        const val UI_STATE_FULLSCREEN_TASK = 3
        const val UI_STATE_ALL_APPS = 4

        const val FLAG_LIGHT_NAV = 1 shl 0
        const val FLAG_DARK_NAV = 1 shl 1
        const val FLAG_LIGHT_STATUS = 1 shl 2
        const val FLAG_DARK_STATUS = 1 shl 3
    }
}
