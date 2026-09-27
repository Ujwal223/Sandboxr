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

package com.sandboxr.launcher

import java.util.Collections

/**
 * Central registry of files the Sandboxr Launcher writes to the application data directory.
 */
object LauncherFiles {

    private const val XML = ".xml"

    const val LAUNCHER_DB = "launcher.db"
    const val LAUNCHER_5_BY_8_DB = "launcher_5_by_8.db"
    const val LAUNCHER_6_BY_5_DB = "launcher_6_by_5.db"
    const val LAUNCHER_4_BY_5_DB = "launcher_4_by_5.db"
    const val LAUNCHER_4_BY_6_DB = "launcher_4_by_6.db"
    const val LAUNCHER_5_BY_6_DB = "launcher_5_by_6.db"
    const val LAUNCHER_4_BY_4_DB = "launcher_4_by_4.db"
    const val LAUNCHER_3_BY_3_DB = "launcher_3_by_3.db"
    const val LAUNCHER_2_BY_2_DB = "launcher_2_by_2.db"
    const val LAUNCHER_7_BY_3_DB = "launcher_7_by_3.db"
    const val LAUNCHER_8_BY_3_DB = "launcher_8_by_3.db"
    const val LAUNCHER_10_BY_5_DB = "launcher_10_by_5.db"
    const val LAUNCHER_11_BY_5_DB = "launcher_11_by_5.db"
    const val LAUNCHER_13_BY_6_DB = "launcher_13_by_6.db"
    const val LAUNCHER_16_BY_8_DB = "launcher_16_by_8.db"
    const val LAUNCHER_22_BY_11_DB = "launcher_22_by_11.db"

    const val BACKUP_DB = "backup.db"
    const val SHARED_PREFERENCES_KEY = "com.sandboxr.launcher.prefs"
    const val MANAGED_USER_PREFERENCES_KEY = "com.sandboxr.launcher.managedusers.prefs"
    const val DEVICE_PREFERENCES_KEY = "com.sandboxr.launcher.device.prefs"

    const val WIDGET_PREVIEWS_DB = "widgetpreviews.db"
    const val APP_ICONS_DB = "app_icons.db"

    @JvmField
    val GRID_DB_FILES: List<String> = Collections.unmodifiableList(
        listOf(
            LAUNCHER_DB,
            LAUNCHER_5_BY_8_DB,
            LAUNCHER_6_BY_5_DB,
            LAUNCHER_4_BY_5_DB,
            LAUNCHER_4_BY_6_DB,
            LAUNCHER_5_BY_6_DB,
            LAUNCHER_4_BY_4_DB,
            LAUNCHER_3_BY_3_DB,
            LAUNCHER_2_BY_2_DB,
            LAUNCHER_7_BY_3_DB,
            LAUNCHER_8_BY_3_DB,
            LAUNCHER_10_BY_5_DB,
            LAUNCHER_11_BY_5_DB,
            LAUNCHER_13_BY_6_DB,
            LAUNCHER_16_BY_8_DB,
            LAUNCHER_22_BY_11_DB
        )
    )

    @JvmField
    val OTHER_FILES: List<String> = Collections.unmodifiableList(
        listOf(
            BACKUP_DB,
            "$SHARED_PREFERENCES_KEY$XML",
            WIDGET_PREVIEWS_DB,
            "$MANAGED_USER_PREFERENCES_KEY$XML",
            "$DEVICE_PREFERENCES_KEY$XML",
            APP_ICONS_DB
        )
    )

    @JvmField
    val ALL_FILES: List<String> = Collections.unmodifiableList(
        GRID_DB_FILES + OTHER_FILES
    )
}
