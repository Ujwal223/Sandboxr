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

package com.sandboxr.launcher.workspacefunctions

import android.appwidget.AppWidgetProviderInfo
import android.content.pm.LauncherActivityInfo
import com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_DESKTOP
import com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_HOTSEAT
import com.sandboxr.launcher.appfunctions.workspace.HotseatItemSpec
import com.sandboxr.launcher.appfunctions.workspace.HotseatSpec
import com.sandboxr.launcher.appfunctions.workspace.UnplacedAppSpec
import com.sandboxr.launcher.appfunctions.workspace.UnplacedWidgetSpec
import com.sandboxr.launcher.appfunctions.workspace.WorkspaceItemSpec
import com.sandboxr.launcher.appfunctions.workspace.WorkspaceRepository
import com.sandboxr.launcher.appfunctions.workspace.WorkspaceScreenSpec
import com.sandboxr.launcher.appfunctions.workspace.WorkspaceSpec
import com.sandboxr.launcher.appfunctions.workspace.WorkspaceTransaction
import com.sandboxr.launcher.appfunctions.workspace.provider.InstalledItemsProvider
import com.sandboxr.launcher.appfunctions.workspace.provider.WorkspaceProvider
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceData
import javax.inject.Inject

/**
 * Concrete implementation of [WorkspaceRepository] connecting AppFunctions to launcher models.
 */
class WorkspaceRepositoryImpl @Inject constructor(
    private val workspaceProvider: WorkspaceProvider<WorkspaceData>,
    private val installedAppsProvider: InstalledItemsProvider<LauncherActivityInfo>,
    private val installedWidgetsProvider: InstalledItemsProvider<AppWidgetProviderInfo>
) : WorkspaceRepository {

    override suspend fun getWorkspace(): WorkspaceSpec {
        val data = workspaceProvider.getWorkspace()
        val screenItems = data.filter { it.container == CONTAINER_DESKTOP }.map { item: ItemInfo ->
            WorkspaceItemSpec(
                id = item.id,
                cellX = item.cellX,
                cellY = item.cellY,
                spanX = item.spanX,
                spanY = item.spanY,
                title = item.title?.toString() ?: "",
                packageName = item.targetPackage ?: item.targetComponent?.packageName ?: "",
                itemType = item.itemType
            )
        }
        val hotseatItems = data.filter { it.container == CONTAINER_HOTSEAT }.map { item: ItemInfo ->
            HotseatItemSpec(
                id = item.id,
                rank = item.screenId,
                title = item.title?.toString() ?: "",
                packageName = item.targetPackage ?: item.targetComponent?.packageName ?: "",
                itemType = item.itemType
            )
        }
        return WorkspaceSpec(
            screens = listOf(WorkspaceScreenSpec(screenItems)),
            hotseat = HotseatSpec(hotseatItems),
            rows = null,
            columns = null
        )
    }

    override suspend fun getInstalledApps(orderByUsageStats: Boolean): List<UnplacedAppSpec> {
        val apps = installedAppsProvider.getInstalledItems(orderByUsageStats)
        return apps.map { info ->
            UnplacedAppSpec(
                packageName = info.applicationInfo.packageName,
                className = info.componentName.className,
                label = info.label?.toString() ?: ""
            )
        }
    }

    override suspend fun getInstalledWidgets(orderByUsageStats: Boolean): List<UnplacedWidgetSpec> {
        val widgets = installedWidgetsProvider.getInstalledItems(orderByUsageStats)
        return widgets.map { info ->
            UnplacedWidgetSpec(
                providerPackage = info.provider.packageName,
                providerClass = info.provider.className,
                label = info.label ?: "",
                minSpanX = 1,
                minSpanY = 1
            )
        }
    }

    override fun newTransaction(): WorkspaceTransaction {
        return WorkspaceTransactionImpl(this)
    }
}
