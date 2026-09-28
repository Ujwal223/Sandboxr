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
package com.android.launcher3.ui

import com.sandboxr.launcher.LauncherModel
import com.sandboxr.launcher.model.BgDataModel
import com.sandboxr.launcher.model.BgDataModel.Callbacks
import com.sandboxr.launcher.model.IModelWriter
import com.sandboxr.launcher.model.data.ItemInfo
import com.android.launcher3.util.ItemInfoMatcher
import java.util.Collections
import java.util.concurrent.Executor

class DefaultLauncherUiStateNotifier(
    private val uiExecutor: Executor,
    private val bgDataModel: BgDataModel,
    private val verifyChanges: Boolean = true,
    private val model: LauncherModel,
) : LauncherUiStateNotifier {

    private val callbacks = mutableListOf<Callbacks>()

    override fun addCallback(callback: Callbacks) {
        callbacks.add(callback)
    }

    override fun removeCallback(callback: Callbacks) {
        callbacks.remove(callback)
    }

    override fun notifyItemModifiedOptimistically(item: ItemInfo) {
        uiExecutor.execute {
            for (cb in callbacks) {
                cb.bindItemsUpdated(Collections.singleton(item))
            }
        }
    }

    override fun notifyModelChanged(changeLog: IModelWriter.ChangeLog, owner: Callbacks?) {
        val startId = bgDataModel.lastBindId
        uiExecutor.execute {
            val allCallbacks = callbacks + model.callbacks
            if (changeLog.itemsAdded.isNotEmpty()) {
                allCallbacks
                    .filter { it != owner }
                    .forEach { it.bindItemsAdded(changeLog.itemsAdded) }
            }
            if (changeLog.itemsModified.isNotEmpty()) {
                allCallbacks
                    .filter { it != owner }
                    .forEach { it.bindItemsUpdated(changeLog.itemsModified) }
            }
            if (changeLog.itemsRemoved.isNotEmpty()) {
                val matcher = ItemInfoMatcher.ofItems(changeLog.itemsRemoved)
                allCallbacks
                    .filter { it != owner }
                    .forEach { it.bindWorkspaceComponentsRemoved(matcher) }
            }
            verifyUiConsistency(startId)
        }
    }

    private fun verifyUiConsistency(startId: Int) {
        if (!verifyChanges || callbacks.isEmpty()) {
            return
        }

        val executeId = bgDataModel.lastBindId
        if (executeId > startId) {
            return
        }
        if (executeId == startId) {
            return
        }
        model.rebindCallbacks("model-ui-consistency-failed")
    }
}
