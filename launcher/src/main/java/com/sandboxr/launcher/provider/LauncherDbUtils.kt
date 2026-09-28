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

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.sandboxr.launcher.LauncherSettings.Favorites
import com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER
import com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_DESKTOP
import com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_HOTSEAT
import com.sandboxr.launcher.LauncherSettings.Favorites.SCREEN
import com.sandboxr.launcher.LauncherSettings.Favorites.TABLE_NAME
import com.sandboxr.launcher.LauncherSettings.Favorites._ID

/**
 * Database utility methods for Sandboxr Launcher database queries, transactions, and migrations.
 */
object LauncherDbUtils {

    const val TAG = "LauncherDbUtils"

    /**
     * Returns a SQL WHERE clause matching a single item ID.
     */
    @JvmStatic
    fun itemIdMatch(itemId: Long): String = "$_ID = $itemId"

    @JvmStatic
    fun itemIdMatch(itemId: Int): String = "$_ID = $itemId"

    /**
     * Returns a SQL WHERE clause matching items on the given workspace screens or hotseat.
     */
    @JvmStatic
    fun selectionForWorkspaceScreen(vararg screens: Int): String {
        val screenList = screens.joinToString(", ")
        return "$SCREEN IN ($screenList) OR $CONTAINER = $CONTAINER_HOTSEAT OR " +
            "$CONTAINER IN (SELECT $_ID FROM $TABLE_NAME WHERE $SCREEN IN ($screenList) OR $CONTAINER = $CONTAINER_HOTSEAT)"
    }

    /**
     * Queries a single column as an IntArray.
     */
    @JvmStatic
    fun queryIntArray(
        distinct: Boolean,
        db: SQLiteDatabase,
        tableName: String,
        columnName: String,
        selection: String? = null,
        groupBy: String? = null,
        orderBy: String? = null,
    ): IntArray {
        val list = mutableListOf<Int>()
        db.query(
            distinct,
            tableName,
            arrayOf(columnName),
            selection,
            null,
            groupBy,
            null,
            orderBy,
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.getInt(0))
            }
        }
        return list.toIntArray()
    }

    /**
     * Checks if a table exists in the given database.
     */
    @JvmStatic
    fun tableExists(db: SQLiteDatabase, tableName: String): Boolean {
        return db.query(
            /* distinct = */ true,
            /* table = */ "sqlite_master",
            /* columns = */ arrayOf("tbl_name"),
            /* selection = */ "tbl_name = ?",
            /* selectionArgs = */ arrayOf(tableName),
            null,
            null,
            null,
            null,
        ).use { it.count > 0 }
    }

    /**
     * Drops the specified table if it exists.
     */
    @JvmStatic
    fun dropTable(db: SQLiteDatabase, tableName: String) {
        db.execSQL("DROP TABLE IF EXISTS $tableName")
    }

    /**
     * Copies data from [fromTable] to [toTable].
     */
    @JvmStatic
    fun copyTable(
        fromDb: SQLiteDatabase,
        fromTable: String,
        toDb: SQLiteDatabase,
        toTable: String,
        profileId: Long = 0L,
    ) {
        dropTable(toDb, toTable)
        Favorites.addTableToDb(toDb, profileId, optional = false, tableName = toTable)
        val columns = Favorites.getColumns(profileId)
        if (fromDb != toDb) {
            toDb.execSQL("ATTACH DATABASE '${fromDb.path}' AS from_db")
            toDb.execSQL("INSERT INTO $toTable SELECT $columns FROM from_db.$fromTable")
            toDb.execSQL("DETACH DATABASE 'from_db'")
        } else {
            toDb.execSQL("INSERT INTO $toTable SELECT $columns FROM $fromTable")
        }
    }

    @JvmStatic
    fun copyTable(
        fromDb: SQLiteDatabase,
        fromTable: String,
        toDb: SQLiteDatabase,
        toTable: String,
        context: Context,
    ) {
        val userCache = com.sandboxr.launcher.pm.UserCache.getInstance(context)
        val profileId = userCache.getSerialNumberForUser(android.os.Process.myUserHandle())
        copyTable(fromDb, fromTable, toDb, toTable, profileId)
    }

    /**
     * Shifts desktop cells by [x] cells in Y direction.
     */
    @JvmStatic
    fun shiftWorkspaceByXCells(db: SQLiteDatabase, x: Int, toTable: String = TABLE_NAME) {
        db.execSQL("UPDATE $toTable SET ${Favorites.CELLY} = ${Favorites.CELLY} + $x WHERE $CONTAINER = $CONTAINER_DESKTOP")
    }

    /**
     * Lightweight RAII transaction manager.
     */
    class SQLiteTransaction(val db: SQLiteDatabase) : java.io.Closeable, AutoCloseable {
        init {
            db.beginTransaction()
        }

        fun commit() = db.setTransactionSuccessful()

        override fun close() = db.endTransaction()
    }

    /**
     * Extension to iterate over a Cursor sequentially.
     */
    fun Cursor.asSequence(): Sequence<Cursor> = generateSequence {
        if (moveToNext()) this else null
    }
}
