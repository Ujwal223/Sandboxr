/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.sandboxr.launcher.apppairs

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherSettings
import com.sandboxr.launcher.R
import com.sandboxr.launcher.model.IModelWriter
import com.sandboxr.launcher.model.data.AppPairInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo

/**
 * Controller orchestrating App Pair lifecycle:
 * - Validating pair candidates
 * - Creating and configuring [AppPairInfo] instances
 * - Launching both apps simultaneously in split-screen mode
 * - Persisting app pairs to the home screen database
 */
class AppPairsController(private val context: Context) {

    companion object {
        private const val TAG = "AppPairsController"

        /**
         * Validates if two items can form a valid app pair.
         */
        @JvmStatic
        fun canCreateAppPair(item1: ItemInfo?, item2: ItemInfo?): Boolean {
            if (item1 == null || item2 == null) return false
            if (item1 === item2) return false
            if (item1 is AppPairInfo || item2 is AppPairInfo) return false
            return item1 is WorkspaceItemInfo && item2 is WorkspaceItemInfo
        }

        /**
         * Creates an [AppPairInfo] from two [WorkspaceItemInfo] instances.
         */
        @JvmStatic
        fun createAppPair(app1: WorkspaceItemInfo, app2: WorkspaceItemInfo, title: CharSequence? = null): AppPairInfo {
            val appPairInfo = AppPairInfo()
            app1.rank = 0
            app2.rank = 1
            appPairInfo.add(app1)
            appPairInfo.add(app2)
            if (title != null) {
                appPairInfo.title = title
            }
            return appPairInfo
        }
    }

    /**
     * Launches both apps of an [AppPairInfo] into split-screen mode.
     */
    fun launchAppPair(appPair: AppPairInfo): Boolean {
        val contents = appPair.getAppContents()
        if (contents.size < 2) {
            Log.e(TAG, "Cannot launch app pair with fewer than 2 items")
            return false
        }

        val app1 = contents[0]
        val app2 = contents[1]

        val rawIntent1 = app1.intent ?: app1.getIntent()
        val rawIntent2 = app2.intent ?: app2.getIntent()

        if (rawIntent1 == null || rawIntent2 == null) {
            Log.e(TAG, "Missing launch intent for app pair members")
            return false
        }

        val intent1 = Intent(rawIntent1)
        val intent2 = Intent(rawIntent2)

        try {
            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)

            val options1 = ActivityOptions.makeBasic()
            val options2 = ActivityOptions.makeBasic()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Multi-window split screen launch flags
                intent2.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }

            context.startActivity(intent1, options1.toBundle())
            context.startActivity(intent2, options2.toBundle())
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app pair in split screen", e)
            runCatching {
                Toast.makeText(context, R.string.app_pair_launch_error, Toast.LENGTH_SHORT).show()
            }
            return false
        }
    }

    /**
     * Saves an [AppPairInfo] to the launcher database at the designated workspace position.
     */
    fun saveAppPairToWorkspace(
        appPair: AppPairInfo,
        screenId: Int,
        cellX: Int,
        cellY: Int,
        modelWriter: IModelWriter?
    ): Boolean {
        appPair.container = LauncherSettings.Favorites.CONTAINER_DESKTOP
        appPair.screenId = screenId
        appPair.cellX = cellX
        appPair.cellY = cellY
        appPair.spanX = 1
        appPair.spanY = 1

        if (modelWriter != null) {
            modelWriter.addItemToDatabase(appPair, appPair.container, screenId, cellX, cellY)
            runCatching {
                Toast.makeText(context, R.string.app_pair_save_toast, Toast.LENGTH_SHORT).show()
            }
            return true
        }
        return false
    }
}
