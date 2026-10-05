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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentTasksRepositoryImpl @Inject constructor() : RecentTasksRepository {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasksFlow: Flow<List<Task>> = _tasks.asStateFlow()

    private val mThumbnails = ConcurrentHashMap<Int, MutableStateFlow<ThumbnailData?>>()
    private val mVisibleTasks = ConcurrentHashMap<Int, Set<Int>>()
    private var mHighResThumbnailsRequired = false

    override fun getAllTaskData(displayId: Int, forceRefresh: Boolean): Flow<List<Task>> {
        return _tasks.map { list -> list.filter { it.key.displayId == displayId } }
    }

    override fun getTaskDataById(taskId: Int): Flow<Task?> {
        return _tasks.map { list -> list.firstOrNull { it.key.id == taskId } }
    }

    override fun getThumbnailById(taskId: Int): Flow<ThumbnailData?> {
        return getOrCreateThumbnailFlow(taskId).asStateFlow()
    }

    override fun getCurrentThumbnailById(taskId: Int): ThumbnailData? {
        val direct = _tasks.value.firstOrNull { it.key.id == taskId }?.thumbnail
        return direct ?: mThumbnails[taskId]?.value
    }

    override fun setVisibleTasks(displayId: Int, visibleTaskIdList: Set<Int>) {
        mVisibleTasks[displayId] = visibleTaskIdList
    }

    override fun setHighResThumbnailsRequired(highResThumbnailsRequired: Boolean) {
        mHighResThumbnailsRequired = highResThumbnailsRequired
    }

    override fun removeTask(taskId: Int) {
        val current = _tasks.value
        _tasks.value = current.filter { it.key.id != taskId }
        mThumbnails.remove(taskId)
    }

    override fun clearAllTasks() {
        _tasks.value = emptyList()
        mThumbnails.clear()
        mVisibleTasks.clear()
    }

    override fun addOrUpdateTask(task: Task) {
        val current = _tasks.value.toMutableList()
        val index = current.indexOfFirst { it.key.id == task.key.id }
        if (index >= 0) {
            current[index] = task
        } else {
            current.add(0, task)
        }
        _tasks.value = current

        if (task.thumbnail != null) {
            getOrCreateThumbnailFlow(task.key.id).value = task.thumbnail
        }
    }

    fun updateThumbnail(taskId: Int, thumbnail: ThumbnailData?) {
        getOrCreateThumbnailFlow(taskId).value = thumbnail
        val current = _tasks.value.toMutableList()
        val index = current.indexOfFirst { it.key.id == taskId }
        if (index >= 0) {
            val updated = current[index].copy()
            updated.thumbnail = thumbnail
            current[index] = updated
            _tasks.value = current
        }
    }

    private fun getOrCreateThumbnailFlow(taskId: Int): MutableStateFlow<ThumbnailData?> {
        return mThumbnails.computeIfAbsent(taskId) {
            MutableStateFlow(_tasks.value.firstOrNull { it.key.id == taskId }?.thumbnail)
        }
    }
}
