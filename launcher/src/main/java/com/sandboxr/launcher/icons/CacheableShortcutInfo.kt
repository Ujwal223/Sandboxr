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

package com.sandboxr.launcher.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import com.sandboxr.launcher.icons.BitmapInfo
import com.sandboxr.launcher.util.PackageUserKey

/**
 * Wrapper over ShortcutInfo to provide extra information related to ShortcutInfo.
 */
class CacheableShortcutInfo(
    val shortcutInfo: ShortcutInfo,
    val userHandle: UserHandle = shortcutInfo.userHandle,
) {
    companion object {
        @JvmStatic
        fun convertShortcutsToCacheableShortcuts(
            shortcuts: List<ShortcutInfo>,
            activities: List<LauncherActivityInfo>,
        ): List<CacheableShortcutInfo> {
            return shortcuts.map { CacheableShortcutInfo(it) }
        }
    }
}

object CacheableShortcutCachingLogic {
    @JvmStatic
    fun getComponent(item: CacheableShortcutInfo): ComponentName? {
        return item.shortcutInfo.activity
    }

    @JvmStatic
    fun getUser(item: CacheableShortcutInfo): UserHandle {
        return item.userHandle
    }
}
