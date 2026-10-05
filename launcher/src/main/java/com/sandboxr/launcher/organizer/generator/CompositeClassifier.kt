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

import com.sandboxr.launcher.model.data.ItemInfo
import javax.inject.Inject

/**
 * Combines multiple classifiers to categorize items with aggregated confidence scoring.
 */
class CompositeClassifier @Inject constructor(
    private val classifiers: List<ItemInfoClassifier>
) : ItemInfoClassifier {

    override suspend fun classify(
        items: List<ItemInfo>,
        topics: List<String>
    ): List<TopicClassifiedItem> {
        val results = mutableListOf<TopicClassifiedItem>()
        val classifiedItems = mutableSetOf<ItemInfo>()

        for (classifier in classifiers) {
            val unclassified = items.filterNot { classifiedItems.contains(it) }
            if (unclassified.isEmpty()) break

            val classified = classifier.classify(unclassified, topics)
            for (result in classified) {
                classifiedItems.add(result.itemInfo)
                results.add(result)
            }
        }
        return results
    }
}
