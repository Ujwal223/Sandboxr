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

/**
 * Enum representing the navigation mode of the device.
 *
 * Android supports three distinct navigation paradigms:
 * - [THREE_BUTTONS]: The legacy three-button navbar (back, home, recents).
 * - [TWO_BUTTONS]: Two-button navigation (back + home).
 * - [NO_BUTTON]: Fully gesture-based navigation (swipe home, swipe recents).
 */
enum class NavigationMode(
    /** True if this mode uses gesture-based navigation (swiping from screen edges). */
    @JvmField val hasGestures: Boolean,
    /** Resource identifier suffix used to load mode-specific resources. */
    @JvmField val resValue: Int,
) {
    THREE_BUTTONS(false, 0),
    TWO_BUTTONS(true, 1),
    NO_BUTTON(true, 2);

    companion object {
        /**
         * Returns the [NavigationMode] corresponding to the given resource integer value,
         * defaulting to [THREE_BUTTONS] if the value is unrecognized.
         */
        @JvmStatic
        fun fromResValue(value: Int): NavigationMode =
            entries.firstOrNull { it.resValue == value } ?: THREE_BUTTONS
    }
}
