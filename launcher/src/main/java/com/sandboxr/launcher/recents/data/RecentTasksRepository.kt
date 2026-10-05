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

package com.sandboxr.launcher.recents.data

import com.android.systemui.shared.recents.model.Task
import com.android.systemui.shared.recents.model.ThumbnailData
import kotlinx.coroutines.flow.Flow

interface RecentTasksRepository {
    /** Gets all recent tasks, refreshing from data source if [forceRefresh] is true. */
    fun getAllTaskData(displayId: Int, forceRefresh: Boolean = false): Flow<List<Task>>

    /** Gets the task data associated with [taskId]. */
    fun getTaskDataById(taskId: Int): Flow<Task?>

    /** Gets the thumbnail data flow associated with [taskId]. */
    fun getThumbnailById(taskId: Int): Flow<ThumbnailData?>

    /** Gets the current snapshot thumbnail immediately if available. */
    fun getCurrentThumbnailById(taskId: Int): ThumbnailData?

    /** Sets the set of currently visible tasks in recents overview. */
    fun setVisibleTasks(displayId: Int, visibleTaskIdList: Set<Int>)

    /** Sets whether high-res thumbnails are required. */
    fun setHighResThumbnailsRequired(highResThumbnailsRequired: Boolean)

    /** Removes a task by its ID (e.g. on swipe-to-dismiss). */
    fun removeTask(taskId: Int)

    /** Removes all tasks (e.g. on clear-all). */
    fun clearAllTasks()

    /** Adds or updates a task. */
    fun addOrUpdateTask(task: Task)
}
