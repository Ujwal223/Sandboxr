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

package com.sandboxr.launcher.model.data

import android.content.Context
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherSettings
import com.sandboxr.launcher.icons.IconCache
import com.sandboxr.launcher.icons.cache.CacheLookupFlag
import com.android.launcher3.logger.LauncherAtom

/** A type of app collection that launches multiple apps into split screen. */
class AppPairInfo() : CollectionInfo() {
    companion object {
        const val MIN_ITEMS = 2
        const val MAX_ITEMS = 3

        @JvmStatic
        fun hasValidItemCount(count: Int): Boolean {
            return count in MIN_ITEMS..MAX_ITEMS
        }
    }

    private var contents = mutableListOf<WorkspaceItemInfo>()

    init {
        itemType = LauncherSettings.Favorites.ITEM_TYPE_APP_GROUP
    }

    /** Convenience constructor, calls primary constructor and init block */
    constructor(apps: List<WorkspaceItemInfo>) : this() {
        apps.forEach(this::add)
    }

    /** Creates a new AppPairInfo that is a copy of the provided one. */
    constructor(appPairInfo: AppPairInfo) : this() {
        contents = appPairInfo.contents.toMutableList()
        copyFrom(appPairInfo)
    }

    override fun add(item: ItemInfo) {
        if (item is WorkspaceItemInfo) {
            contents.add(item)
        }
    }

    fun add(item: WorkspaceItemInfo) {
        contents.add(item)
    }

    fun remove(item: WorkspaceItemInfo) {
        contents.remove(item)
    }

    override fun getContents(): List<ItemInfo> {
        return ArrayList(contents)
    }

    override fun getAppContents(): List<WorkspaceItemInfo> {
        return contents.toList()
    }

    fun getFirstApp() = contents[0]
    fun getSecondApp() = contents[1]

    /** Returns if either of the app pair members is currently disabled. */
    override fun isDisabled(): Boolean = anyMatch { it.isDisabled() }

    /** Checks if member apps are launchable at the current screen size. */
    fun isLaunchable(context: Context): Pair<Boolean, Boolean> {
        val dp = com.sandboxr.launcher.InvariantDeviceProfile.INSTANCE(context).getDeviceProfile(context)
        val isTablet = dp.isTablet
        return Pair(
            isTablet || !getFirstApp().isNonResizeable,
            isTablet || !getSecondApp().isNonResizeable,
        )
    }

    /** Fetches high-res icons for member apps if needed. */
    fun fetchHiResIconsIfNeeded(iconCache: IconCache) {
        getAppContents()
            .forEach { iconCache.getTitleAndIcon(it, CacheLookupFlag.DEFAULT_LOOKUP_FLAG) }
    }

    /**
     * App pairs will report itself as "disabled" (for accessibility) if either of the following is
     * true:
     * 1) One of the member WorkspaceItemInfos is disabled.
     * 2) One of the member apps can't be launched due to screen size requirements.
     */
    fun shouldReportDisabled(context: Context): Boolean {
        return isDisabled || !isLaunchable(context).first || !isLaunchable(context).second
    }

    /** Generates a default title for the app pair and sets it. */
    fun generateTitle(context: Context): CharSequence? {
        val app1: CharSequence? = getFirstApp().title
        val app2: CharSequence? = getSecondApp().title
        title = "$app1 & $app2"
        return title
    }

    /** Generates an ItemInfo for logging. */
    override fun buildProto(cInfo: CollectionInfo?, context: Context): LauncherAtom.ItemInfo {
        val appPairIcon = LauncherAtom.FolderIcon.newBuilder().setCardinality(contents.size)
        appPairIcon.setLabelInfo(title.toString())
        return getDefaultItemInfoBuilder(context)
            .setFolderIcon(appPairIcon)
            .setRank(rank)
            .setContainerInfo(getContainerInfo())
            .build()
    }
}
