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

package com.sandboxr.launcher.organizer.creation.screen.ui.spacecreator

import com.sandboxr.launcher.organizer.generator.CreationSession

data class SpaceCreatorTopic(
    val topic: String,
    val appCount: Int
)

/**
 * UI State for Space / Screen creation flow.
 */
data class CreateScreenState(
    val availableTopics: List<SpaceCreatorTopic> = emptyList(),
    val selectedTopics: Set<String> = emptySet(),
    val selectedLayoutType: String = LAYOUT_STANDARD_4X5,
    val generatedResult: CreationSession.GenerationResult? = null,
    val isLoading: Boolean = false,
    val isComplete: Boolean = false
) {
    companion object {
        const val LAYOUT_COMPACT_4X4 = "4x4 Compact"
        const val LAYOUT_STANDARD_4X5 = "4x5 Standard"
        const val LAYOUT_SPACIOUS_5X5 = "5x5 Spacious"
    }
}
