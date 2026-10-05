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

import javax.inject.Inject

/**
 * Provides categorized topic buckets used by the organizer engine.
 */
interface TopicProvider {
    suspend fun getTopics(): List<String>
}

/**
 * Standard topic provider for organizing installed apps into cohesive functional groups.
 */
open class DefaultTopicProvider @Inject constructor() : TopicProvider {

    override suspend fun getTopics(): List<String> {
        return listOf(
            TOPIC_COMMUNICATION,
            TOPIC_SOCIAL,
            TOPIC_PRODUCTIVITY,
            TOPIC_MEDIA,
            TOPIC_TOOLS,
            TOPIC_GAMES,
            TOPIC_FINANCE,
            TOPIC_SHOPPING,
            TOPIC_HEALTH
        )
    }

    companion object {
        const val TOPIC_COMMUNICATION = "Communication"
        const val TOPIC_SOCIAL = "Social"
        const val TOPIC_PRODUCTIVITY = "Work & Productivity"
        const val TOPIC_MEDIA = "Media & Entertainment"
        const val TOPIC_TOOLS = "Tools & Utilities"
        const val TOPIC_GAMES = "Games"
        const val TOPIC_FINANCE = "Finance"
        const val TOPIC_SHOPPING = "Shopping"
        const val TOPIC_HEALTH = "Health & Fitness"
    }
}
