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

package com.sandboxr.launcher.icons.cache

data class CacheLookupFlag(
    val flags: Int = 0
) {
    fun useLowRes(): Boolean = (flags and FLAG_USE_LOW_RES) != 0
    @JvmOverloads
    fun withUseLowRes(enable: Boolean = true): CacheLookupFlag =
        if (enable) CacheLookupFlag(flags or FLAG_USE_LOW_RES)
        else CacheLookupFlag(flags and FLAG_USE_LOW_RES.inv())

    fun usePackageIcon(): Boolean = (flags and FLAG_USE_PACKAGE_ICON) != 0
    @JvmOverloads
    fun withUsePackageIcon(enable: Boolean = true): CacheLookupFlag =
        if (enable) CacheLookupFlag(flags or FLAG_USE_PACKAGE_ICON)
        else CacheLookupFlag(flags and FLAG_USE_PACKAGE_ICON.inv())

    fun skipAddToMemCache(): Boolean = (flags and FLAG_SKIP_ADD_TO_MEM_CACHE) != 0
    @JvmOverloads
    fun withSkipAddToMemCache(enable: Boolean = true): CacheLookupFlag =
        if (enable) CacheLookupFlag(flags or FLAG_SKIP_ADD_TO_MEM_CACHE)
        else CacheLookupFlag(flags and FLAG_SKIP_ADD_TO_MEM_CACHE.inv())

    fun isLowRes(): Boolean = useLowRes()
    @JvmOverloads
    fun withLowRes(enable: Boolean = true): CacheLookupFlag = withUseLowRes(enable)

    @JvmOverloads
    fun withThemeIcon(enable: Boolean = true): CacheLookupFlag = this

    companion object {
        private const val FLAG_USE_LOW_RES = 1 shl 0
        private const val FLAG_USE_PACKAGE_ICON = 1 shl 1
        private const val FLAG_SKIP_ADD_TO_MEM_CACHE = 1 shl 2

        @JvmField
        val DEFAULT_LOOKUP_FLAG = CacheLookupFlag(0)
    }
}
