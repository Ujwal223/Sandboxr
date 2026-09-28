/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.os.UserHandle
import java.util.HashSet
import java.util.function.BiConsumer

/**
 * Handler for batch updating icon cache.
 */
open class IconCacheUpdateHandler {
    open fun addPackagesToIgnore(user: UserHandle, packageName: String) {}
    open fun <T> updateIcons(
        items: List<T>,
        cachingLogic: Any?,
        onPackageIconsUpdated: BiConsumer<HashSet<String>, UserHandle>? = null,
    ) {}
    open fun finish() {}
}
