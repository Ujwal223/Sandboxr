/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.launcher3.pm

import android.content.Context
import com.sandboxr.launcher.util.DaggerSingletonObject

typealias UserManagerState = com.sandboxr.launcher.pm.UserManagerState

open class UserCache {
    companion object {
        @JvmField
        var INSTANCE = DaggerSingletonObject { com.sandboxr.launcher.pm.UserCache.getInstance(it.applicationContext()) }

        @JvmStatic
        fun getInstance(context: Context): com.sandboxr.launcher.pm.UserCache =
            com.sandboxr.launcher.pm.UserCache.getInstance(context)
    }

    typealias CachedUserInfo = com.sandboxr.launcher.pm.UserCache.CachedUserInfo
    typealias UserChangeEvent = com.sandboxr.launcher.pm.UserCache.UserChangeEvent
}
