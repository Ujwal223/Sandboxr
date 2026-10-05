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

package com.sandboxr.launcher.logging

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Monitors and logs launcher settings and preference modifications.
 */
class SettingsChangeLogger(
    private val context: Context,
    private val statsLogManager: StatsLogManager = StatsLogManager.newInstance(context)
) : SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == null) return
        Log.d(TAG, "Launcher setting changed: $key")
        statsLogManager
            .logger()
            .withEditText(key)
            .log(StatsLogManager.LauncherEvent.LAUNCHER_SETTINGS_CHANGE)
    }

    companion object {
        private const val TAG = "SettingsChangeLogger"
    }
}
