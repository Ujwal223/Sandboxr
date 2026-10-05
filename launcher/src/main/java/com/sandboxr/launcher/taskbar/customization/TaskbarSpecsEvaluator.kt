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

package com.sandboxr.launcher.taskbar.customization

import com.sandboxr.launcher.DeviceProfile

/**
 * Evaluates the taskbar specifications based on available space, grid size, and icons.
 */
class TaskbarSpecsEvaluator(
    private val deviceProfile: DeviceProfile,
    private val taskbarFeatureEvaluator: TaskbarFeatureEvaluator,
    val numRows: Int = deviceProfile.inv?.numRows ?: 5,
    val numColumns: Int = deviceProfile.inv?.numColumns ?: 6,
) {
    val taskbarIconSize: TaskbarIconSize
        get() = if (taskbarFeatureEvaluator.supportsTransitionToTransientTaskbar) {
            TaskbarIconSpecs.defaultTransientIconSize
        } else {
            TaskbarIconSpecs.defaultPersistentIconSize
        }

    val taskbarIconTouchSize: Float
        get() = maxOf(
            TaskbarIconSpecs.minimumTaskbarIconTouchSize.size.toFloat(),
            taskbarIconSize.size.toFloat(),
        )

    val numShownHotseatIcons: Int
        get() = deviceProfile.inv?.numDatabaseHotseatIcons ?: deviceProfile.inv?.numColumns ?: 6

    val maxPinnableCount: Int
        get() = deviceProfile.inv?.numDatabaseHotseatIcons ?: numShownHotseatIcons

    private val defaultIconSize: TaskbarIconSize =
        if (taskbarFeatureEvaluator.isPinned || taskbarFeatureEvaluator.isThreeButtonNav) {
            TaskbarIconSpecs.defaultPersistentIconSize
        } else {
            TaskbarIconSpecs.defaultTransientIconSize
        }

    val taskbarIconPadding: Float = (taskbarIconTouchSize - defaultIconSize.size.toFloat()) / 2.0f

    val taskbarIconMargin: TaskbarIconMarginSize =
        if (taskbarFeatureEvaluator.isTransient) {
            TaskbarIconSpecs.defaultTransientIconMargin
        } else {
            TaskbarIconSpecs.defaultPersistentIconMargin
        }

    fun getIconSizeByGrid(columns: Int, rows: Int): TaskbarIconSize {
        return if (taskbarFeatureEvaluator.supportsTransitionToTransientTaskbar) {
            TaskbarIconSpecs.transientTaskbarIconSizeByGridSize.getOrDefault(
                TransientTaskbarIconSizeKey(
                    columns,
                    rows,
                    deviceProfile.isLandscape,
                ),
                TaskbarIconSpecs.defaultTransientIconSize,
            )
        } else {
            TaskbarIconSpecs.defaultPersistentIconSize
        }
    }

    fun getIconSizeStepDown(iconSize: TaskbarIconSize): TaskbarIconSize {
        if (!taskbarFeatureEvaluator.isTransient) return TaskbarIconSpecs.defaultPersistentIconSize

        val currentIconSizeIndex = TaskbarIconSpecs.transientTaskbarIconSizes.indexOf(iconSize)
        return if (currentIconSizeIndex > 0) {
            TaskbarIconSpecs.transientTaskbarIconSizes[currentIconSizeIndex - 1]
        } else {
            TaskbarIconSpecs.minimumIconSize
        }
    }

    fun calculateSpaceNeeded(containers: List<TaskbarContainer>): Int {
        return containers.sumOf { it.spaceNeeded }
    }
}

data class TaskbarIconSize(val size: Int)

data class TransientTaskbarIconSizeKey(val columns: Int, val rows: Int, val isLandscape: Boolean)

data class TaskbarIconMarginSize(val size: Int)
