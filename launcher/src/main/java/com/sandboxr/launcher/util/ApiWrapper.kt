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

package com.sandboxr.launcher.util

import android.app.Person
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Build
import android.os.UserHandle

open class ApiWrapper(protected val context: Context) {

    open fun isNonResizeableActivity(lai: LauncherActivityInfo): Boolean = false

    open fun supportsMultiInstance(lai: LauncherActivityInfo): Boolean = true

    open fun getActivityOverrides(): Map<String, LauncherActivityInfo> = emptyMap()

    open fun getAppMarketActivityIntent(packageName: String, user: UserHandle): Intent? =
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))

    open fun getPersons(shortcutInfo: ShortcutInfo): Array<Person> = emptyArray()

    companion object {
        class Initializer<T>(private val factory: (Context) -> T) {
            private var instance: T? = null

            operator fun get(context: Context): T {
                return instance ?: synchronized(this) {
                    instance ?: factory(context.applicationContext).also { instance = it }
                }
            }
        }

        @JvmField
        val INSTANCE = Initializer { context -> ApiWrapper(context) }
    }
}
