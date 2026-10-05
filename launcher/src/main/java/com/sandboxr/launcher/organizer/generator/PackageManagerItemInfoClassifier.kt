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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.ItemInfo
import javax.inject.Inject

/**
 * Classifies apps based on Android's system [ApplicationInfo.category] and package semantics.
 */
class PackageManagerItemInfoClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) : ItemInfoClassifier {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    override suspend fun classify(
        items: List<ItemInfo>,
        topics: List<String>
    ): List<TopicClassifiedItem> {
        return items.mapNotNull { item ->
            val pkg = item.targetPackage ?: item.intent?.component?.packageName ?: (item as? AppInfo)?.componentName?.packageName
            val category = resolveCategory(item, pkg)
            val matchedTopic = matchTopic(category, pkg, topics)
            if (matchedTopic != null) {
                TopicClassifiedItem(item, matchedTopic, 1.0f)
            } else {
                null
            }
        }
    }

    private fun resolveCategory(item: ItemInfo, pkg: String?): Int {
        if (pkg != null) {
            try {
                val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
                return appInfo.category
            } catch (e: Exception) {
                // fall through
            }
        }
        item.intent?.let { intent ->
            val resolved = launcherApps?.resolveActivity(intent, item.user)?.applicationInfo
            if (resolved != null) return resolved.category
        }
        return ApplicationInfo.CATEGORY_UNDEFINED
    }

    private fun matchTopic(category: Int, pkg: String?, topics: List<String>): String? {
        val topicByName = when (category) {
            ApplicationInfo.CATEGORY_GAME -> topics.find { it.contains("Game", ignoreCase = true) }
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_IMAGE -> topics.find { it.contains("Media", ignoreCase = true) }
            ApplicationInfo.CATEGORY_SOCIAL -> topics.find { it.contains("Social", ignoreCase = true) }
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> topics.find { it.contains("Productivity", ignoreCase = true) || it.contains("Work", ignoreCase = true) }
            ApplicationInfo.CATEGORY_MAPS -> topics.find { it.contains("Tool", ignoreCase = true) || it.contains("Travel", ignoreCase = true) }
            ApplicationInfo.CATEGORY_NEWS -> topics.find { it.contains("News", ignoreCase = true) || it.contains("Media", ignoreCase = true) }
            else -> null
        }

        if (topicByName != null) return topicByName

        // Keyword heuristics on package name
        if (pkg != null) {
            val lower = pkg.lowercase()
            when {
                lower.contains("message") || lower.contains("chat") || lower.contains("dialer") || lower.contains("phone") || lower.contains("sms") ->
                    return topics.find { it.contains("Communication", ignoreCase = true) } ?: topics.firstOrNull()
                lower.contains("social") || lower.contains("twitter") || lower.contains("instagram") || lower.contains("facebook") ->
                    return topics.find { it.contains("Social", ignoreCase = true) }
                lower.contains("calc") || lower.contains("clock") || lower.contains("setting") || lower.contains("file") || lower.contains("browser") ->
                    return topics.find { it.contains("Tool", ignoreCase = true) }
                lower.contains("doc") || lower.contains("sheet") || lower.contains("office") || lower.contains("note") || lower.contains("task") ->
                    return topics.find { it.contains("Productivity", ignoreCase = true) || it.contains("Work", ignoreCase = true) }
                lower.contains("music") || lower.contains("video") || lower.contains("player") || lower.contains("gallery") || lower.contains("camera") ->
                    return topics.find { it.contains("Media", ignoreCase = true) }
                lower.contains("game") ->
                    return topics.find { it.contains("Game", ignoreCase = true) }
                lower.contains("bank") || lower.contains("pay") || lower.contains("wallet") ->
                    return topics.find { it.contains("Finance", ignoreCase = true) }
            }
        }

        return topics.firstOrNull()
    }
}
