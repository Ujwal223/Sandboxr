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

import com.sandboxr.launcher.appfunctions.workspace.HotseatSpec
import com.sandboxr.launcher.appfunctions.workspace.RemoveItemParamsSpec
import com.sandboxr.launcher.appfunctions.workspace.UnplacedAppSpec
import com.sandboxr.launcher.appfunctions.workspace.UnplacedWidgetSpec
import com.sandboxr.launcher.appfunctions.workspace.WorkspaceAppFunctions
import com.sandboxr.launcher.appfunctions.workspace.WorkspaceRepository
import com.sandboxr.launcher.appfunctions.workspace.WorkspaceSpec
import com.sandboxr.launcher.appfunctions.workspace.WorkspaceTransaction
import dagger.Module
import dagger.Provides

/**
 * Module providing no-op [WorkspaceAppFunctions] for contexts where workspace functions are disabled.
 */
@Module
class NoOpWorkspaceFunctionsModule {

    @Provides
    fun provideWorkspaceAppFunctions(): WorkspaceAppFunctions {
        return WorkspaceAppFunctions(NoOpWorkspaceRepository())
    }

    private class NoOpWorkspaceRepository : WorkspaceRepository {
        override suspend fun getWorkspace(): WorkspaceSpec {
            return WorkspaceSpec(
                screens = emptyList(),
                hotseat = HotseatSpec(emptyList()),
                rows = null,
                columns = null
            )
        }

        override suspend fun getInstalledApps(orderByUsageStats: Boolean): List<UnplacedAppSpec> {
            return emptyList()
        }

        override suspend fun getInstalledWidgets(orderByUsageStats: Boolean): List<UnplacedWidgetSpec> {
            return emptyList()
        }

        override fun newTransaction(): WorkspaceTransaction {
            return object : WorkspaceTransaction {
                override fun removeItem(target: RemoveItemParamsSpec): WorkspaceTransaction = this
                override suspend fun commit(): WorkspaceSpec = getWorkspace()
            }
        }
    }
}
