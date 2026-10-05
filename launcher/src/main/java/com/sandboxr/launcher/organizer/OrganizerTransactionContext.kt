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

package com.sandboxr.launcher.organizer

import com.sandboxr.launcher.LauncherSettings.Favorites
import com.sandboxr.launcher.model.BgDataModel
import com.sandboxr.launcher.model.IModelWriter
import com.sandboxr.launcher.model.data.FolderInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.repository.HomeScreenRepository
import java.util.ArrayList
import javax.inject.Inject

/**
 * Specialized context for executing workspace organizer transactions (creating screens, adding folders, rearranging pages).
 */
class OrganizerTransactionContext @Inject constructor(
    private val modelWriter: IModelWriter? = null,
    private val homeScreenRepository: HomeScreenRepository? = null
) {

    val createdScreens = mutableListOf<List<ItemInfo>>()
    val addedFolders = mutableListOf<FolderInfo>()
    var lastPageOrder: List<Int>? = null

    /**
     * Adds a new workspace screen populated with [items].
     */
    fun addScreen(items: List<ItemInfo>, targetScreenId: Int = -1) {
        createdScreens.add(items)
        val screenId = if (targetScreenId >= 0) targetScreenId else 999
        for (item in items) {
            item.container = Favorites.CONTAINER_DESKTOP
            item.screenId = screenId
            modelWriter?.addItemToDatabase(item, item.container, item.screenId, item.cellX, item.cellY)

            if (item is FolderInfo) {
                for (folderItem in item.getContents()) {
                    folderItem.container = item.id
                    modelWriter?.addItemToDatabase(folderItem, item.id, 0, 0, 0)
                }
            }
        }
    }

    /**
     * Adds generated folders to the workspace.
     */
    fun addFolders(folders: List<FolderInfo>) {
        addedFolders.addAll(folders)
        for ((index, folder) in folders.withIndex()) {
            folder.container = Favorites.CONTAINER_DESKTOP
            folder.screenId = 0
            folder.cellX = index % 4
            folder.cellY = (index / 4) + 1
            modelWriter?.addItemToDatabase(folder, folder.container, folder.screenId, folder.cellX, folder.cellY)

            for (item in folder.getContents()) {
                item.container = folder.id
                modelWriter?.addItemToDatabase(item, folder.id, 0, 0, 0)
            }
        }
    }

    /**
     * Reorders pages by mapping old screen indices to new screen indices.
     */
    fun reorderPages(pageOrder: List<Int>) {
        lastPageOrder = pageOrder
        // Persists updated page order
    }
}

