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

package com.sandboxr.launcher.recents

import android.os.Bundle
import com.sandboxr.launcher.R
import com.sandboxr.launcher.recents.data.RecentTasksRepository
import com.sandboxr.launcher.recents.data.RecentTasksRepositoryImpl
import com.sandboxr.launcher.recents.fallback.RecentsState
import com.sandboxr.launcher.recents.views.RecentsView
import com.sandboxr.launcher.statemanager.StateManager
import com.sandboxr.launcher.statemanager.StatefulActivity

/**
 * Activity for displaying recent tasks overview in standalone or fallback mode.
 */
class RecentsActivity : StatefulActivity<RecentsState>() {

    private val mStateManager = StateManager(this, RecentsState.DEFAULT)
    private var mRecentsView: RecentsView? = null
    var tasksRepository: RecentTasksRepository = RecentTasksRepositoryImpl()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inflateRootView(R.layout.fallback_activity_recents_view)
        mRecentsView = findViewById(R.id.overview_panel)
        mRecentsView?.recentTasksRepository = tasksRepository
    }

    override fun getStateManager(): StateManager<RecentsState, RecentsActivity> = mStateManager

    override fun collectStateHandlers(out: MutableList<StateManager.StateHandler<RecentsState>>) {
        // Collect state handlers for overview state transitions
    }

    override fun onHandleConfigurationChanged() {
        // Handle config updates (orientation, multi-window)
    }

    override fun shouldAnimateStateChange(): Boolean = true

    fun getOverviewPanel(): RecentsView? = mRecentsView
}
