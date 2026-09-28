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

import android.content.ContentUris
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.BaseColumns
import android.util.Base64
import java.util.LinkedHashMap

/**
 * Settings and database column definitions for Sandboxr Launcher.
 */
object LauncherSettings {

    /**
     * Default authority for Sandboxr Launcher settings.
     */
    const val AUTHORITY = "com.sandboxr.launcher.settings"

    /**
     * Types of launch animations.
     */
    object Animation {
        const val DEFAULT = 0
        const val VIEW_BACKGROUND = 1
        const val DEFAULT_NO_ICON = 2
    }

    /**
     * Favorites table definitions and column constants.
     */
    object Favorites : BaseColumns {
        const val _ID = BaseColumns._ID
        const val _COUNT = BaseColumns._COUNT

        const val TABLE_NAME = "favorites"
        const val TMP_TABLE = "favorites_tmp"

        /** The time of the last update to this row. Type: INTEGER */
        const val MODIFIED = "modified"

        /** Descriptive name of the favorite item. Type: TEXT */
        const val TITLE = "title"

        /** Intent URI describing the target component/action. Type: TEXT */
        const val INTENT = "intent"

        /** The item type. Type: INTEGER */
        const val ITEM_TYPE = "itemType"

        const val ITEM_TYPE_NON_ACTIONABLE = -1
        const val ITEM_TYPE_APPLICATION = 0
        const val ITEM_TYPE_SHORTCUT = 1
        const val ITEM_TYPE_FOLDER = 2
        const val ITEM_TYPE_APPWIDGET = 4
        const val ITEM_TYPE_CUSTOM_APPWIDGET = 5
        const val ITEM_TYPE_DEEP_SHORTCUT = 6
        const val ITEM_TYPE_TASK = 7
        const val ITEM_TYPE_QSB = 8
        const val ITEM_TYPE_SEARCH_ACTION = 9
        const val ITEM_TYPE_APP_GROUP = 10
        const val ITEM_TYPE_PRIVATE_SPACE_INSTALL_APP_BUTTON = 11
        const val ITEM_TYPE_FILE_SYSTEM_FILE = 12
        const val ITEM_TYPE_FILE_SYSTEM_FOLDER = 13
        const val ITEM_TYPE_CUSTOM_VIEW = 14
        const val ITEM_TYPE_SYSTEM_DRAG = 15

        /** Custom icon bitmap BLOB. Type: BLOB */
        const val ICON = "icon"

        /** Container holding the favorite. Type: INTEGER */
        const val CONTAINER = "container"

        const val CONTAINER_DESKTOP = -100
        const val CONTAINER_HOTSEAT = -101
        const val CONTAINER_ALL_APPS_PREDICTION = -102
        const val CONTAINER_WIDGETS_PREDICTION = -111
        const val CONTAINER_HOTSEAT_PREDICTION = -103
        const val CONTAINER_ALL_APPS = -104
        const val CONTAINER_WIDGETS_TRAY = -105
        const val CONTAINER_BOTTOM_WIDGETS_TRAY = -112
        const val CONTAINER_PIN_WIDGETS = -113
        const val CONTAINER_WALLPAPERS = -114
        const val CONTAINER_SHORTCUTS = -107
        const val CONTAINER_SETTINGS = -108
        const val CONTAINER_TASKSWITCHER = -109
        const val CONTAINER_PRIVATESPACE = -110
        const val EXTENDED_CONTAINERS = -200
        const val CONTAINER_UNKNOWN = -1

        /** Screen holding the favorite (desktop page index or hotseat order). Type: INTEGER */
        const val SCREEN = "screen"

        /** The X cell position. Type: INTEGER */
        const val CELLX = "cellX"

        /** The Y cell position. Type: INTEGER */
        const val CELLY = "cellY"

        /** The horizontal cell span. Type: INTEGER */
        const val SPANX = "spanX"

        /** The vertical cell span. Type: INTEGER */
        const val SPANY = "spanY"

        /** User profile ID. Type: INTEGER */
        const val PROFILE_ID = "profileId"

        /** The appWidgetId of the widget. Type: INTEGER */
        const val APPWIDGET_ID = "appWidgetId"

