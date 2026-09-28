/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.sandboxr.launcher

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.sandboxr.launcher.LauncherSettings.Favorites
import com.sandboxr.launcher.LauncherSettings.Settings
import com.sandboxr.launcher.LauncherSettings.WorkspaceScreens
import com.sandboxr.launcher.model.DatabaseHelper
import com.sandboxr.launcher.provider.LauncherDbUtils.SQLiteTransaction
import java.io.FileDescriptor
import java.io.PrintWriter

/**
 * Primary ContentProvider serving home-screen workspace items, favorites, and screens for Sandboxr Launcher.
 */
open class LauncherProvider : ContentProvider() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var uriMatcher: UriMatcher
    private var authority: String = LauncherSettings.AUTHORITY

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        authority = LauncherSettings.getAuthority(ctx)
        uriMatcher = buildUriMatcher(authority)
        dbHelper = DatabaseHelper(ctx, LauncherFiles.LAUNCHER_DB)
        return true
    }

    @VisibleForTesting
    fun setDatabaseHelperForTesting(helper: DatabaseHelper) {
        this.dbHelper = helper
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            MATCH_FAVORITES -> "vnd.android.cursor.dir/favorite"
            MATCH_FAVORITES_ID -> "vnd.android.cursor.item/favorite"
            MATCH_WORKSPACE_SCREENS -> "vnd.android.cursor.dir/workspaceScreen"
            MATCH_WORKSPACE_SCREENS_ID -> "vnd.android.cursor.item/workspaceScreen"
            else -> null
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val match = uriMatcher.match(uri)
        val qb = SQLiteQueryBuilder()

        when (match) {
            MATCH_FAVORITES -> {
                qb.tables = Favorites.TABLE_NAME
            }
            MATCH_FAVORITES_ID -> {
                qb.tables = Favorites.TABLE_NAME
                qb.appendWhere("${Favorites._ID} = ${ContentUris.parseId(uri)}")
            }
            MATCH_WORKSPACE_SCREENS -> {
                qb.tables = WorkspaceScreens.TABLE_NAME
            }
            MATCH_WORKSPACE_SCREENS_ID -> {
                qb.tables = WorkspaceScreens.TABLE_NAME
                qb.appendWhere("${WorkspaceScreens._ID} = ${ContentUris.parseId(uri)}")
            }
            else -> {
                Log.w(TAG, "Unknown query URI: $uri")
                return null
            }
        }

        val db = dbHelper.readableDatabase
        val cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder)
        context?.contentResolver?.let { resolver ->
            cursor?.setNotificationUri(resolver, uri)
        }
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val vals = values?.let { ContentValues(it) } ?: ContentValues()
        val match = uriMatcher.match(uri)
        val db = dbHelper.writableDatabase

        val tableName = when (match) {
            MATCH_FAVORITES -> Favorites.TABLE_NAME
            MATCH_WORKSPACE_SCREENS -> WorkspaceScreens.TABLE_NAME
            else -> {
                Log.e(TAG, "Cannot insert into URI: $uri")
                return null
            }
        }

        if (tableName == Favorites.TABLE_NAME) {
            if (!vals.containsKey(Favorites._ID)) {
                vals.put(Favorites._ID, dbHelper.generateNewItemId())
            }
            if (!vals.containsKey(Favorites.MODIFIED)) {
                vals.put(Favorites.MODIFIED, System.currentTimeMillis())
            }
        } else if (tableName == WorkspaceScreens.TABLE_NAME) {
            if (!vals.containsKey(WorkspaceScreens._ID)) {
                vals.put(WorkspaceScreens._ID, dbHelper.generateNewScreenId())
            }
            if (!vals.containsKey(WorkspaceScreens.MODIFIED)) {
                vals.put(WorkspaceScreens.MODIFIED, System.currentTimeMillis())
            }
        }

        val rowId = db.insert(tableName, null, vals)
        if (rowId < 0) {
            Log.e(TAG, "Failed to insert row into $tableName: $vals")
            return null
        }

        val resultUri = ContentUris.withAppendedId(uri, rowId)
        notifyChange(uri)
        return resultUri
    }

    override fun bulkInsert(uri: Uri, values: Array<out ContentValues>): Int {
        val match = uriMatcher.match(uri)
        val tableName = when (match) {
            MATCH_FAVORITES -> Favorites.TABLE_NAME
            MATCH_WORKSPACE_SCREENS -> WorkspaceScreens.TABLE_NAME
            else -> return 0
        }

        val db = dbHelper.writableDatabase
        var count = 0
        SQLiteTransaction(db).use { tx ->
            for (value in values) {
                val vals = ContentValues(value)
                if (tableName == Favorites.TABLE_NAME) {
                    if (!vals.containsKey(Favorites._ID)) {
                        vals.put(Favorites._ID, dbHelper.generateNewItemId())
                    }
                    if (!vals.containsKey(Favorites.MODIFIED)) {
                        vals.put(Favorites.MODIFIED, System.currentTimeMillis())
                    }
                }
                val rowId = db.insert(tableName, null, vals)
                if (rowId >= 0) {
                    count++
                }
            }
            tx.commit()
        }

        if (count > 0) {
            notifyChange(uri)
        }
        return count
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        if (values == null || values.size() == 0) return 0
        val match = uriMatcher.match(uri)
        val db = dbHelper.writableDatabase

        val vals = ContentValues(values)
        if (!vals.containsKey(Favorites.MODIFIED)) {
            vals.put(Favorites.MODIFIED, System.currentTimeMillis())
        }

        val count = when (match) {
            MATCH_FAVORITES -> db.update(Favorites.TABLE_NAME, vals, selection, selectionArgs)
            MATCH_FAVORITES_ID -> {
                val id = ContentUris.parseId(uri)
                val where = "${Favorites._ID} = $id" + if (selection != null) " AND ($selection)" else ""
                db.update(Favorites.TABLE_NAME, vals, where, selectionArgs)
            }
            MATCH_WORKSPACE_SCREENS -> db.update(WorkspaceScreens.TABLE_NAME, vals, selection, selectionArgs)
            MATCH_WORKSPACE_SCREENS_ID -> {
                val id = ContentUris.parseId(uri)
                val where = "${WorkspaceScreens._ID} = $id" + if (selection != null) " AND ($selection)" else ""
                db.update(WorkspaceScreens.TABLE_NAME, vals, where, selectionArgs)
            }
            else -> 0
        }

        if (count > 0) {
            notifyChange(uri)
        }
        return count
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val match = uriMatcher.match(uri)
        val db = dbHelper.writableDatabase

        val count = when (match) {
            MATCH_FAVORITES -> db.delete(Favorites.TABLE_NAME, selection, selectionArgs)
            MATCH_FAVORITES_ID -> {
                val id = ContentUris.parseId(uri)
                val where = "${Favorites._ID} = $id" + if (selection != null) " AND ($selection)" else ""
                db.delete(Favorites.TABLE_NAME, where, selectionArgs)
            }
            MATCH_WORKSPACE_SCREENS -> db.delete(WorkspaceScreens.TABLE_NAME, selection, selectionArgs)
            MATCH_WORKSPACE_SCREENS_ID -> {
                val id = ContentUris.parseId(uri)
                val where = "${WorkspaceScreens._ID} = $id" + if (selection != null) " AND ($selection)" else ""
                db.delete(WorkspaceScreens.TABLE_NAME, where, selectionArgs)
            }
            else -> 0
        }

        if (count > 0) {
            notifyChange(uri)
        }
        return count
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val callingIdentity = Binder.clearCallingIdentity()
        try {
            when (method) {
                Settings.METHOD_NEW_ITEM_ID -> {
                    return Bundle().apply {
                        putLong(Settings.EXTRA_VALUE, dbHelper.generateNewItemId().toLong())
                    }
                }
                Settings.METHOD_NEW_SCREEN_ID -> {
                    return Bundle().apply {
                        putLong(Settings.EXTRA_VALUE, dbHelper.generateNewScreenId())
                    }
                }
                Settings.METHOD_CREATE_EMPTY_DB -> {
                    dbHelper.writableDatabase
                    return Bundle().apply {
                        putBoolean(Settings.EXTRA_VALUE, true)
                    }
                }
                Settings.METHOD_CLEAR_EMPTY_DB_FLAG -> {
                    return Bundle().apply {
                        putBoolean(Settings.EXTRA_VALUE, true)
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingIdentity)
        }
        return null
    }

    override fun dump(fd: FileDescriptor, writer: PrintWriter, args: Array<out String>) {
        writer.println("LauncherProvider:")
        writer.println("  Authority: $authority")
        writer.println("  Database: ${dbHelper.dbName}")
        try {
            val db = dbHelper.readableDatabase
            var favCount = 0L
            var screenCount = 0L
            db.rawQuery("SELECT count(*) FROM ${Favorites.TABLE_NAME}", null).use {
                if (it.moveToFirst()) favCount = it.getLong(0)
            }
            db.rawQuery("SELECT count(*) FROM ${WorkspaceScreens.TABLE_NAME}", null).use {
                if (it.moveToFirst()) screenCount = it.getLong(0)
            }
            writer.println("  Favorites rows: $favCount")
            writer.println("  WorkspaceScreens rows: $screenCount")
        } catch (e: Exception) {
            writer.println("  Error querying database: ${e.message}")
        }
    }

    private fun notifyChange(uri: Uri) {
        context?.contentResolver?.notifyChange(uri, null)
    }

    companion object {
        private const val TAG = "LauncherProvider"

        private const val MATCH_FAVORITES = 1
        private const val MATCH_FAVORITES_ID = 2
        private const val MATCH_WORKSPACE_SCREENS = 3
        private const val MATCH_WORKSPACE_SCREENS_ID = 4

        @JvmStatic
        fun buildUriMatcher(authority: String): UriMatcher {
            val matcher = UriMatcher(UriMatcher.NO_MATCH)
            matcher.addURI(authority, Favorites.TABLE_NAME, MATCH_FAVORITES)
            matcher.addURI(authority, "${Favorites.TABLE_NAME}/#", MATCH_FAVORITES_ID)
            matcher.addURI(authority, WorkspaceScreens.TABLE_NAME, MATCH_WORKSPACE_SCREENS)
            matcher.addURI(authority, "${WorkspaceScreens.TABLE_NAME}/#", MATCH_WORKSPACE_SCREENS_ID)
            return matcher
        }
    }
}
