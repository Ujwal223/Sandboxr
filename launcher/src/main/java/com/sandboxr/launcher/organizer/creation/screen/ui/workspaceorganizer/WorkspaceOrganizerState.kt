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

package com.sandboxr.launcher.organizer.creation.screen.ui.workspaceorganizer

import com.sandboxr.launcher.model.data.ItemInfo

/**
 * Represents a workspace screen and the items placed upon it.
 */
data class WorkspaceScreenData(
    val screenId: Int,
    val pageIndex: Int,
    val items: List<ItemInfo> = emptyList()
)

/**
 * UI State for the Workspace Organizer screen.
 */
data class WorkspaceOrganizerState(
    val screens: List<WorkspaceScreenData> = emptyList(),
    val selectedPageIndex: Int = 0,
    val selectedItem: ItemInfo? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false
)
