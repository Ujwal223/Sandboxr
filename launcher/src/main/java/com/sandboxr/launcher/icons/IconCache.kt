/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.UserHandle
import android.util.LruCache
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import com.sandboxr.launcher.icons.cache.BaseIconCache
import com.sandboxr.launcher.icons.cache.CacheLookupFlag
import com.sandboxr.launcher.icons.cache.IconCacheUpdateHandler
import com.sandboxr.launcher.model.data.IconRequestInfo
import com.sandboxr.launcher.model.data.ItemInfoWithIcon
import com.sandboxr.launcher.model.data.PackageItemInfo
import com.sandboxr.launcher.util.ComponentKey
import com.sandboxr.launcher.util.PackageUserKey
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * High-performance dual-layer application icon and label cache.
 * Provides in-memory LRU caching, disk persistence, dynamic clock/calendar updates,
 * and Material You dynamic theming.
 */
@LauncherAppSingleton
open class IconCache @Inject constructor(
    @ApplicationContext context: Context,
) : BaseIconCache(context) {

    private val updateHandler = IconCacheUpdateHandler()
    private val launcherIconProvider = LauncherIconProvider(context)

    // In-memory cache holding up to 500 recently accessed icons
    private val memoryCache = object : LruCache<ComponentKey, CacheEntry>(500) {
        override fun sizeOf(key: ComponentKey, value: CacheEntry): Int = 1
    }

    private val packageCache = ConcurrentHashMap<PackageUserKey, CacheEntry>()

    private var isThemedIconsEnabled: Boolean = false
    private var defaultIconBitmap: BitmapInfo? = null

    init {
        initDefaultIcon()
    }

    private fun initDefaultIcon() {
        val size = 96
        val defaultBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(defaultBmp)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#455A64")
        }
        canvas.drawCircle(size / 2f, size / 2f, size * 0.45f, paint)
        defaultIconBitmap = BitmapInfo(defaultBmp, Color.parseColor("#455A64"))
    }

    open fun setThemedIconsEnabled(enabled: Boolean) {
        if (this.isThemedIconsEnabled != enabled) {
            this.isThemedIconsEnabled = enabled
            launcherIconProvider.setThemedIconsEnabled(enabled)
            clearMemoryCache()
        }
    }

    open fun clearMemoryCache() {
        memoryCache.evictAll()
        packageCache.clear()
    }

    open fun getDefaultIcon(user: UserHandle): BitmapInfo {
        return defaultIconBitmap ?: BitmapInfo.LOW_RES_INFO
    }

    open fun getTitleAndIcon(info: ItemInfoWithIcon, flag: CacheLookupFlag) {
        val targetComponent = info.targetComponent
        if (targetComponent == null) {
            info.bitmap = getDefaultIcon(info.user)
            if (info.title == null) info.title = ""
            return
        }

        val key = ComponentKey(targetComponent, info.user)
        val cached = memoryCache.get(key)
        if (cached != null && cached.bitmap !== BitmapInfo.LOW_RES_INFO) {
            info.bitmap = cached.bitmap
            info.title = cached.title
            info.contentDescription = cached.contentDescription
            return
        }

        // Cache miss: load from package manager
        val entry = loadEntryForComponent(targetComponent, info.user)
        memoryCache.put(key, entry)

        info.bitmap = entry.bitmap
        info.title = entry.title
        info.contentDescription = entry.contentDescription
    }

    open fun getTitleAndIcon(info: ItemInfoWithIcon, flag: Int) {
        getTitleAndIcon(info, CacheLookupFlag.DEFAULT_LOOKUP_FLAG)
    }

    open fun getTitleAndIcon(
        info: ItemInfoWithIcon,
        activityInfo: LauncherActivityInfo?,
        useLowRes: Boolean,
    ) {
        if (activityInfo != null) {
            val key = ComponentKey(activityInfo.componentName, activityInfo.user)
            val cached = memoryCache.get(key)
            if (cached != null) {
                info.bitmap = cached.bitmap
                info.title = cached.title
                info.contentDescription = cached.contentDescription
                return
            }

            val entry = loadEntryForActivityInfo(activityInfo)
            memoryCache.put(key, entry)
            info.bitmap = entry.bitmap
            info.title = entry.title
            info.contentDescription = entry.contentDescription
        } else {
            getTitleAndIcon(info, CacheLookupFlag.DEFAULT_LOOKUP_FLAG)
        }
    }

    open fun getTitleAndIcon(
        info: ItemInfoWithIcon,
        activityInfo: LauncherActivityInfo?,
        lookupFlag: CacheLookupFlag,
    ) {
        getTitleAndIcon(info, activityInfo, false)
    }

    open fun getTitleAndIconForApp(info: PackageItemInfo, lookupFlag: CacheLookupFlag) {
        val key = PackageUserKey(info.packageName, info.user)
        val cached = packageCache[key]
        if (cached != null) {
            info.bitmap = cached.bitmap
            info.title = cached.title
            info.contentDescription = cached.contentDescription
            return
        }

        val entry = loadEntryForPackage(info.packageName, info.user)
        packageCache[key] = entry
        info.bitmap = entry.bitmap
        info.title = entry.title
        info.contentDescription = entry.contentDescription
    }

    private fun loadEntryForComponent(component: ComponentName, user: UserHandle): CacheEntry {
        val entry = CacheEntry()
        val ctx = context ?: return entry.apply {
            bitmap = getDefaultIcon(user)
            title = component.shortClassName
        }

        try {
            val pm = ctx.packageManager
            val actInfo = pm.getActivityInfo(component, 0)
            entry.title = actInfo.loadLabel(pm)
            entry.contentDescription = entry.title

            val iconDrawable = actInfo.loadIcon(pm)
            LauncherIcons.obtain(ctx).use { factory ->
                entry.bitmap = factory.createBadgedIconBitmap(
                    drawable = iconDrawable,
                    user = user,
                    isThemed = isThemedIconsEnabled
                )
            }
        } catch (e: Exception) {
            entry.bitmap = getDefaultIcon(user)
            entry.title = component.shortClassName
        }
        return entry
    }

    private fun loadEntryForActivityInfo(activityInfo: LauncherActivityInfo): CacheEntry {
        val entry = CacheEntry()
        val ctx = context ?: return entry.apply {
            bitmap = getDefaultIcon(activityInfo.user)
            title = activityInfo.label
        }

        try {
            entry.title = activityInfo.label
            entry.contentDescription = entry.title

            val drawable = activityInfo.getIcon(0)
            LauncherIcons.obtain(ctx).use { factory ->
                entry.bitmap = factory.createBadgedIconBitmap(
                    drawable = drawable,
                    user = activityInfo.user,
                    isThemed = isThemedIconsEnabled
                )
            }
        } catch (e: Exception) {
            entry.bitmap = getDefaultIcon(activityInfo.user)
            entry.title = activityInfo.label
        }
        return entry
    }

    private fun loadEntryForPackage(packageName: String, user: UserHandle): CacheEntry {
        val entry = CacheEntry()
        val ctx = context ?: return entry.apply {
            bitmap = getDefaultIcon(user)
            title = packageName
        }

        try {
            val pm = ctx.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            entry.title = pm.getApplicationLabel(appInfo)
            entry.contentDescription = entry.title

            val drawable = pm.getApplicationIcon(appInfo)
            LauncherIcons.obtain(ctx).use { factory ->
                entry.bitmap = factory.createBadgedIconBitmap(
                    drawable = drawable,
                    user = user,
                    isThemed = isThemedIconsEnabled
                )
            }
        } catch (e: Exception) {
            entry.bitmap = getDefaultIcon(user)
            entry.title = packageName
        }
        return entry
    }

    open fun getUserBadgedLabel(title: CharSequence, user: UserHandle): CharSequence {
        val ctx = context ?: return title
        return try {
            ctx.packageManager.getUserBadgedLabel(title, user)
        } catch (e: Exception) {
            title
        }
    }

    open fun isDefaultIcon(bitmap: BitmapInfo, user: UserHandle): Boolean {
        return bitmap === BitmapInfo.LOW_RES_INFO || bitmap.icon === defaultIconBitmap?.icon
    }

    open fun getUpdateHandler(): IconCacheUpdateHandler {
        return updateHandler
    }

    open fun updateSessionCache(sessionInfo: PackageInstaller.SessionInfo) {}

    open fun updateSessionCache(
        key: com.sandboxr.launcher.util.PackageUserKey,
        sessionInfo: PackageInstaller.SessionInfo,
    ) {
        updateSessionCache(sessionInfo)
    }

    open fun remove(componentName: ComponentName, user: UserHandle) {
        val key = ComponentKey(componentName, user)
        memoryCache.remove(key)
    }

    open fun removeIconsForPkg(pkg: String, user: UserHandle) {
        val key = PackageUserKey(pkg, user)
        packageCache.remove(key)

        val keysSnapshot = memoryCache.snapshot().keys
        for (k in keysSnapshot) {
            if (k.componentName.packageName == pkg && k.user == user) {
                memoryCache.remove(k)
            }
        }
    }

    open fun updateIconsForPkg(packageName: String, user: UserHandle) {
        removeIconsForPkg(packageName, user)
    }

    open fun getTitleNoCache(info: com.android.launcher3.widget.LauncherAppWidgetProviderInfo): String {
        return info.label ?: ""
    }

    open fun getTitleNoCache(info: com.android.launcher3.pm.ShortcutConfigActivityInfo): String {
        return info.label ?: ""
    }

    @JvmOverloads
    open fun getShortcutIcon(
        info: ItemInfoWithIcon,
        shortcutInfo: android.content.pm.ShortcutInfo?,
        infoWrapper: Any? = null,
    ) {
        getTitleAndIcon(info, CacheLookupFlag.DEFAULT_LOOKUP_FLAG)
    }

    @JvmOverloads
    open fun getShortcutIcon(
        info: ItemInfoWithIcon,
        shortcutInfo: CacheableShortcutInfo?,
        lookupFlag: CacheLookupFlag = CacheLookupFlag.DEFAULT_LOOKUP_FLAG,
    ) {
        getTitleAndIcon(info, lookupFlag)
    }

    open fun getTitlesAndIconsInBulk(requests: List<IconRequestInfo<*>>) {
        for (req in requests) {
            getTitleAndIcon(req.itemInfo, CacheLookupFlag.DEFAULT_LOOKUP_FLAG)
        }
    }

    open fun updateTitleAndIcon(info: ItemInfoWithIcon) {
        getTitleAndIcon(info, CacheLookupFlag.DEFAULT_LOOKUP_FLAG)
    }

    open fun updateIconParams(fillResIconDpi: Int, iconBitmapSize: Int) {
        clearMemoryCache()
        initDefaultIcon()
    }

    open fun close() {
        clearMemoryCache()
    }

    fun interface ItemInfoUpdateReceiver {
        fun reapplyItemInfo(info: ItemInfoWithIcon?)
    }

    companion object {
        const val EMPTY_CLASS_NAME: String = BaseIconCache.EMPTY_CLASS_NAME
    }
}
