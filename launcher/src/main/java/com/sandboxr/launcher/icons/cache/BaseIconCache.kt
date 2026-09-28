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

package com.sandboxr.launcher.icons.cache

import android.content.ComponentName
import android.content.Context
import android.os.UserHandle
import com.sandboxr.launcher.icons.BitmapInfo
import com.sandboxr.launcher.util.ComponentKey

/**
 * Base cache of application icons and titles.
 */
open class BaseIconCache(
    protected val context: Context? = null,
    protected val dbFileName: String? = null,
    protected val bgLooper: Any? = null,
    protected val iconDpi: Int = 0,
    protected val iconPixelSize: Int = 0,
    protected val inMemoryCache: Boolean = true,
    protected val iconProvider: Any? = null,
) {
    companion object {
        const val COLUMN_ROWID = "rowid"
        const val COLUMN_COMPONENT = "componentName"
        const val COLUMN_FRESHNESS_ID = "freshnessId"
        const val EMPTY_CLASS_NAME = ".none"

        @JvmStatic
        fun getPackageKey(packageName: String, user: UserHandle): ComponentKey {
            return ComponentKey(ComponentName(packageName, packageName + EMPTY_CLASS_NAME), user)
        }
    }

    open class CacheEntry {
        @JvmField var bitmap: BitmapInfo = BitmapInfo.LOW_RES_INFO
        @JvmField var title: CharSequence? = ""
        @JvmField var contentDescription: CharSequence? = ""
    }
}
