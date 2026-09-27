/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.launcher3.graphics;

import android.content.Context;

/**
 * AOSP backwards-compatibility bridge for {@link com.sandboxr.launcher.graphics.BitmapCreationCheck}.
 */
public final class BitmapCreationCheck {

    public static final boolean ENABLED = com.sandboxr.launcher.graphics.BitmapCreationCheck.ENABLED;

    public static void startTracking(Context context) {
        com.sandboxr.launcher.graphics.BitmapCreationCheck.startTracking(context);
    }

    private BitmapCreationCheck() {}
}
