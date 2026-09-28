/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.sandboxr.launcher.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.sandboxr.launcher.backuprestore.LauncherRestoreEventLogger
import com.sandboxr.launcher.LauncherSettings.Favorites
import com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER
import com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_DESKTOP
import com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_HOTSEAT
import com.sandboxr.launcher.LauncherSettings.Favorites.PROFILE_ID
import com.sandboxr.launcher.LauncherSettings.Favorites.RESTORED
import com.sandboxr.launcher.LauncherSettings.Favorites.SCREEN
import com.sandboxr.launcher.LauncherSettings.Favorites.TABLE_NAME
import com.sandboxr.launcher.LauncherSettings.Favorites._ID
import com.sandboxr.launcher.provider.LauncherDbUtils.SQLiteTransaction

/**
 * Utility task to sanitize and adapt the launcher database schema after restore or migration.
 */
class RestoreDbTask {

    /**
     * Sanitizes the database after restore:
     * 1. Re-maps profile IDs from the previous device/snapshot to [myProfileId].
     * 2. Flags all items with restored = 1.
     * 3. Consolidates screen indexes to eliminate gaps.
     *
     * @return Number of sanitized items in the database.
     */
    fun sanitizeDB(
        context: Context,
        db: SQLiteDatabase,
        myProfileId: Long = 0L,
    ): Int {
        if (!LauncherDbUtils.tableExists(db, TABLE_NAME)) return 0

        var sanitizedCount = 0
        try {
            SQLiteTransaction(db).use { tx ->
                val oldProfileId = getDefaultProfileId(db)
                if (oldProfileId != myProfileId) {
                    val profileUpdate = ContentValues().apply {
                        put(PROFILE_ID, myProfileId)
                    }
                    db.update(TABLE_NAME, profileUpdate, "$PROFILE_ID = ?", arrayOf(oldProfileId.toString()))
                }

                // Mark all items as restored
                val restoreUpdate = ContentValues().apply {
                    put(RESTORED, 1)
                }
                sanitizedCount = db.update(TABLE_NAME, restoreUpdate, null, null)

                // Remove gaps in screen IDs for desktop container
                removeScreenGaps(db)

                tx.commit()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sanitize database", e)
        }
        return sanitizedCount
    }

    /**
     * Finds the primary profile ID stored in the existing database.
     */
    fun getDefaultProfileId(db: SQLiteDatabase): Long {
        var profileId = 0L
        try {
            db.rawQuery(
                "SELECT $PROFILE_ID, count(*) AS c FROM $TABLE_NAME WHERE $CONTAINER IN ($CONTAINER_DESKTOP, $CONTAINER_HOTSEAT) GROUP BY $PROFILE_ID ORDER BY c DESC LIMIT 1",
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    profileId = cursor.getLong(0)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query default profile id", e)
        }
        return profileId
    }

    /**
     * Returns a list of all distinct profile IDs in the database.
     */
    fun getProfileIds(db: SQLiteDatabase): List<Long> {
        val list = mutableListOf<Long>()
        try {
            db.rawQuery("SELECT DISTINCT $PROFILE_ID FROM $TABLE_NAME", null).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(cursor.getLong(0))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query profile ids", e)
        }
        return list
    }

    /**
     * Re-indexes workspace screens to ensure contiguous 0-based page IDs.
     */
    fun removeScreenGaps(db: SQLiteDatabase) {
        try {
            val screens = mutableListOf<Int>()
            db.rawQuery(
                "SELECT DISTINCT $SCREEN FROM $TABLE_NAME WHERE $CONTAINER = $CONTAINER_DESKTOP ORDER BY $SCREEN ASC",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    screens.add(cursor.getInt(0))
                }
            }

            screens.forEachIndexed { newIndex, oldScreen ->
                if (newIndex != oldScreen) {
                    val update = ContentValues().apply {
                        put(SCREEN, newIndex)
                    }
                    db.update(
                        TABLE_NAME,
                        update,
                        "$CONTAINER = $CONTAINER_DESKTOP AND $SCREEN = ?",
                        arrayOf(oldScreen.toString())
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove screen gaps", e)
        }
    }

    companion object {
        private const val TAG = "RestoreDbTask"
        const val PREF_RESTORE_TASK_PENDING = "restore_task_pending"
        const val FIRST_LOAD_AFTER_RESTORE_KEY = "first_load_after_restore"
        const val RESTORED_DEVICE_TYPE = "restored_device_type"
        const val APPWIDGET_IDS = "appwidget_ids"
        const val APPWIDGET_OLD_IDS = "appwidget_old_ids"

        @JvmStatic
        fun setPending(context: Context, isPending: Boolean = true) {
            val prefs = context.getSharedPreferences(
                com.sandboxr.launcher.LauncherFiles.SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE
            )
            prefs.edit().putBoolean(PREF_RESTORE_TASK_PENDING, isPending).apply()
        }

        @JvmStatic
        fun isPending(context: Context): Boolean {
            val prefs = context.getSharedPreferences(
                com.sandboxr.launcher.LauncherFiles.SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE
            )
            return prefs.getBoolean(PREF_RESTORE_TASK_PENDING, false)
        }

        @JvmStatic
        fun performRestoreSanitization(context: Context, db: SQLiteDatabase, myProfileId: Long = 0L): Int {
            return RestoreDbTask().sanitizeDB(context, db, myProfileId)
        }

        @JvmStatic
        fun createRestoreTask(context: Context): java.util.function.Consumer<com.sandboxr.launcher.model.ModelDbController> {
            return java.util.function.Consumer { controller -> }
        }

        @JvmStatic
        fun sendMetricsForFailedMigration(context: Context, error: String) {
            Log.w(TAG, "sendMetricsForFailedMigration: $error")
        }

        fun LauncherRestoreEventLogger.sendMetricsForFailedMigration(
            table: com.sandboxr.launcher.util.SQLiteTable,
            error: String,
            selection: String? = null,
        ) {
            try {
                table.query(arrayOf(Favorites.ITEM_TYPE), selection).use { cursor ->
                    val counts = mutableMapOf<Int, Int>()
                    while (cursor.moveToNext()) {
                        val type = cursor.getInt(0)
                        counts[type] = counts.getOrDefault(type, 0) + 1
                    }
                    counts.forEach { (type, count) ->
                        logLauncherItemsRestoreFailed("favorite_type_$type", count, error)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendMetricsForFailedMigration: Error reading from database", e)
            }
        }
    }
}
