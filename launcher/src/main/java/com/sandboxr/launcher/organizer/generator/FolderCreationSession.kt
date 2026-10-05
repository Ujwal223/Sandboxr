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

import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.FolderInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.model.repository.AppsListRepository
import javax.inject.Inject

/**
 * Session that organizes installed applications into categorized folders.
 */
class FolderCreationSession @Inject constructor(
    private val appsListRepository: AppsListRepository,
    private val classifier: ItemInfoClassifier,
    private val topicProvider: TopicProvider,
    private val folderPlacer: FolderPlacer = FolderPlacer()
) : CreationSession {

    private var topicClassifiedItems: List<TopicClassifiedItem> = emptyList()

    override suspend fun startClassification(): List<TopicClassifiedItem> {
        val apps = appsListRepository.appsListStateRef.value.apps.toList()
        val topics = topicProvider.getTopics()
        topicClassifiedItems = classifier.classify(apps, topics)
        return topicClassifiedItems
    }

    override suspend fun startGeneration(
        selectedTopics: List<String>
    ): CreationSession.GenerationResult {
        val folders = topicClassifiedItems
            .asSequence()
            .filter { selectedTopics.contains(it.topic) && it.score >= 0.5f }
            .groupBy { it.topic }
            .filter { (_, items) -> items.isNotEmpty() }
            .map { (topic, items) ->
                val folderInfo = FolderInfo().apply {
                    title = topic
                }
                val placedItems = folderPlacer.place(items)
                for (item in placedItems) {
                    val wsItem = if (item is WorkspaceItemInfo) item else (item as? AppInfo)?.let { WorkspaceItemInfo(it) } ?: WorkspaceItemInfo()
                    folderInfo.add(wsItem)
                }
                folderInfo
            }
            .toList()

        return CreationSession.GenerationResult.Folders(folders)
    }
}
