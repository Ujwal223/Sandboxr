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

package com.sandboxr.launcher.appfunctions.workspace

/**
 * Defines a contract for querying and modifying the workspace.
 */
interface WorkspaceRepository {

    /**
     * Retrieves the current state of the workspace as a flat, serializable spec.
     */
    suspend fun getWorkspace(): WorkspaceSpec

    /**
     * Lists all installed apps on the device.
     */
    suspend fun getInstalledApps(orderByUsageStats: Boolean): List<UnplacedAppSpec>

    /**
     * Lists all installed widgets on the device.
     */
    suspend fun getInstalledWidgets(orderByUsageStats: Boolean): List<UnplacedWidgetSpec>

    /**
     * Starts a new atomic transaction for modifying the workspace.
     */
    fun newTransaction(): WorkspaceTransaction
}

/**
 * Represents a single atomic unit of work for workspace mutation.
 */
interface WorkspaceTransaction {

    fun removeItem(target: RemoveItemParamsSpec): WorkspaceTransaction

    suspend fun commit(): WorkspaceSpec
}
