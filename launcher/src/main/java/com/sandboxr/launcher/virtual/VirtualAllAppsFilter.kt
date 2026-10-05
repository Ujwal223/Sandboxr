/*
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

package com.sandboxr.launcher.virtual

import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.ui.AppItem
import java.util.Locale

/**
 * Filter scope for the All Apps drawer.
 */
sealed class EnvironmentFilterMode {
    object All : EnvironmentFilterMode()
    object SystemOnly : EnvironmentFilterMode()
    data class Environment(val envId: String) : EnvironmentFilterMode()
}

/**
 * Filtering and search engine for the GrapheneOS All Apps drawer and Compose app grids.
 * Allows instant tabbed and scoped filtering between System apps and individual
 * container environments.
 */
class VirtualAllAppsFilter {

    var activeFilter: EnvironmentFilterMode = EnvironmentFilterMode.All
        private set

    fun setFilterMode(mode: EnvironmentFilterMode) {
        this.activeFilter = mode
    }

    /**
     * Filters a list of [AppItem]s according to the active filter mode and optional search query.
     */
    fun filterAppItems(
        items: List<AppItem>,
        searchQuery: String = ""
    ): List<AppItem> {
        val query = searchQuery.trim().lowercase(Locale.getDefault())

        return items.filter { item ->
            val matchesEnv = when (val mode = activeFilter) {
                is EnvironmentFilterMode.All -> true
                is EnvironmentFilterMode.SystemOnly -> item.isSystemApp || item.envId == "system"
                is EnvironmentFilterMode.Environment -> item.envId == mode.envId
            }

            val matchesSearch = if (query.isEmpty()) {
                true
            } else {
                item.label.lowercase(Locale.getDefault()).contains(query) ||
                        item.packageName.lowercase(Locale.getDefault()).contains(query)
            }

            matchesEnv && matchesSearch
        }.sortedWith(
            compareBy<AppItem> { !it.isSystemApp }
                .thenBy { it.label.lowercase(Locale.getDefault()) }
        )
    }

    /**
     * Filters a list of canonical [VirtualAppInfo]s according to active filter and search query.
     */
    fun filterVirtualApps(
        apps: List<VirtualAppInfo>,
        searchQuery: String = ""
    ): List<VirtualAppInfo> {
        val query = searchQuery.trim().lowercase(Locale.getDefault())

        return apps.filter { app ->
            val matchesEnv = when (val mode = activeFilter) {
                is EnvironmentFilterMode.All -> true
                is EnvironmentFilterMode.SystemOnly -> false
                is EnvironmentFilterMode.Environment -> app.envId == mode.envId
            }

            val matchesSearch = if (query.isEmpty()) {
                true
            } else {
                app.label.lowercase(Locale.getDefault()).contains(query) ||
                        app.packageName.lowercase(Locale.getDefault()).contains(query)
            }

            matchesEnv && matchesSearch
        }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }
}
