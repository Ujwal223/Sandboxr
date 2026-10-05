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

/**
 * Result of classifying an [ItemInfo] into a topic with an associated confidence score.
 */
data class TopicClassifiedItem(
    val itemInfo: ItemInfo,
    val topic: String,
    val score: Float = 1.0f
)

/**
 * Classifies a collection of [ItemInfo] objects into functional topics.
 */
interface ItemInfoClassifier {
    suspend fun classify(items: List<ItemInfo>, topics: List<String>): List<TopicClassifiedItem>
}