        /** The ComponentName string of the widget provider. Type: TEXT */
        const val APPWIDGET_PROVIDER = "appWidgetProvider"

        /** Restored flag (0 = normal, 1 = restored/pending install). Type: INTEGER */
        const val RESTORED = "restored"

        /** Rank within an ordered container (folder or hotseat). Type: INTEGER */
        const val RANK = "rank"

        /** Flags and options for ItemInfo. Type: INTEGER */
        const val OPTIONS = "options"

        /** Source container that the widget was added from. Type: INTEGER */
        const val APPWIDGET_SOURCE = "appWidgetSource"

        /** The base content URI for favorites */
        @JvmField
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$TABLE_NAME")

        /** Returns the content URI for a specific item ID */
        @JvmStatic
        fun getContentUri(id: Long): Uri = ContentUris.withAppendedId(CONTENT_URI, id)

        /** Returns the content URI for a given custom authority */
        @JvmStatic
        fun getContentUri(authority: String, id: Long): Uri =
            Uri.parse("content://$authority/$TABLE_NAME/$id")

        @JvmStatic
        fun containerToString(container: Int): String = when (container) {
            CONTAINER_DESKTOP -> "desktop"
            CONTAINER_HOTSEAT -> "hotseat"
            CONTAINER_ALL_APPS_PREDICTION -> "prediction"
            CONTAINER_WIDGETS_PREDICTION -> "widgets_prediction"
            CONTAINER_HOTSEAT_PREDICTION -> "hotseat_prediction"
            CONTAINER_ALL_APPS -> "all_apps"
            CONTAINER_WIDGETS_TRAY -> "widgets_tray"
            CONTAINER_BOTTOM_WIDGETS_TRAY -> "bottom_widgets_tray"
            CONTAINER_PIN_WIDGETS -> "pin_widgets"
            CONTAINER_WALLPAPERS -> "wallpapers"
            CONTAINER_SHORTCUTS -> "shortcuts"
            CONTAINER_SETTINGS -> "settings"
            CONTAINER_TASKSWITCHER -> "taskswitcher"
            CONTAINER_PRIVATESPACE -> "privatespace"
            EXTENDED_CONTAINERS -> "extended_containers"
            CONTAINER_UNKNOWN -> "unknown"
            else -> container.toString()
        }

        @JvmStatic
        fun itemTypeToString(type: Int): String = when (type) {
            ITEM_TYPE_APPLICATION -> "APP"
            ITEM_TYPE_SHORTCUT -> "SHORTCUT"
            ITEM_TYPE_FOLDER -> "FOLDER"
            ITEM_TYPE_APPWIDGET -> "WIDGET"
            ITEM_TYPE_CUSTOM_APPWIDGET -> "CUSTOMWIDGET"
            ITEM_TYPE_DEEP_SHORTCUT -> "DEEPSHORTCUT"
            ITEM_TYPE_TASK -> "TASK"
            ITEM_TYPE_QSB -> "QSB"
            ITEM_TYPE_SEARCH_ACTION -> "SEARCH_ACTION"
            ITEM_TYPE_APP_GROUP -> "APP_PAIR"
            ITEM_TYPE_PRIVATE_SPACE_INSTALL_APP_BUTTON -> "PRIVATE_SPACE_INSTALL_APP_BUTTON"
            ITEM_TYPE_FILE_SYSTEM_FILE -> "FILE_SYSTEM_FILE"
            ITEM_TYPE_FILE_SYSTEM_FOLDER -> "FILE_SYSTEM_FOLDER"
            ITEM_TYPE_CUSTOM_VIEW -> "CUSTOM_VIEW"
            ITEM_TYPE_SYSTEM_DRAG -> "SYSTEM_DRAG"
            else -> type.toString()
        }

        @JvmStatic
        fun addTableToDb(db: SQLiteDatabase, myProfileId: Long, optional: Boolean) {
            addTableToDb(db, myProfileId, optional, TABLE_NAME)
        }

        @JvmStatic
        fun addTableToDb(
            db: SQLiteDatabase,
            myProfileId: Long,
            optional: Boolean,
            tableName: String
        ) {
            val ifNotExists = if (optional) "IF NOT EXISTS " else ""
            db.execSQL("CREATE TABLE $ifNotExists$tableName (${getJoinedColumnsToTypes(myProfileId)});")
        }

