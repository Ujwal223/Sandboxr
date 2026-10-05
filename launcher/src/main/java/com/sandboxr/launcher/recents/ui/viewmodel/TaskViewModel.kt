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

package com.sandboxr.launcher.recents.ui.viewmodel

import com.sandboxr.launcher.recents.domain.model.TaskModel
import com.sandboxr.launcher.recents.domain.usecase.GetTaskUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskViewModel @Inject constructor(
    private val getTaskUseCase: GetTaskUseCase,
) {
    fun getTaskUiState(taskId: Int): Flow<TaskTileUiState> {
        return getTaskUseCase(taskId).map { taskModel ->
            if (taskModel == null) {
                TaskTileUiState(tasks = listOf(TaskData.NoData(taskId)))
            } else {
                TaskTileUiState(
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
                    )
                )
            }
        }
    }
}

class GroupedTaskViewModel @Inject constructor(
    private val getTaskUseCase: GetTaskUseCase,
) {
    fun getGroupedTaskUiState(taskIds: List<Int>): Flow<TaskTileUiState> {
        return getTaskUseCase(taskIds.firstOrNull() ?: 0).map { taskModel ->
            if (taskModel == null) {
                TaskTileUiState(tasks = taskIds.map { TaskData.NoData(it) })
            } else {
                TaskTileUiState(
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
                    )
                )
            }
        }
    }
}
