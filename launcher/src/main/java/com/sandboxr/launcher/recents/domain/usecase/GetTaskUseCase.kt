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

package com.sandboxr.launcher.recents.domain.usecase

import android.os.UserHandle
import com.sandboxr.launcher.recents.data.RecentTasksRepository
import com.sandboxr.launcher.recents.data.UserLockedStateRepository
import com.sandboxr.launcher.recents.domain.model.TaskModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetTaskUseCase
@Inject
constructor(
    private val tasksRepository: RecentTasksRepository,
    private val getRemainingAppTimerDurationUseCase: GetRemainingAppTimerDurationUseCase,
    private val userLockedStateRepository: UserLockedStateRepository,
) {
    operator fun invoke(taskId: Int): Flow<TaskModel?> =
        tasksRepository.getTaskDataById(taskId).map { task ->
            if (task == null) return@map null

            val packageName = task.key.getPackageName() ?: ""
            val remainingDuration = getRemainingAppTimerDurationUseCase(
                packageName = packageName,
                userHandle = UserHandle.getUserHandleForUid(task.key.userId),
            )

            val isLocked = task.isLocked || userLockedStateRepository.getIsUserLocked(task.key.userId)

            TaskModel(
                id = task.key.id,
                packageName = packageName,
                title = task.title,
                titleDescription = task.titleDescription,
                icon = task.icon,
                thumbnail = task.thumbnail,
                backgroundColor = task.colorBackground,
                isLocked = isLocked,
                isMinimized = false,
                remainingAppDuration = remainingDuration,
                isAppLocked = false,
            )
        }
}
