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

import android.graphics.Rect

/**
 * Configuration options for layout of tasks in recents overview.
 */
data class TaskLayoutConfig(
    val taskWidth: Int = 0,
    val taskHeight: Int = 0,
    val pageSpacing: Int = 16,
    val taskCornerRadius: Float = 24f,
    val isRtl: Boolean = false,
    val isGrid: Boolean = false,
    val scale: Float = 1f,
)

/**
 * Layout state of recents overview.
 */
data class TaskLayoutState(
    val screenBounds: Rect = Rect(),
    val availableRect: Rect = Rect(),
    val config: TaskLayoutConfig = TaskLayoutConfig(),
    val focusedTaskIndex: Int = 0,
    val scrollOffset: Int = 0,
)
