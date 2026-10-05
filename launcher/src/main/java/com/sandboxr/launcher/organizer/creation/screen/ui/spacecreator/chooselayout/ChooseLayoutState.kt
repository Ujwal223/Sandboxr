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

package com.sandboxr.launcher.organizer.creation.screen.ui.spacecreator.chooselayout

data class LayoutOption(
    val id: String,
    val title: String,
    val description: String,
    val columns: Int,
    val rows: Int
)

data class ChooseLayoutState(
    val options: List<LayoutOption> = listOf(
        LayoutOption("GRID_4X4", "Compact (4x4)", "Clean layout with larger spacing", 4, 4),
        LayoutOption("GRID_4X5", "Standard (4x5)", "Default launcher grid layout", 4, 5),
        LayoutOption("GRID_5X5", "Spacious (5x5)", "Maximum density for heavy multitasking", 5, 5)
    ),
    val selectedOptionId: String = "GRID_4X5"
)
