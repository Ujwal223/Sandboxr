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

package com.sandboxr.launcher.organizer.generator

import com.sandboxr.launcher.model.data.FolderInfo
import com.sandboxr.launcher.model.data.ItemInfo
import javax.inject.Inject
import javax.inject.Provider

/**
 * Handles all aspects of space or folder creation from classification to layout generation.
 */
interface CreationSession {

    suspend fun startClassification(): List<TopicClassifiedItem>

    suspend fun startGeneration(selectedTopics: List<String>): GenerationResult

    suspend fun cancelSession() {}

    sealed class GenerationResult {
        data class Screens(val pages: List<List<ItemInfo>>) : GenerationResult()
        data class Folders(val folders: List<FolderInfo>) : GenerationResult()
    }

    enum class SessionType {
        SCREEN,
        FOLDER
    }

    class Factory @Inject constructor(
        private val folderCreationSessionProvider: Provider<FolderCreationSession>,
        private val screenCreationSessionProvider: Provider<ScreenCreationSession>
    ) {
        fun createSession(sessionType: SessionType): CreationSession {
            return when (sessionType) {
                SessionType.FOLDER -> folderCreationSessionProvider.get()
                SessionType.SCREEN -> screenCreationSessionProvider.get()
            }
        }
    }
}
