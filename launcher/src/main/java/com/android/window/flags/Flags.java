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

package com.android.window.flags;

public final class Flags {
    public static final String FLAG_ENABLE_TASKBAR_OVERFLOW = "com.android.window.flags.enable_taskbar_overflow";
    public static final String FLAG_ENABLE_OVERFLOW_BUTTON_FOR_TASKBAR_PINNED_ITEMS = "com.android.window.flags.enable_overflow_button_for_taskbar_pinned_items";
    public static final String FLAG_ENABLE_PINNING_APP_WITH_CONTEXT_MENU = "com.android.window.flags.enable_pinning_app_with_context_menu";
    public static final String FLAG_ENABLE_DESKTOP_WINDOWING_MODE = "com.android.window.flags.enable_desktop_windowing_mode";

    private Flags() {}

    public static boolean enableNonDefaultDisplaySplitBugfix() {
        return false;
    }

    public static boolean enableOverflowButtonForTaskbarPinnedItems() {
        return false;
    }

    public static boolean enableTaskbarOverflow() {
        return false;
    }

    public static boolean enablePinningAppWithContextMenu() {
        return false;
    }

    public static boolean enableDesktopWindowingMode() {
        return false;
    }

    public static boolean betterDeskDeactivationInRecentsTransition() {
        return false;
    }

    public static boolean useInputReportedFocusForAccessibility() {
        return false;
    }
}
