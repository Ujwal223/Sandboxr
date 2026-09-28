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

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle
import com.sandboxr.launcher.icons.BitmapInfo
import com.sandboxr.launcher.icons.CacheableShortcutInfo
import com.sandboxr.launcher.icons.LauncherIcons
import com.sandboxr.launcher.model.data.PackageItemInfo

interface CachedObject {
    val componentName: ComponentName
    val user: UserHandle
}

/**
 * Generic logic interface for extracting components, titles, and icons from varying model items.
 */
interface CachingLogic<T> {
    fun getComponent(item: T): ComponentName
    fun getUser(item: T): UserHandle
    fun getLabel(item: T): CharSequence
    fun loadIcon(context: Context, item: T): BitmapInfo
    fun addToDatabase(values: ContentValues, item: T) {}
}

class LauncherActivityCachingLogic : CachingLogic<LauncherActivityInfo> {
    override fun getComponent(item: LauncherActivityInfo): ComponentName = item.componentName

    override fun getUser(item: LauncherActivityInfo): UserHandle = item.user

    override fun getLabel(item: LauncherActivityInfo): CharSequence = item.label

    override fun loadIcon(context: Context, item: LauncherActivityInfo): BitmapInfo {
        LauncherIcons.obtain(context).use { factory ->
            return factory.createBadgedIconBitmap(item.getIcon(0), item.user)
        }
    }

    companion object {
        @JvmField
        val INSTANCE = LauncherActivityCachingLogic()
    }
}

class CachedObjectCachingLogic : CachingLogic<CachedObject> {
    override fun getComponent(item: CachedObject): ComponentName = item.componentName

    override fun getUser(item: CachedObject): UserHandle = item.user

    override fun getLabel(item: CachedObject): CharSequence = item.componentName.shortClassName

    override fun loadIcon(context: Context, item: CachedObject): BitmapInfo = BitmapInfo.LOW_RES_INFO

    companion object {
        @JvmField
        val INSTANCE = CachedObjectCachingLogic()
    }
}

class PackageItemCachingLogic : CachingLogic<PackageItemInfo> {
    override fun getComponent(item: PackageItemInfo): ComponentName =
        ComponentName(item.packageName, item.packageName + ".Main")

    override fun getUser(item: PackageItemInfo): UserHandle = item.user

    override fun getLabel(item: PackageItemInfo): CharSequence = item.title ?: item.packageName

    override fun loadIcon(context: Context, item: PackageItemInfo): BitmapInfo {
        return item.bitmap
    }

    companion object {
        @JvmField
        val INSTANCE = PackageItemCachingLogic()
    }
}

class ShortcutCachingLogic : CachingLogic<CacheableShortcutInfo> {
    override fun getComponent(item: CacheableShortcutInfo): ComponentName =
        item.shortcutInfo.activity ?: ComponentName(item.shortcutInfo.`package`, item.shortcutInfo.id)

    override fun getUser(item: CacheableShortcutInfo): UserHandle = item.userHandle

    override fun getLabel(item: CacheableShortcutInfo): CharSequence = item.shortcutInfo.shortLabel ?: ""

    override fun loadIcon(context: Context, item: CacheableShortcutInfo): BitmapInfo {
        return BitmapInfo.LOW_RES_INFO
    }

    companion object {
        @JvmField
        val INSTANCE = ShortcutCachingLogic()
    }
}
