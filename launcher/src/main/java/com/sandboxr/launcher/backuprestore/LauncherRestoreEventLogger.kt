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

package com.sandboxr.launcher.backuprestore

import android.content.Context
import android.util.Log

/**
 * Event logger for tracking workspace backup and restore metrics, item successes, and failures.
 */
open class LauncherRestoreEventLogger @javax.inject.Inject constructor() {

    @Retention(AnnotationRetention.SOURCE)
    annotation class RestoreError {
        companion object {
            const val PROFILE_DELETED = "user_profile_deleted"
            const val MISSING_WIDGET_PROVIDER = "missing_widget_provider"
            const val OVERLAPPING_ITEM = "overlapping_item"
            const val INVALID_WIDGET_SIZE = "invalid_widget_size"
            const val INVALID_WIDGET_CONTAINER = "invalid_widget_container"
            const val SHORTCUT_NOT_FOUND = "shortcut_not_found"
            const val APP_NO_TARGET_PACKAGE = "app_no_target_package"
            const val APP_NO_DB_INTENT = "app_no_db_intent"
            const val APP_NO_LAUNCH_INTENT = "app_no_launch_intent"
            const val APP_NOT_RESTORED_OR_INSTALLING = "app_not_restored_or_installed"
            const val APP_NOT_INSTALLED_EXTERNAL_MEDIA = "app_not_installed_external_media"
            const val WIDGETS_DISABLED = "widgets_disabled"
            const val PROFILE_NOT_RESTORED = "profile_not_restored"
            const val DATABASE_FILE_NOT_RESTORED = "db_file_not_restored"
            const val WIDGET_REMOVED = "widget_not_found"
            const val GRID_MIGRATION_FAILURE = "grid_migration_failed"
            const val NO_SEARCH_WIDGET = "no_search_widget"
            const val INVALID_WIDGET_ID = "invalid_widget_id"
            const val OTHER_WIDGET_INFLATION_FAIL = "other_widget_fail"
            const val UNSPECIFIED_WIDGET_INFLATION_RESULT = "unspecified_widget_inflation_result"
            const val UNRESTORED_PENDING_WIDGET = "unrestored_pending_widget"
            const val INVALID_CUSTOM_WIDGET_ID = "invalid_custom_widget_id"
            const val FILE_SYSTEM_ITEM_FROM_BACKUP = "file_system_item_from_backup"
            const val FILE_SYSTEM_ITEM_NO_LONGER_EXISTS = "file_system_item_no_longer_exists"
        }
    }

    private val restoredCounts = mutableMapOf<String, Int>()
    private val failedCounts = mutableMapOf<String, Int>()

    open fun logLauncherItemsRestoreFailed(dataType: String, count: Int, error: String?) {
        val current = failedCounts.getOrDefault(dataType, 0)
        failedCounts[dataType] = current + count
        Log.w(TAG, "Restore failed for $count items of type $dataType (error=$error)")
    }

    open fun logLauncherItemsRestored(dataType: String, count: Int) {
        val current = restoredCounts.getOrDefault(dataType, 0)
        restoredCounts[dataType] = current + count
        Log.i(TAG, "Successfully restored $count items of type $dataType")
    }

    open fun logSingleItemRestored(dataType: String) {
        logLauncherItemsRestored(dataType, 1)
    }

    open fun logSingleItemFailed(dataType: String, error: String?) {
        logLauncherItemsRestoreFailed(dataType, 1, error)
    }

    open fun logSingleFavoritesItemRestoreFailed(dataType: String, error: String?) {
        logSingleItemFailed(dataType, error)
    }

    open fun logSingleFavoritesItemRestoreFailed(itemType: Int, error: String?) {
        logSingleItemFailed(itemType.toString(), error)
    }

    open fun logSingleFavoritesItemRestored(dataType: String) {
        logSingleItemRestored(dataType)
    }

    open fun logSingleFavoritesItemRestored(itemType: Int) {
        logSingleItemRestored(itemType.toString())
    }

    open fun reportLauncherRestoreResults() {
        Log.i(TAG, "Restore complete. Restored: ${getTotalRestoredCount()}, Failed: ${getTotalFailedCount()}")
    }

    fun getTotalRestoredCount(): Int = restoredCounts.values.sum()

    fun getTotalFailedCount(): Int = failedCounts.values.sum()

    companion object {
        private const val TAG = "LauncherRestoreEvent"

        @JvmStatic
        fun newInstance(context: Context): LauncherRestoreEventLogger {
            return LauncherRestoreEventLogger()
        }
    }
}
