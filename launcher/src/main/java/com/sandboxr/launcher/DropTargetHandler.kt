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

package com.sandboxr.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.View
import com.sandboxr.launcher.DropTarget.DragObject
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo

/**
 * Coordinates actions when items are dropped on drop targets (Delete, Uninstall, App Info, etc.).
 */
class DropTargetHandler(
    private val launcher: Launcher
) {

    fun onDropAnimationComplete() {
        launcher.getStateManager().goToState(LauncherState.NORMAL)
    }

    fun onDeleteComplete(item: ItemInfo, view: View?) {
        launcher.getWorkspace()?.removeWorkspaceItem(view, item)
        if (item.id != ItemInfo.NO_ID) {
            try {
                launcher.getModelWriter()?.deleteItemFromDatabase(item, "removed by drop target")
            } catch (e: Exception) {
                Log.w(TAG, "Error removing item from db: $item", e)
            }
        }
        launcher.getWorkspace()?.stripEmptyScreens()
    }

    fun onSecondaryTargetCompleteDrop(target: ComponentName?, d: DragObject) {
        val info = d.dragInfo ?: return
        val context = launcher
        val packageName = target?.packageName 
            ?: (info as? AppInfo)?.componentName?.packageName
            ?: (info as? WorkspaceItemInfo)?.targetPackage
        if (packageName != null) {
            try {
                val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch uninstall intent for $packageName", e)
            }
        }
    }

    fun onAppInfoCompleteDrop(target: ComponentName?, d: DragObject) {
        val info = d.dragInfo ?: return
        val context = launcher
        val packageName = target?.packageName 
            ?: (info as? AppInfo)?.componentName?.packageName
            ?: (info as? WorkspaceItemInfo)?.targetPackage
        if (packageName != null) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch app info intent for $packageName", e)
            }
        }
    }

    fun prepareToUndoDelete(item: ItemInfo) {
        // Can be attached to UndoDeleteController
    }

    companion object {
        private const val TAG = "DropTargetHandler"
    }
}
