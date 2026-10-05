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

package com.android.launcher3.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ApplicationInfo.FLAG_EXTERNAL_STORAGE
import android.content.pm.ApplicationInfo.FLAG_INSTALLED
import android.content.pm.ApplicationInfo.FLAG_SUSPENDED
import android.content.pm.ApplicationInfo.FLAG_SYSTEM
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.UserHandle
import kotlin.LazyThreadSafetyMode.NONE

class ApplicationInfoWrapper private constructor(provider: () -> ApplicationInfo?) {

    constructor(appInfo: ApplicationInfo?) : this({ appInfo })

    constructor(
        ctx: Context,
        pkg: String,
        user: UserHandle,
    ) : this({
        try {
            ctx.getSystemService(LauncherApps::class.java)
                ?.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES, user)
                ?.let { ai ->
                    if (ai.enabled && (ai.flags and FLAG_INSTALLED) != 0) {
                        ai
                    } else {
                        null
                    }
                }
        } catch (e: Exception) {
            null
        }
    })

    constructor(
        ctx: Context,
        intent: Intent,
    ) : this(
        provider@{
            try {
                val pm = ctx.packageManager
                val packageName: String =
                    intent.component?.packageName
                        ?: intent.getPackage()
                        ?: return@provider pm.resolveActivity(
                                intent,
                                PackageManager.MATCH_DEFAULT_ONLY,
                            )
                            ?.activityInfo
                            ?.applicationInfo
                pm.getApplicationInfo(packageName, 0)
            } catch (e: Exception) {
                null
            }
        }
    )

    private val appInfo: ApplicationInfo? by lazy(NONE, provider)

    private fun hasFlag(flag: Int) = appInfo?.let { (it.flags and flag) != 0 } ?: false

    fun isOnSdCard(): Boolean = hasFlag(FLAG_EXTERNAL_STORAGE)

    fun isInstalled(): Boolean = hasFlag(FLAG_INSTALLED)

    fun isSuspended(): Boolean = hasFlag(FLAG_INSTALLED) && hasFlag(FLAG_SUSPENDED)

    fun isArchived(): Boolean = false

    fun isSystem(): Boolean = hasFlag(FLAG_SYSTEM)

    fun getInfo(): ApplicationInfo? = appInfo

    fun isAppLockSupported(): Boolean = false

    fun isAppLockEnabled(): Boolean = false
}
