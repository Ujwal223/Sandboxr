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
 * State representing the vertical or paged All Apps drawer.
 */
open class AllAppsState(id: Int) : LauncherState(
    id,
    2 /* LAUNCHER_STATE_ALLAPPS */,
    STATE_FLAGS
) {

    override fun getTransitionDuration(context: ActivityContext, isToState: Boolean): Int = 300

    override fun getVisibleElements(context: ActivityContext): Int = ALL_APPS_CONTENT

    override fun getHistoryForState(previousState: LauncherState): LauncherState = NORMAL

    companion object {
        private val STATE_FLAGS = FLAG_CLOSE_POPUPS or FLAG_WORKSPACE_INACCESSIBLE
    }
}
