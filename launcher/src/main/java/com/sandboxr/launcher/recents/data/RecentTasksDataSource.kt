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
import java.util.function.Consumer

interface RecentTasksDataSource {
    fun getTasks(callback: Consumer<List<Task>>?): Int
}

interface RecentTasksKeysDataSource {
    fun getTaskKeys(callback: Consumer<List<Task.TaskKey>>?): Int
}

interface TaskVisualsChangeListener {
    fun onTaskIconChanged(pkg: String, userId: Int)
    fun onTaskThumbnailChanged(taskId: Int, thumbnailData: ThumbnailData?)
}

interface TaskVisualsChangeNotifier {
    fun addThumbnailChangeListener(listener: TaskVisualsChangeListener)
    fun removeThumbnailChangeListener(listener: TaskVisualsChangeListener)
}

interface TaskVisualsChangedDelegate : TaskVisualsChangeListener {
    fun registerTaskIconChangedCallback(callback: (String, Int) -> Unit)
    fun registerTaskThumbnailChangedCallback(callback: (Int, ThumbnailData?) -> Unit)
}
