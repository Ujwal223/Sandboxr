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

import com.sandboxr.launcher.recents.data.RecentTasksRepository
import com.sandboxr.launcher.recents.domain.model.TaskModel
import javax.inject.Inject

class PreloadThumbnailUseCase @Inject constructor(
    private val tasksRepository: RecentTasksRepository
) {
    operator fun invoke(taskIds: List<Int>) {
        taskIds.forEach { taskId ->
            tasksRepository.getCurrentThumbnailById(taskId)
        }
    }
}

object DesktopLayoutUtils {
    fun calculateDesktopTaskBounds(
        displayBounds: android.graphics.Rect,
        taskIndex: Int,
        totalTasks: Int
    ): android.graphics.Rect {
        val width = (displayBounds.width() * 0.7f).toInt()
        val height = (displayBounds.height() * 0.7f).toInt()
        val offsetX = (taskIndex * 40).coerceAtMost(displayBounds.width() - width)
        val offsetY = (taskIndex * 40).coerceAtMost(displayBounds.height() - height)
        return android.graphics.Rect(
            displayBounds.left + offsetX,
            displayBounds.top + offsetY,
            displayBounds.left + offsetX + width,
            displayBounds.top + offsetY + height
        )
    }
}

class OrganizeDesktopTasksUseCase @Inject constructor() {
    operator fun invoke(tasks: List<TaskModel>): List<TaskModel> {
        return tasks.sortedByDescending { it.id }
    }
}
