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
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import com.sandboxr.launcher.LauncherSettings
import java.util.concurrent.CompletableFuture

/**
 * Utility methods for managing and launching file items on the homescreen.
 */
object HomeScreenFilesUtils {

    const val LAUNCH_INTENT_DEFAULT_FLAGS =
        Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    val isFeatureEnabled: Boolean
        get() = try {
            Environment.isExternalStorageManager()
        } catch (_: Throwable) {
            false
        }

    fun buildItemType(homeScreenFile: HomeScreenFile): Int {
        return if (homeScreenFile.isDirectory) {
            LauncherSettings.Favorites.ITEM_TYPE_FOLDER
        } else {
            LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
        }
    }

    fun buildLaunchIntent(uri: Uri, mimeType: String? = null): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType ?: "*/*")
            flags = LAUNCH_INTENT_DEFAULT_FLAGS
        }
    }
}

/**
 * Fallback no-op implementation of [HomeScreenFilesProvider].
 */
class HomeScreenFilesNoOpProvider : HomeScreenFilesProvider {

    override val iconProvider: HomeScreenFilesIconProvider = object : HomeScreenFilesIconProvider {
        override fun getIcon(context: Context, file: HomeScreenFile): Drawable? = null
        override fun loadThumbnail(context: Context, file: HomeScreenFile, sizePx: Int): CompletableFuture<Bitmap?> {
            return CompletableFuture.completedFuture(null)
        }
    }

    override fun onReady(): CompletableFuture<Void> = CompletableFuture.completedFuture(null)

    override fun canCreateNewFolder(): Boolean = false

    override fun createNewFolder(extras: HomeScreenFilesUpdate.Extras): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(false)
    }

    override fun canMoveToHomeScreen(uriList: List<Uri>?): Boolean = false

    override fun renameFile(uri: Uri, newName: String): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(false)
    }

    override fun deleteFile(uri: Uri): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(false)
    }
}
