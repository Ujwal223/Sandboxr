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

package com.sandboxr.launcher.cuebar.ui.viewmodel

import android.graphics.drawable.Drawable

/**
 * Action type category.
 */
enum class ActionType {
    MA,
    MR,
    Unknown
}

/**
 * ViewModel for icon presentation in action chips and pills.
 */
data class IconViewModel(
    val small: Drawable,
    val large: Drawable,
    val iconId: String,
    val repeatCount: Int = 0
)

/**
 * ViewModel representing an actionable item presented by CueBar in Compose UI.
 */
data class ActionViewModel(
    val icon: IconViewModel,
    val label: String,
    val attribution: String? = null,
    val onClick: () -> Unit,
    val onLongClick: () -> Unit = {},
    val actionType: ActionType = ActionType.Unknown,
    val oneTapEnabled: Boolean = false,
    val oneTapDelayMs: Long = 0L,
)
