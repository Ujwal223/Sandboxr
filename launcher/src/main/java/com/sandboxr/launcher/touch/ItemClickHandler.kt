/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.sandboxr.launcher.touch

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.R
import com.sandboxr.launcher.apppairs.AppPairIcon
import com.sandboxr.launcher.apppairs.AppPairsController
import com.sandboxr.launcher.folder.Folder
import com.sandboxr.launcher.folder.FolderIcon
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.AppPairInfo
import com.sandboxr.launcher.model.data.FolderInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.LauncherAppWidgetInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo

/**
 * Class for handling clicks on workspace and all-apps items.
 */
object ItemClickHandler {

    private const val TAG = "ItemClickHandler"

    interface ItemClickProxy {
        fun onItemClicked(view: View)
    }

    @JvmField
    val INSTANCE = View.OnClickListener { v -> onClick(v) }

    @JvmStatic
    fun onClick(v: View) {
        if (v.windowToken == null) return

        val launcher = Launcher.getLauncher(v.context)
        if (launcher.getDragController()?.isDragging == true) return

        when (val tag = v.tag) {
            is WorkspaceItemInfo -> {
                onClickAppShortcut(v, tag, launcher)
            }
            is FolderInfo -> {
                onClickFolderIcon(v)
            }
            is AppPairInfo -> {
                onClickAppPairIcon(v, tag)
            }
            is AppInfo -> {
                startAppShortcutOrInfoActivity(v, tag, launcher)
            }
            is LauncherAppWidgetInfo -> {
                Log.d(TAG, "onClick: widget clicked pkg=${tag.targetPackage}")
            }
            is ItemClickProxy -> {
                tag.onItemClicked(v)
            }
        }
    }

    private fun onClickFolderIcon(v: View) {
        val folder = (v as? FolderIcon)?.folder ?: return
        if (!folder.isOpen && !folder.isDestroyed) {
            folder.animateOpen()
        }
    }

    private fun onClickAppPairIcon(v: View, appPairInfo: AppPairInfo) {
        AppPairsController(v.context).launchAppPair(appPairInfo)
    }

    private fun onClickAppShortcut(v: View, shortcut: WorkspaceItemInfo, launcher: Launcher) {
        val intent = shortcut.intent ?: return
        try {
            launcher.startActivitySafely(v, intent, shortcut)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to launch shortcut", e)
            Toast.makeText(launcher, R.string.activity_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAppShortcutOrInfoActivity(v: View, appInfo: AppInfo, launcher: Launcher) {
        val intent = appInfo.intent ?: return
        try {
            launcher.startActivitySafely(v, intent, appInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to launch app", e)
            Toast.makeText(launcher, R.string.activity_not_found, Toast.LENGTH_SHORT).show()
        }
    }
}
