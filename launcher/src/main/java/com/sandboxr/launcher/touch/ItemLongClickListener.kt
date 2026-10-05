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

import android.view.View
import com.sandboxr.launcher.Launcher

object ItemLongClickListener {

    @JvmField
    val INSTANCE_WORKSPACE = View.OnLongClickListener { v ->
        onWorkspaceItemLongClick(v)
    }

    @JvmField
    val INSTANCE_ALL_APPS = View.OnLongClickListener { v ->
        onAllAppsItemLongClick(v)
    }

    @JvmStatic
    fun canStartDrag(launcher: Launcher?): Boolean {
        if (launcher == null) return false
        if (launcher.getDragController()?.isDragging == true) return false
        return true
    }

    @JvmStatic
    fun onWorkspaceItemLongClick(v: View): Boolean {
        val launcher = Launcher.getLauncher(v.context) ?: return false
        if (!canStartDrag(launcher)) return false
        return true
    }

    @JvmStatic
    fun onAllAppsItemLongClick(v: View): Boolean {
        val launcher = Launcher.getLauncher(v.context) ?: return false
        if (!canStartDrag(launcher)) return false
        return true
    }
}
