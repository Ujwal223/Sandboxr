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
import com.sandboxr.launcher.appfunctions.workspace.WorkspaceAppFunctions
import com.sandboxr.launcher.appfunctions.workspace.WorkspaceRepository
import com.sandboxr.launcher.appfunctions.workspace.provider.InstalledItemsProvider
import com.sandboxr.launcher.appfunctions.workspace.provider.WorkspaceProvider
import com.sandboxr.launcher.model.data.WorkspaceData
import dagger.Binds
import dagger.Module
import dagger.Provides

/**
 * Dagger module for binding workspace functions dependencies.
 */
@Module
abstract class WorkspaceFunctionsModule {

    @Binds
    abstract fun bindWorkspaceRepository(impl: WorkspaceRepositoryImpl): WorkspaceRepository

    @Binds
    abstract fun bindWorkspaceProvider(impl: LauncherWorkspaceProvider): WorkspaceProvider<WorkspaceData>

    @Binds
    abstract fun bindInstalledAppsProvider(impl: LauncherInstalledAppsProvider): InstalledItemsProvider<LauncherActivityInfo>

    @Binds
    abstract fun bindInstalledWidgetsProvider(impl: LauncherInstalledWidgetsProvider): InstalledItemsProvider<AppWidgetProviderInfo>

    companion object {
        @Provides
        fun provideWorkspaceAppFunctions(repository: WorkspaceRepository): WorkspaceAppFunctions {
            return WorkspaceAppFunctions(repository)
        }
    }
}
