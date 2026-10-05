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

import android.graphics.drawable.Drawable
import android.util.SparseArray
import com.android.systemui.shared.recents.model.Task
import com.android.systemui.shared.recents.model.ThumbnailData
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasksRepository @Inject constructor(
    private val recentTasksRepository: RecentTasksRepository
) : TaskVisualsChangeNotifier, TaskVisualsChangedDelegate {

    private val mTaskCache = SparseArray<Task>()
    private val mThumbnailListeners = CopyOnWriteArrayList<TaskVisualsChangeListener>()
    private val mIconCallbacks = CopyOnWriteArrayList<(String, Int) -> Unit>()
    private val mThumbnailCallbacks = CopyOnWriteArrayList<(Int, ThumbnailData?) -> Unit>()

    fun getTask(taskId: Int): Task? {
        return mTaskCache.get(taskId)
    }

    fun putTask(task: Task) {
        mTaskCache.put(task.key.id, task)
        recentTasksRepository.addOrUpdateTask(task)
    }

    fun removeTask(taskId: Int) {
        mTaskCache.remove(taskId)
        recentTasksRepository.removeTask(taskId)
    }

    fun clear() {
        mTaskCache.clear()
        recentTasksRepository.clearAllTasks()
    }

    override fun addThumbnailChangeListener(listener: TaskVisualsChangeListener) {
        mThumbnailListeners.add(listener)
    }

    override fun removeThumbnailChangeListener(listener: TaskVisualsChangeListener) {
        mThumbnailListeners.remove(listener)
    }

    override fun registerTaskIconChangedCallback(callback: (String, Int) -> Unit) {
        mIconCallbacks.add(callback)
    }

    override fun registerTaskThumbnailChangedCallback(callback: (Int, ThumbnailData?) -> Unit) {
        mThumbnailCallbacks.add(callback)
    }

    override fun onTaskIconChanged(pkg: String, userId: Int) {
        mThumbnailListeners.forEach { it.onTaskIconChanged(pkg, userId) }
        mIconCallbacks.forEach { it(pkg, userId) }
    }

    override fun onTaskThumbnailChanged(taskId: Int, thumbnailData: ThumbnailData?) {
        mThumbnailListeners.forEach { it.onTaskThumbnailChanged(taskId, thumbnailData) }
        mThumbnailCallbacks.forEach { it(taskId, thumbnailData) }
    }
}
