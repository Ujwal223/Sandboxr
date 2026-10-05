/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.sandboxr.launcher.uioverrides

import android.os.Bundle
import android.util.Log
import android.view.View
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.recents.views.RecentsView
import com.sandboxr.launcher.sysuiconnection.SysUIConnectionTracker

/**
 * Extended Launcher Activity that enables Quickstep gesture navigation,
 * SystemUI recents proxy delegation, and virtual container task thumbnail routing.
 */
open class QuickstepLauncher : Launcher() {

    companion object {
        private const val TAG = "QuickstepLauncher"
    }

    private var mRecentsView: RecentsView<QuickstepLauncher>? = null
    private val sysUiTracker = SysUIConnectionTracker.get()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "QuickstepLauncher initialized with zero-root virtual environment support.")
    }

    override fun setupViews() {
        super.setupViews()
        val recents = mOverviewPanel as? RecentsView<QuickstepLauncher>
        mRecentsView = recents
    }

    open fun getRecentsView(): RecentsView<QuickstepLauncher>? = mRecentsView

    override fun onStateSetStart(state: LauncherState) {
        super.onStateSetStart(state)
        if (state == LauncherState.OVERVIEW) {
            mRecentsView?.updateEmptyMessage()
        }
    }

    override fun onDestroy() {
        sysUiTracker.onDisconnected()
        super.onDestroy()
    }
}
