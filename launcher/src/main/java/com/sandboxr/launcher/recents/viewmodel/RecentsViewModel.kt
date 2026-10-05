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

package com.sandboxr.launcher.recents.viewmodel

import com.android.systemui.shared.recents.model.ThumbnailData
import com.sandboxr.launcher.recents.data.AppTimersRepository
import com.sandboxr.launcher.recents.data.RecentTasksRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentsViewModel
@Inject
constructor(
    private val recentsTasksRepository: RecentTasksRepository,
    private val recentsViewData: RecentsViewData,
    private val appTimersRepository: AppTimersRepository,
) {
    var visibleTaskIds: Set<Int> = emptySet()
        private set

    fun refreshAllTaskData(displayId: Int = 0) {
        recentsTasksRepository.getAllTaskData(displayId, true)
    }

    fun updateVisibleTasks(visibleTaskIdList: List<Int>, displayId: Int = 0) {
        visibleTaskIds = visibleTaskIdList.toSet()
        recentsTasksRepository.setVisibleTasks(displayId, visibleTaskIds)
    }

    fun updateTasksFullyVisible(taskIds: Set<Int>) {
        recentsViewData.settledFullyVisibleTaskIds.value = taskIds
    }

    fun updateCentralTaskIds(taskIds: Set<Int>) {
        recentsViewData.centralTaskIds.value = taskIds
    }

    fun setOverlayEnabled(isOverlayEnabled: Boolean) {
        recentsViewData.overlayEnabled.value = isOverlayEnabled
    }

    fun setHighResThumbnailsRequired(highResThumbnailsRequired: Boolean) {
        recentsTasksRepository.setHighResThumbnailsRequired(highResThumbnailsRequired)
    }

    fun onReset() {
        updateVisibleTasks(emptyList())
        appTimersRepository.invalidateCache()
    }

    fun updateRunningTask(taskIds: Set<Int>) {
        recentsViewData.runningTaskIds.value = taskIds
    }

    fun setRunningTaskShowScreenshot(showScreenshot: Boolean) {
        recentsViewData.runningTaskShowScreenshot.value = showScreenshot
    }

    fun dismissTask(taskId: Int) {
        recentsTasksRepository.removeTask(taskId)
    }

    fun clearAllTasks() {
        recentsTasksRepository.clearAllTasks()
    }
}
