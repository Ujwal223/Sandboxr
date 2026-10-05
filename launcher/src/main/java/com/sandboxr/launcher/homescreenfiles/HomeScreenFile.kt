/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.sandboxr.launcher.homescreenfiles

import android.net.Uri
import android.os.UserHandle
import java.io.Serializable

/**
 * Represents a single file or folder item pinned or placed on the launcher homescreen.
 * Includes optional environment isolation metadata [envId] for Sandboxr containers.
 */
data class HomeScreenFile(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val isDirectory: Boolean,
    val user: UserHandle,
    val envId: String? = null
) : Serializable

/**
 * Encapsulates an atomic file update event on the homescreen.
 */
sealed class HomeScreenFilesUpdate {
    data class Added(val files: List<HomeScreenFile>) : HomeScreenFilesUpdate()
    data class Removed(val uris: List<Uri>) : HomeScreenFilesUpdate()
    data class Changed(val files: List<HomeScreenFile>) : HomeScreenFilesUpdate()
    data class Renamed(val oldUri: Uri, val newFile: HomeScreenFile) : HomeScreenFilesUpdate()

    data class Extras(
        val targetUri: Uri? = null,
        val newName: String? = null,
        val envId: String? = null
    )
}
