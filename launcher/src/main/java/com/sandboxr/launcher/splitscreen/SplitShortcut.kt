/*
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

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.View
import com.sandboxr.launcher.R
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.STAGE_POSITION_TOP_OR_LEFT
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.SplitPositionOption
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.SplitSelectSource

/**
 * Menu item / shortcut that allows the user to initiate split screen selection for an app or task.
 */
class SplitShortcut(
    val context: Context,
    val splitController: SplitSelectStateController,
    val taskId: Int? = null,
    val intent: Intent? = null,
    val position: SplitPositionOption = SplitPositionOption(
        STAGE_POSITION_TOP_OR_LEFT,
        R.drawable.ic_desktop,
        R.string.split_screen_position_top
    )
) {

    val iconResId: Int = R.drawable.ic_desktop // or split icon
    val labelResId: Int = R.string.recent_task_option_split_screen

    /**
     * Executes the split action when clicked.
     */
    fun onClick(view: View? = null) {
        if (taskId != null) {
            splitController.initiateSplitSelect(taskId, position.stagePosition)
        } else if (intent != null) {
            splitController.initiateSplitSelect(intent, position.stagePosition)
        }
    }
}
