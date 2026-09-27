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

package com.sandboxr.launcher.states

import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.views.ActivityContext

/**
 * State representing recent apps overview / task switcher.
 */
open class OverviewState @JvmOverloads constructor(
    id: Int,
    additionalFlags: Int = 0
) : LauncherState(
    id,
    3 /* LAUNCHER_STATE_OVERVIEW */,
    FLAG_RECENTS_VIEW_VISIBLE or FLAG_WORKSPACE_INACCESSIBLE or additionalFlags
) {

    override fun isInOverview(): Boolean = true

    override fun getTransitionDuration(context: ActivityContext, isToState: Boolean): Int = 250

    override fun getVisibleElements(context: ActivityContext): Int =
        OVERVIEW_ACTIONS or CLEAR_ALL_BUTTON

    companion object {
        const val FLAG_IS_TASK_VIEW_INTERACTIVE = 1 shl 10

        @JvmStatic
        fun newOverviewState(id: Int): OverviewState = OverviewState(id, FLAG_IS_TASK_VIEW_INTERACTIVE)

        @JvmStatic
        fun newBackgroundState(id: Int): OverviewState = OverviewState(id)

        @JvmStatic
        fun newSwitchState(id: Int): OverviewState = OverviewState(id)

        @JvmStatic
        fun newModalTaskState(id: Int): OverviewState = OverviewState(id)

        @JvmStatic
        fun newSplitSelectState(id: Int): OverviewState = OverviewState(id)
    }
}
