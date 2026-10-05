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

package com.sandboxr.launcher.splitscreen

import android.content.Intent
import com.android.systemui.shared.recents.model.Task

/**
 * Data container storing selected tasks, intents, and positions during split initiation.
 */
class SplitSelectDataHolder {
    var initialTask: Task? = null
    var initialTaskId: Int? = null
    var initialIntent: Intent? = null
    var initialStagePosition: Int = SplitConfigurationOptions.STAGE_POSITION_TOP_OR_LEFT

    var secondTask: Task? = null
    var secondTaskId: Int? = null
    var secondIntent: Intent? = null
    var secondStagePosition: Int = SplitConfigurationOptions.STAGE_POSITION_BOTTOM_OR_RIGHT

    var splitRatio: Float = SplitConfigurationOptions.DEFAULT_SPLIT_RATIO

    val isInitialTaskSet: Boolean
        get() = initialTask != null || initialTaskId != null || initialIntent != null

    val isSecondTaskSet: Boolean
        get() = secondTask != null || secondTaskId != null || secondIntent != null

    val isBothTasksSet: Boolean
        get() = isInitialTaskSet && isSecondTaskSet

    val isBothSplitAppsConfirmed: Boolean
        get() = isBothTasksSet

    fun reset() {
        initialTask = null
        initialTaskId = null
        initialIntent = null
        initialStagePosition = SplitConfigurationOptions.STAGE_POSITION_TOP_OR_LEFT
        secondTask = null
        secondTaskId = null
        secondIntent = null
        secondStagePosition = SplitConfigurationOptions.STAGE_POSITION_BOTTOM_OR_RIGHT
        splitRatio = SplitConfigurationOptions.DEFAULT_SPLIT_RATIO
    }
}
