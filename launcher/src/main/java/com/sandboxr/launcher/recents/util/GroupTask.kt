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

package com.sandboxr.launcher.recents.util

import com.android.systemui.shared.recents.model.Task
import com.sandboxr.launcher.recents.views.TaskViewType
import java.util.Objects

/**
 * Container for grouped or individual tasks in recents overview.
 */
abstract class GroupTask(
    val tasks: List<Task>,
    val displayId: Int,
    val taskViewType: TaskViewType,
) {
    fun containsTask(taskId: Int): Boolean = tasks.any { it.key.id == taskId }

    fun containsPackage(packageName: String?): Boolean =
        tasks.any { it.key.getPackageName() == packageName }

    fun isEmpty(): Boolean = tasks.isEmpty()

    abstract fun copy(): GroupTask

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupTask) return false
        return taskViewType == other.taskViewType && tasks == other.tasks
    }

    override fun hashCode(): Int = Objects.hash(tasks, taskViewType)
}

class SingleTask(val task: Task) :
    GroupTask(listOf(task), task.key.displayId, TaskViewType.SINGLE) {

    override fun copy(): GroupTask = SingleTask(task.copy())

    override fun toString(): String = "SingleTask(task=$task)"
}

class SplitTask(val primaryTask: Task, val secondaryTask: Task) :
    GroupTask(listOf(primaryTask, secondaryTask), primaryTask.key.displayId, TaskViewType.GROUPED) {

    override fun copy(): GroupTask = SplitTask(primaryTask.copy(), secondaryTask.copy())

    override fun toString(): String = "SplitTask(primary=$primaryTask, secondary=$secondaryTask)"
}
