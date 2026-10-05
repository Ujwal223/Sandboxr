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

package com.sandboxr.launcher.taskbar

import com.sandboxr.launcher.model.data.ItemInfo

/**
 * Controller managing app pinning to the Taskbar and pin state persistence.
 */
class TaskbarPinningController(
    val activity: TaskbarActivityContext,
) {
    private lateinit var controllers: TaskbarControllers
    private val pinnedItems = mutableListOf<ItemInfo>()

    var isPinned: Boolean
        get() = activity.sharedState.isPinned
        set(value) {
            activity.sharedState.isPinned = value
            controllers.taskbarStashController.onPinningStateChanged(value)
        }

    fun init(controllers: TaskbarControllers) {
        this.controllers = controllers
    }

    fun pinItem(item: ItemInfo): Boolean {
        if (pinnedItems.any { it.id == item.id }) return false
        pinnedItems.add(item)
        controllers.taskbarViewController.onPinnedItemsChanged(pinnedItems)
        return true
    }

    fun unpinItem(item: ItemInfo): Boolean {
        val removed = pinnedItems.removeAll { it.id == item.id }
        if (removed) {
            controllers.taskbarViewController.onPinnedItemsChanged(pinnedItems)
        }
        return removed
    }

    fun getPinnedItems(): List<ItemInfo> = pinnedItems.toList()
}
