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

package com.sandboxr.launcher.touch

import android.view.View
import android.view.View.OnLongClickListener
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.model.data.ItemInfo

/**
 * Handles long-clicks on workspace items to trigger drag operations or context menus.
 */
object ItemLongClickListener {

    @JvmField
    val INSTANCE_WORKSPACE: OnLongClickListener = OnLongClickListener { v ->
        onWorkspaceItemLongClick(v)
    }

    @JvmField
    val INSTANCE_ALL_APPS: OnLongClickListener = OnLongClickListener { v ->
        onAllAppsItemLongClick(v)
    }

    private fun onWorkspaceItemLongClick(v: View): Boolean {
        val launcher = Launcher.getLauncher(v.context) ?: return false
        val state = launcher.getStateManager().state
        if (state != LauncherState.NORMAL && state != LauncherState.OVERVIEW && state != LauncherState.EDIT_MODE) {
            return false
        }
        val tag = v.tag as? ItemInfo ?: return false
        launcher.getWorkspace()?.startDrag(v, tag)
        return true
    }

    private fun onAllAppsItemLongClick(v: View): Boolean {
        val launcher = Launcher.getLauncher(v.context) ?: return false
        val tag = v.tag as? ItemInfo ?: return false
        launcher.getWorkspace()?.beginDragShared(v, launcher.getAppsView(), tag)
        return true
    }
}
