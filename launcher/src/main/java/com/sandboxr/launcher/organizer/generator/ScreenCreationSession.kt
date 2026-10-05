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

import android.graphics.Point
import com.sandboxr.launcher.InvariantDeviceProfile
import com.sandboxr.launcher.model.repository.AppsListRepository
import javax.inject.Inject

/**
 * Session that generates complete space/screen layouts for selected topics.
 */
class ScreenCreationSession @Inject constructor(
    private val appsListRepository: AppsListRepository,
    private val classifier: ItemInfoClassifier,
    private val topicProvider: TopicProvider,
    private val templateGenerator: TemplateGenerator,
    private val placer: Placer = HeuristicScreenPlacer()
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
        val filtered = topicClassifiedItems.filter { selectedTopics.contains(it.topic) }
        val templates = templateGenerator.generateTemplates(
            n = selectedTopics.size.coerceAtLeast(1),
            gridSize = Point(4, 5)
        )
        val pages = placer.place(filtered, templates)
        return CreationSession.GenerationResult.Screens(pages)
    }
}
