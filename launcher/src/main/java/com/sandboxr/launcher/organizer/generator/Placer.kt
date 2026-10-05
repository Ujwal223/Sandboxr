/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.sandboxr.launcher.organizer.generator

import com.sandboxr.launcher.LauncherSettings
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo

/**
 * Arranges classified items into containers based on layout templates.
 */
interface Placer {
    fun place(
        classifiedItems: List<TopicClassifiedItem>,
        templates: List<Template>
    ): List<List<ItemInfo>>
}

/**
 * Arranges items sequentially into a folder by assigning their ranks.
 */
class FolderPlacer {
    fun place(itemsToPlace: List<TopicClassifiedItem>): List<ItemInfo> {
        return itemsToPlace.mapIndexed { index, topicItem ->
            val info = topicItem.itemInfo
            val item = if (info is AppInfo) {
                WorkspaceItemInfo(info)
            } else {
                info.makeShallowCopy()
            }
            item.rank = index
            item
        }
    }
}

/**
 * Places classified items into screen pages following grid templates.
 */
class HeuristicScreenPlacer : Placer {

    override fun place(
        classifiedItems: List<TopicClassifiedItem>,
        templates: List<Template>
    ): List<List<ItemInfo>> {
        if (classifiedItems.isEmpty() || templates.isEmpty()) return emptyList()

        val screens = mutableListOf<List<ItemInfo>>()
        val sortedItems = classifiedItems.sortedByDescending { it.score }

        for (template in templates) {
            val screenItems = mutableListOf<ItemInfo>()
            val available = ArrayList(sortedItems)

            for (slot in template.items) {
                if (available.isEmpty()) break
                val matched = available.removeAt(0)
                val base = matched.itemInfo
                val item = if (base is AppInfo) WorkspaceItemInfo(base) else base.makeShallowCopy()
                item.cellX = slot.cellAndSpan.cellX
                item.cellY = slot.cellAndSpan.cellY
                item.spanX = slot.cellAndSpan.spanX
                item.spanY = slot.cellAndSpan.spanY
                item.container = LauncherSettings.Favorites.CONTAINER_DESKTOP
                screenItems.add(item)
            }

            if (screenItems.isNotEmpty()) {
                screens.add(screenItems)
            }
        }
        return screens
    }
}
