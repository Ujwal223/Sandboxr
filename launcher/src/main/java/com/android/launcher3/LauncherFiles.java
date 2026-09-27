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

package com.android.launcher3;

import java.util.List;

/**
 * AOSP backwards-compatibility bridge for {@link com.sandboxr.launcher.LauncherFiles}.
 */
public final class LauncherFiles {

    public static final String LAUNCHER_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_DB;
    public static final String LAUNCHER_5_BY_8_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_5_BY_8_DB;
    public static final String LAUNCHER_6_BY_5_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_6_BY_5_DB;
    public static final String LAUNCHER_4_BY_5_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_4_BY_5_DB;
    public static final String LAUNCHER_4_BY_6_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_4_BY_6_DB;
    public static final String LAUNCHER_5_BY_6_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_5_BY_6_DB;
    public static final String LAUNCHER_4_BY_4_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_4_BY_4_DB;
    public static final String LAUNCHER_3_BY_3_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_3_BY_3_DB;
    public static final String LAUNCHER_2_BY_2_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_2_BY_2_DB;
    public static final String LAUNCHER_7_BY_3_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_7_BY_3_DB;
    public static final String LAUNCHER_8_BY_3_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_8_BY_3_DB;
    public static final String LAUNCHER_10_BY_5_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_10_BY_5_DB;
    public static final String LAUNCHER_11_BY_5_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_11_BY_5_DB;
    public static final String LAUNCHER_13_BY_6_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_13_BY_6_DB;
    public static final String LAUNCHER_16_BY_8_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_16_BY_8_DB;
    public static final String LAUNCHER_22_BY_11_DB = com.sandboxr.launcher.LauncherFiles.LAUNCHER_22_BY_11_DB;

    public static final String BACKUP_DB = com.sandboxr.launcher.LauncherFiles.BACKUP_DB;
    public static final String SHARED_PREFERENCES_KEY = com.sandboxr.launcher.LauncherFiles.SHARED_PREFERENCES_KEY;
    public static final String MANAGED_USER_PREFERENCES_KEY = com.sandboxr.launcher.LauncherFiles.MANAGED_USER_PREFERENCES_KEY;
    public static final String DEVICE_PREFERENCES_KEY = com.sandboxr.launcher.LauncherFiles.DEVICE_PREFERENCES_KEY;

    public static final String WIDGET_PREVIEWS_DB = com.sandboxr.launcher.LauncherFiles.WIDGET_PREVIEWS_DB;
    public static final String APP_ICONS_DB = com.sandboxr.launcher.LauncherFiles.APP_ICONS_DB;

    public static final List<String> GRID_DB_FILES = com.sandboxr.launcher.LauncherFiles.GRID_DB_FILES;
    public static final List<String> OTHER_FILES = com.sandboxr.launcher.LauncherFiles.OTHER_FILES;
    public static final List<String> ALL_FILES = com.sandboxr.launcher.LauncherFiles.ALL_FILES;

    private LauncherFiles() {}
}
