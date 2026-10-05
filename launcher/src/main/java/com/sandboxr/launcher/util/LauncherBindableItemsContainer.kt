/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.sandboxr.launcher.util

import android.view.View
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.views.ActivityContext
import java.util.function.Predicate

/** Interface representing a container which can bind Launcher items with some utility methods */
fun interface LauncherBindableItemsContainer {

    fun updateContainerItems(updates: Set<ItemInfo>, context: ActivityContext): Set<ItemInfo> {
        val op = ItemOperator { info, v ->
            if (v is BubbleTextView && info is WorkspaceItemInfo && updates.contains(info)) {
                v.applyFromWorkspaceItem(info)
            }
            false
        }

        mapOverItems(op)
        return emptySet()
    }

    fun getFirstMatch(vararg matchers: Predicate<ItemInfo>): View? =
        matchers.firstNotNullOfOrNull { mapOverItems { info, _ -> info != null && it.test(info) } }

    fun getViewByItemId(id: Int): View? = mapOverItems { info, _ -> info != null && info.id == id }

    fun isContainerSupported(container: Int) = false

    fun mapOverItems(op: ItemOperator): View?

    fun mapOverVisibleItems(op: ItemOperator): View? = mapOverItems(op)

    fun interface ItemOperator {
        fun evaluate(info: ItemInfo?, view: View): Boolean
    }
}
