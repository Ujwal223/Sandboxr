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

package com.sandboxr.launcher;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

/**
 * Listener for changes in Launcher preferences.
 */
public interface LauncherPrefChangeListener extends OnSharedPreferenceChangeListener {

    /** Callback invoked when the preference for key has changed. */
    void onPrefChanged(String key);

    @Override
    default void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        onPrefChanged(key);
    }
}
