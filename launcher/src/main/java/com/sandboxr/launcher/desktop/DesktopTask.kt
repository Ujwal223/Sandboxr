/*
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

package com.sandboxr.launcher.desktop

import com.android.systemui.shared.recents.model.Task
import com.sandboxr.launcher.recents.util.GroupTask
import com.sandboxr.launcher.recents.views.TaskViewType
import java.util.Objects

/**
 * A container holding N tasks that belong to a single desktop workspace tile in overview.
 */
class DesktopTask(
    val deskId: Int,
    desktopDisplayId: Int,
    tasks: List<Task>
) : GroupTask(tasks, desktopDisplayId, TaskViewType.DESKTOP) {

    override fun copy(): DesktopTask = DesktopTask(deskId, displayId, tasks.map { it.copy() })

    override fun toString(): String = "DesktopTask(deskId=$deskId, displayId=$displayId, tasksCount=${tasks.size})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DesktopTask) return false
        if (deskId != other.deskId) return false
        if (displayId != other.displayId) return false
        return super.equals(other)
    }

    override fun hashCode(): Int = Objects.hash(super.hashCode(), deskId, displayId)
}
