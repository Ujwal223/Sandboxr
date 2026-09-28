/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.sandboxr.launcher.model

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Process
import android.util.Log
import com.sandboxr.launcher.LauncherFiles
import com.sandboxr.launcher.LauncherSettings.Favorites
import com.sandboxr.launcher.LauncherSettings.WorkspaceScreens
import com.sandboxr.launcher.provider.LauncherDbUtils
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * SQLite database helper for the Sandboxr Launcher home-screen favorites and workspace model.
 */
open class DatabaseHelper @JvmOverloads constructor(
    private val context: Context,
    val dbName: String = LauncherFiles.LAUNCHER_DB,
    private val onEmptyDbCreateCallback: Runnable? = null,
) : SQLiteOpenHelper(context, dbName, null, SCHEMA_VERSION),
    com.android.launcher3.AutoInstallsLayout.LayoutParserCallback {

    private val maxItemId = AtomicLong(-1L)
    private val maxScreenId = AtomicLong(-1L)

    override fun onCreate(db: SQLiteDatabase) {
        if (LOGD) Log.d(TAG, "Creating new launcher database: $dbName at version $SCHEMA_VERSION")
        val myProfileId = getDefaultProfileId()
        Favorites.addTableToDb(db, myProfileId, optional = false)
        WorkspaceScreens.addTableToDb(db, optional = false)

        initIds(db)
        onEmptyDbCreateCallback?.run()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (LOGD) Log.d(TAG, "onUpgrade: $oldVersion -> $newVersion on $dbName")
        // Progressive migrations across schema changes
        if (oldVersion < 30) {
            // Recreate if schema is very old
            LauncherDbUtils.dropTable(db, Favorites.TABLE_NAME)
            LauncherDbUtils.dropTable(db, WorkspaceScreens.TABLE_NAME)
            onCreate(db)
            return
        }

        if (oldVersion < 32) {
            if (!hasColumn(db, Favorites.TABLE_NAME, Favorites.APPWIDGET_SOURCE)) {
                db.execSQL("ALTER TABLE ${Favorites.TABLE_NAME} ADD COLUMN ${Favorites.APPWIDGET_SOURCE} INTEGER NOT NULL DEFAULT -1")
            }
        }

        if (oldVersion < 34) {
            if (!hasColumn(db, Favorites.TABLE_NAME, Favorites.OPTIONS)) {
                db.execSQL("ALTER TABLE ${Favorites.TABLE_NAME} ADD COLUMN ${Favorites.OPTIONS} INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "onDowngrade: $oldVersion -> $newVersion on $dbName; preserving compatible columns")
        // If downgrade occurs, ensure required tables and columns exist without deleting user data
        val myProfileId = getDefaultProfileId()
        Favorites.addTableToDb(db, myProfileId, optional = true)
        WorkspaceScreens.addTableToDb(db, optional = true)
    }

    /**
     * Initializes the max ID counters from the database.
     */
    @Synchronized
    fun initIds(db: SQLiteDatabase = writableDatabase) {
        if (maxItemId.get() == -1L) {
            maxItemId.set(initializeMaxItemId(db))
        }
        if (maxScreenId.get() == -1L) {
            maxScreenId.set(initializeMaxScreenId(db))
        }
    }

    /**
     * Generates a new unique favorite item ID as a Long.
     */
    fun generateNewItemIdLong(): Long {
        if (maxItemId.get() == -1L) {
            initIds()
        }
        return maxItemId.incrementAndGet()
    }

    /**
     * Generates a new unique favorite item ID.
     */
    override fun generateNewItemId(): Int {
        return generateNewItemIdLong().toInt()
    }

    /**
     * Generates a new unique workspace screen ID.
     */
    fun generateNewScreenId(): Long {
        if (maxScreenId.get() == -1L) {
            initIds()
        }
        return maxScreenId.incrementAndGet()
    }

    val newScreenId: Int
        get() = generateNewScreenId().toInt()

    override fun insertAndCheck(db: SQLiteDatabase, values: android.content.ContentValues): Int {
        if (!values.containsKey(Favorites._ID)) {
            throw RuntimeException("Error: attempting to add item without specifying an id")
        }
        val id = values.getAsInteger(Favorites._ID)
        updateMaxId(id)
        return db.insert(Favorites.TABLE_NAME, null, values).toInt()
    }

    fun updateMaxId(id: Int) {
        maxItemId.accumulateAndGet(id.toLong(), ::max)
    }

    fun createEmptyDB(db: SQLiteDatabase) {
        LauncherDbUtils.dropTable(db, Favorites.TABLE_NAME)
        LauncherDbUtils.dropTable(db, WorkspaceScreens.TABLE_NAME)
        onCreate(db)
    }

    fun removeGhostWidgets(db: SQLiteDatabase) {
        // No-op ghost widget cleanup
    }

    fun newLauncherWidgetHolder(): com.android.launcher3.widget.LauncherWidgetHolder {
        return com.android.launcher3.widget.LauncherWidgetHolder.newInstance(context)
    }

    fun loadFavorites(db: SQLiteDatabase, loader: Any): Int {
        return try {
            val method = loader.javaClass.getMethod("loadLayout", SQLiteDatabase::class.java)
            val result = method.invoke(loader, db) as? Int ?: 0
            maxItemId.set(initializeMaxItemId(db))
            result
        } catch (e: Exception) {
            0
        }
    }

    private fun initializeMaxItemId(db: SQLiteDatabase): Long {
        return queryMaxId(db, Favorites.TABLE_NAME, Favorites._ID)
    }

    private fun initializeMaxScreenId(db: SQLiteDatabase): Long {
        return queryMaxId(db, WorkspaceScreens.TABLE_NAME, WorkspaceScreens._ID)
    }

    private fun queryMaxId(db: SQLiteDatabase, tableName: String, idColumn: String): Long {
        var id = 0L
        if (!LauncherDbUtils.tableExists(db, tableName)) return 0L
        try {
            db.rawQuery("SELECT MAX($idColumn) FROM $tableName", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    id = cursor.getLong(0)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query max ID from $tableName", e)
        }
        return max(0L, id)
    }

    private fun hasColumn(db: SQLiteDatabase, tableName: String, columnName: String): Boolean {
        return try {
            db.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (columnName.equals(cursor.getString(nameIndex), ignoreCase = true)) {
                        return true
                    }
                }
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    protected open fun getDefaultProfileId(): Long = 0L

    companion object {
        const val SCHEMA_VERSION = 34
        private const val TAG = "DatabaseHelper"
        private const val LOGD = false
    }
}
