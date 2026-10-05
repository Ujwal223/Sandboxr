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

package com.sandboxr.launcher.recents.ui.mapper

import com.android.systemui.shared.recents.model.Task
import com.sandboxr.launcher.recents.domain.model.TaskModel
import com.sandboxr.launcher.recents.ui.viewmodel.TaskData
import com.sandboxr.launcher.recents.ui.viewmodel.TaskTileUiState
import javax.inject.Inject

class TaskUiStateMapper @Inject constructor() {

    fun map(taskModel: TaskModel, isCentralTask: Boolean = false): TaskTileUiState {
        return TaskTileUiState(
            tasks = listOf(
                TaskData.Data(
                    taskId = taskModel.id,
                    packageName = taskModel.packageName,
                    title = taskModel.title,
                    titleDescription = taskModel.titleDescription,
                    icon = taskModel.icon,
                    thumbnailData = taskModel.thumbnail,
                    backgroundColor = taskModel.backgroundColor,
                    isLocked = taskModel.isLocked,
                    isLiveTile = false,
                    remainingAppTimerDuration = taskModel.remainingAppDuration,
                    isAppLocked = taskModel.isAppLocked,
                )
            ),
            isCentralTask = isCentralTask,
        )
    }

    fun map(task: Task, isCentralTask: Boolean = false): TaskTileUiState {
        return TaskTileUiState(
            tasks = listOf(
                TaskData.Data(
                    taskId = task.key.id,
                    packageName = task.key.getPackageName() ?: "",
                    title = task.title,
                    titleDescription = task.titleDescription,
                    icon = task.icon,
                    thumbnailData = task.thumbnail,
                    backgroundColor = task.colorBackground,
                    isLocked = task.isLocked,
                    isLiveTile = task.isRunning,
                    remainingAppTimerDuration = null,
                    isAppLocked = false,
                )
            ),
            isCentralTask = isCentralTask,
        )
    }
}
