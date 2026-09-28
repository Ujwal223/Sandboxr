/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.launcher3

import android.content.Context
import com.sandboxr.launcher.InvariantDeviceProfile

/** Compatibility class for LauncherAppState */
class LauncherAppState private constructor(private val context: Context) {

    fun getIconCache(): com.sandboxr.launcher.icons.IconCache =
        com.sandboxr.launcher.icons.IconCache(context)

    companion object {
        @JvmStatic
        fun getIDP(context: Context): InvariantDeviceProfile =
            InvariantDeviceProfile.INSTANCE(context)

        @JvmStatic
        fun getInstance(context: Context): LauncherAppState =
            LauncherAppState(context.applicationContext)
    }
}
