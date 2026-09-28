/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.sandboxr.launcher.pm

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager

/**
 * Utility helper for managing Android 8.0+ pinned shortcut and pinned widget requests.
 */
object PinRequestHelper {

    @JvmStatic
    fun getPinItemRequest(intent: Intent): PinItemRequest? {
        val extra = intent.getParcelableExtra<PinItemRequest>(LauncherApps.EXTRA_PIN_ITEM_REQUEST)
        return if (extra is PinItemRequest) extra else null
    }

    @JvmStatic
    fun createRequestForShortcut(context: Context, info: ShortcutInfo): PinItemRequest? {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return null
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return null
        val resultIntent = shortcutManager.createShortcutResultIntent(info)
        return launcherApps.getPinItemRequest(resultIntent)
    }
}
