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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import java.util.concurrent.CompletableFuture

/**
 * Interface providing file thumbnails and MIME-type iconography.
 */
interface HomeScreenFilesIconProvider {
    fun getIcon(context: Context, file: HomeScreenFile): Drawable?
    fun loadThumbnail(context: Context, file: HomeScreenFile, sizePx: Int): CompletableFuture<Bitmap?>
}

/**
 * Core interface for managing file items pinned to the launcher homescreen.
 */
interface HomeScreenFilesProvider {
    val iconProvider: HomeScreenFilesIconProvider

    fun onReady(): CompletableFuture<Void>

    fun canCreateNewFolder(): Boolean

    fun createNewFolder(extras: HomeScreenFilesUpdate.Extras): CompletableFuture<Boolean>

    fun canMoveToHomeScreen(uriList: List<Uri>?): Boolean

    fun renameFile(uri: Uri, newName: String): CompletableFuture<Boolean>

    fun deleteFile(uri: Uri): CompletableFuture<Boolean>
}
