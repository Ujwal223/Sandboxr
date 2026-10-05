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

package com.sandboxr.launcher.desktop

import android.content.Context
import android.view.View
import com.sandboxr.launcher.R

/**
 * Menu action / shortcut that allows the user to bring the selected task into Desktop windowing mode.
 */
class DesktopShortcut(
    val context: Context,
    val taskId: Int,
    val desktopController: DesktopVisibilityController,
    val onLaunchedToDesktop: ((taskId: Int) -> Unit)? = null
) {

    val iconResId: Int = R.drawable.ic_desktop
    val labelResId: Int = R.string.recent_task_option_desktop

    fun onClick(view: View? = null) {
        // Move task to active desktop desk
        val displayId = try {
            context.display?.displayId ?: 0
        } catch (_: Throwable) {
            0
        }
        val activeDesk = desktopController.getActiveDeskId(displayId)
        val deskToUse = if (activeDesk != DesktopVisibilityController.INACTIVE_DESK_ID) activeDesk else 1
        desktopController.setActiveDesk(displayId, deskToUse)
        onLaunchedToDesktop?.invoke(taskId)
    }
}
