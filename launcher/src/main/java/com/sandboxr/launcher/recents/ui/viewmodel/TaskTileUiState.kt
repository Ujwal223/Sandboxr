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

package com.sandboxr.launcher.recents.ui.viewmodel

import android.graphics.drawable.Drawable
import com.android.systemui.shared.recents.model.ThumbnailData
import java.time.Duration

data class TaskTileUiState(
    val tasks: List<TaskData>,
    val hasHeader: Boolean = true,
    val sysUiStatusNavFlags: Int = 0,
    val taskOverlayEnabled: Boolean = false,
    val isCentralTask: Boolean = false,
)

sealed class TaskData {
    abstract val taskId: Int

    data class NoData(override val taskId: Int) : TaskData()

    data class Data(
        override val taskId: Int,
        val packageName: String,
        val title: String?,
        val titleDescription: String?,
        val icon: Drawable?,
        val thumbnailData: ThumbnailData?,
        val backgroundColor: Int,
        val isLocked: Boolean,
        val isLiveTile: Boolean,
        val remainingAppTimerDuration: Duration?,
        val isAppLocked: Boolean,
    ) : TaskData()
}