        @JvmStatic
        fun getColumnsToTypes(profileId: Long): LinkedHashMap<String, String> {
            val map = LinkedHashMap<String, String>()
            map[_ID] = "INTEGER PRIMARY KEY"
            map[TITLE] = "TEXT"
            map[INTENT] = "TEXT"
            map[CONTAINER] = "INTEGER"
            map[SCREEN] = "INTEGER"
            map[CELLX] = "INTEGER"
            map[CELLY] = "INTEGER"
            map[SPANX] = "INTEGER"
            map[SPANY] = "INTEGER"
            map[ITEM_TYPE] = "INTEGER"
            map[APPWIDGET_ID] = "INTEGER NOT NULL DEFAULT -1"
            map[ICON] = "BLOB"
            map[APPWIDGET_PROVIDER] = "TEXT"
            map[MODIFIED] = "INTEGER NOT NULL DEFAULT 0"
            map[RESTORED] = "INTEGER NOT NULL DEFAULT 0"
            map[PROFILE_ID] = "INTEGER DEFAULT $profileId"
            map[RANK] = "INTEGER NOT NULL DEFAULT 0"
            map[OPTIONS] = "INTEGER NOT NULL DEFAULT 0"
            map[APPWIDGET_SOURCE] = "INTEGER NOT NULL DEFAULT -1"
            return map
        }

        @JvmStatic
        fun getJoinedColumnsToTypes(profileId: Long): String {
            return getColumnsToTypes(profileId).entries.joinToString(", ") { "${it.key} ${it.value}" }
        }

        @JvmStatic
        fun getColumns(profileId: Long): String {
            return getColumnsToTypes(profileId).keys.joinToString(", ")
        }
    }

    /**
     * Workspace screens table definitions.
     */
    object WorkspaceScreens : BaseColumns {
        const val _ID = BaseColumns._ID
        const val TABLE_NAME = "workspaceScreens"

        /** Rank/order of the screen. Type: INTEGER */
        const val SCREEN_RANK = "screenRank"

        /** Time of modification. Type: INTEGER */
        const val MODIFIED = "modified"

        @JvmField
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$TABLE_NAME")

        @JvmStatic
        fun addTableToDb(db: SQLiteDatabase, optional: Boolean) {
            val ifNotExists = if (optional) "IF NOT EXISTS " else ""
            db.execSQL(
                "CREATE TABLE $ifNotExists$TABLE_NAME (" +
                    "$_ID INTEGER PRIMARY KEY, " +
                    "$SCREEN_RANK INTEGER, " +
                    "$MODIFIED INTEGER NOT NULL DEFAULT 0);"
            )
        }
    }

    /**
     * Settings related methods and constants.
     */
    object Settings {
        const val METHOD_CLEAR_EMPTY_DB_FLAG = "clear_empty_db_flag"
        const val METHOD_NEW_ITEM_ID = "generate_new_item_id"
        const val METHOD_NEW_SCREEN_ID = "generate_new_screen_id"
        const val METHOD_CREATE_EMPTY_DB = "create_empty_db"
        const val METHOD_LOAD_DEFAULT_FAVORITES = "load_default_favorites"
        const val METHOD_REMOVE_GHOST_WIDGETS = "remove_ghost_widgets"

        const val EXTRA_VALUE = "value"
        const val EXTRA_DB_NAME = "db_name"

        const val LAYOUT_PROVIDER_KEY = "launcher3.layout.provider"
        const val LAYOUT_DIGEST_LABEL = "launcher-layout"
        const val LAYOUT_DIGEST_TAG = "ignore"
        const val BLOB_KEY_PREFIX = "blob://"

        @JvmStatic
        fun createBlobProviderKey(digest: ByteArray): String {
            return BLOB_KEY_PREFIX + Base64.encodeToString(digest, Base64.NO_WRAP or Base64.NO_PADDING)
        }
    }

    /** Returns the authority string for the given context. */
    @JvmStatic
    fun getAuthority(context: Context): String = "${context.packageName}.settings"
}
