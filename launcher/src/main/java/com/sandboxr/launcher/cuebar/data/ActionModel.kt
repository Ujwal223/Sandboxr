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

package com.sandboxr.launcher.cuebar.data

import android.graphics.drawable.Drawable

/**
 * Data model representing an ambient actionable suggestion displayed on CueBar.
 */
data class ActionModel(
    val icon: IconModel,
    val label: String,
    val attribution: String? = null,
    val onPerformAction: () -> Unit,
    val onPerformLongClick: () -> Unit = {},
    val taskId: Int = -1,
    val actionType: String? = null,
    val oneTapEnabled: Boolean = false,
    val oneTapDelayMs: Long = 0L,
    val isEnabledWithImeVisible: Boolean = false,
) {
    companion object {
        const val VERSION = 2
        const val INVALID_TASK_ID = -1
    }
}

/**
 * Represents icon drawables and identifier for an ambient action.
 */
data class IconModel(
    val small: Drawable,
    val large: Drawable,
    val iconId: String,
) {
    companion object {
        const val VERSION = 1
    }
}
