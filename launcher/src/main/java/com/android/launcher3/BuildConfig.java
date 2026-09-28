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

package com.android.launcher3;

public final class BuildConfig {
    public static final boolean DEBUG = com.sandboxr.launcher.BuildConfig.DEBUG;
    public static final String LIBRARY_PACKAGE_NAME = com.sandboxr.launcher.BuildConfig.LIBRARY_PACKAGE_NAME;
    public static final String BUILD_TYPE = com.sandboxr.launcher.BuildConfig.BUILD_TYPE;
    public static final boolean IS_STUDIO_BUILD = com.sandboxr.launcher.BuildConfig.IS_STUDIO_BUILD;
    public static final boolean WIDGETS_ENABLED = com.sandboxr.launcher.BuildConfig.WIDGETS_ENABLED;
    public static final boolean IS_DEBUG_DEVICE = DEBUG || "userdebug".equals(android.os.Build.TYPE) || "eng".equals(android.os.Build.TYPE);

    private BuildConfig() {}
}
