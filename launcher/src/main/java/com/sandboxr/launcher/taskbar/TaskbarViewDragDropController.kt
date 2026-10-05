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

import android.view.View
import com.sandboxr.launcher.model.data.ItemInfo

/**
 * Controller handling customization drag-and-drop reordering for Taskbar icons.
 */
class TaskbarViewDragDropController(
    val activity: TaskbarActivityContext,
) {
    private lateinit var controllers: TaskbarControllers
    var isDragging: Boolean = false
        private set
    var draggedItem: ItemInfo? = null
        private set

    fun init(controllers: TaskbarControllers) {
        this.controllers = controllers
    }

    fun startDrag(view: View, item: ItemInfo) {
        isDragging = true
        draggedItem = item
        view.visibility = View.INVISIBLE
    }

    fun onDragCompleted(success: Boolean, targetIndex: Int = -1) {
        isDragging = false
        draggedItem = null
    }

    fun reorder(fromIndex: Int, toIndex: Int, items: MutableList<ItemInfo>) {
        if (fromIndex in items.indices && toIndex in items.indices) {
            val item = items.removeAt(fromIndex)
            items.add(toIndex, item)
            controllers.taskbarViewController.onPinnedItemsChanged(items)
        }
    }
}
