/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.launcher3.icons;

/**
 * AOSP bridge for BitmapInfo.
 */
public class BitmapInfo extends com.sandboxr.launcher.icons.BitmapInfo {

    public static final com.sandboxr.launcher.icons.BitmapInfo LOW_RES_INFO = com.sandboxr.launcher.icons.BitmapInfo.LOW_RES_INFO;
    public static final int FLAG_THEMED = com.sandboxr.launcher.icons.BitmapInfo.FLAG_THEMED;
    public static final int FLAG_NO_BADGE = com.sandboxr.launcher.icons.BitmapInfo.FLAG_NO_BADGE;
    public static final int FLAG_SKIP_USER_BADGE = com.sandboxr.launcher.icons.BitmapInfo.FLAG_SKIP_USER_BADGE;
    public static final int FLAG_INSTANT = com.sandboxr.launcher.icons.BitmapInfo.FLAG_INSTANT;
    public static final int FLAG_WORK = com.sandboxr.launcher.icons.BitmapInfo.FLAG_WORK;
    public static final int FLAG_CLONED = com.sandboxr.launcher.icons.BitmapInfo.FLAG_CLONED;
    public static final int FLAG_PRIVATE = com.sandboxr.launcher.icons.BitmapInfo.FLAG_PRIVATE;

    public BitmapInfo(android.graphics.Bitmap icon, int color) {
        super(icon, color);
    }
}
