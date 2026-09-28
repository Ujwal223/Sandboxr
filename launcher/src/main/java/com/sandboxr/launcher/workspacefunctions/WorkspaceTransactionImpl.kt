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
import com.sandboxr.launcher.appfunctions.workspace.WorkspaceRepository
import com.sandboxr.launcher.appfunctions.workspace.WorkspaceSpec
import com.sandboxr.launcher.appfunctions.workspace.WorkspaceTransaction
import javax.inject.Inject

/**
 * Concrete implementation of [WorkspaceTransaction] for mutating workspace layout.
 */
class WorkspaceTransactionImpl @Inject constructor(
    private val repository: WorkspaceRepository
) : WorkspaceTransaction {

    private val mPendingRemovals = mutableListOf<RemoveItemParamsSpec>()

    override fun removeItem(target: RemoveItemParamsSpec): WorkspaceTransaction {
        mPendingRemovals.add(target)
        return this
    }

    override suspend fun commit(): WorkspaceSpec {
        // Commits changes and returns latest snapshot
        return repository.getWorkspace()
    }
}
