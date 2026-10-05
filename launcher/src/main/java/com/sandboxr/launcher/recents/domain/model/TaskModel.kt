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

package com.sandboxr.launcher.recents.domain.model

import android.graphics.drawable.Drawable
import com.android.systemui.shared.recents.model.ThumbnailData
import java.time.Duration

typealias TaskId = Int

/**
 * Data class representing a task in the application.
 */
data class TaskModel(
    val id: TaskId,
    val packageName: String,
    val title: String?,
    val titleDescription: String?,
    val icon: Drawable?,
    val thumbnail: ThumbnailData?,
    val backgroundColor: Int,
    val isLocked: Boolean,
    val isMinimized: Boolean,
    val remainingAppDuration: Duration?,
    val isAppLocked: Boolean,
)
