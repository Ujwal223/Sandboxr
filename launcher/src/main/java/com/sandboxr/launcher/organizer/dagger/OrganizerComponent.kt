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

package com.sandboxr.launcher.organizer.dagger

import com.sandboxr.launcher.organizer.OrganizerTransactionContext
import com.sandboxr.launcher.organizer.creation.screen.ui.foldercreator.FolderCreatorViewModel
import com.sandboxr.launcher.organizer.creation.screen.ui.spacecreator.SpaceCreatorViewModel
import com.sandboxr.launcher.organizer.creation.screen.ui.workspaceorganizer.WorkspaceOrganizerViewModel
import com.sandboxr.launcher.organizer.generator.CreationSession

/**
 * Component providing organizer engine and UI dependencies.
 */
interface OrganizerComponent {
    fun getFolderCreatorViewModel(): FolderCreatorViewModel
    fun getWorkspaceOrganizerViewModel(): WorkspaceOrganizerViewModel
    fun getSpaceCreatorViewModel(): SpaceCreatorViewModel
    fun getOrganizerTransactionContext(): OrganizerTransactionContext
    fun getCreationSessionFactory(): CreationSession.Factory
}
