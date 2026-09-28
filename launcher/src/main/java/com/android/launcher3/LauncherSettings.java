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

package com.android.launcher3;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.LinkedHashMap;

/**
 * AOSP Java Compatibility Bridge delegating to {@link com.sandboxr.launcher.LauncherSettings}.
 */
public class LauncherSettings {

    public static final String AUTHORITY = com.sandboxr.launcher.LauncherSettings.AUTHORITY;

    public static final class Animation {
        public static final int DEFAULT = com.sandboxr.launcher.LauncherSettings.Animation.DEFAULT;
        public static final int VIEW_BACKGROUND = com.sandboxr.launcher.LauncherSettings.Animation.VIEW_BACKGROUND;
        public static final int DEFAULT_NO_ICON = com.sandboxr.launcher.LauncherSettings.Animation.DEFAULT_NO_ICON;
    }

    public static final class Favorites implements BaseColumns {
        public static final com.sandboxr.launcher.icons.cache.CacheLookupFlag DESKTOP_ICON_FLAG =
                com.sandboxr.launcher.icons.cache.CacheLookupFlag.DEFAULT_LOOKUP_FLAG.withThemeIcon();

        public static final String TABLE_NAME = com.sandboxr.launcher.LauncherSettings.Favorites.TABLE_NAME;
        public static final String TMP_TABLE = com.sandboxr.launcher.LauncherSettings.Favorites.TMP_TABLE;

        public static final String MODIFIED = com.sandboxr.launcher.LauncherSettings.Favorites.MODIFIED;
        public static final String TITLE = com.sandboxr.launcher.LauncherSettings.Favorites.TITLE;
        public static final String INTENT = com.sandboxr.launcher.LauncherSettings.Favorites.INTENT;
        public static final String ITEM_TYPE = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE;

        public static final int ITEM_TYPE_NON_ACTIONABLE = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_NON_ACTIONABLE;
        public static final int ITEM_TYPE_APPLICATION = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
        public static final int ITEM_TYPE_SHORTCUT = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
        public static final int ITEM_TYPE_FOLDER = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_FOLDER;
        public static final int ITEM_TYPE_APPWIDGET = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET;
        public static final int ITEM_TYPE_CUSTOM_APPWIDGET = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET;
        public static final int ITEM_TYPE_DEEP_SHORTCUT = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT;
        public static final int ITEM_TYPE_TASK = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_TASK;
        public static final int ITEM_TYPE_QSB = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_QSB;
        public static final int ITEM_TYPE_SEARCH_ACTION = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_SEARCH_ACTION;
        public static final int ITEM_TYPE_APP_GROUP = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_APP_GROUP;
        public static final int ITEM_TYPE_PRIVATE_SPACE_INSTALL_APP_BUTTON = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_PRIVATE_SPACE_INSTALL_APP_BUTTON;
        public static final int ITEM_TYPE_FILE_SYSTEM_FILE = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_FILE_SYSTEM_FILE;
        public static final int ITEM_TYPE_FILE_SYSTEM_FOLDER = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_FILE_SYSTEM_FOLDER;
        public static final int ITEM_TYPE_CUSTOM_VIEW = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_VIEW;
        public static final int ITEM_TYPE_SYSTEM_DRAG = com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_SYSTEM_DRAG;

        public static final String ICON = com.sandboxr.launcher.LauncherSettings.Favorites.ICON;
        public static final String CONTAINER = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER;

        public static final int CONTAINER_DESKTOP = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_DESKTOP;
        public static final int CONTAINER_HOTSEAT = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_HOTSEAT;
        public static final int CONTAINER_ALL_APPS_PREDICTION = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_ALL_APPS_PREDICTION;
        public static final int CONTAINER_WIDGETS_PREDICTION = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_WIDGETS_PREDICTION;
        public static final int CONTAINER_HOTSEAT_PREDICTION = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION;
        public static final int CONTAINER_ALL_APPS = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_ALL_APPS;
        public static final int CONTAINER_WIDGETS_TRAY = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_WIDGETS_TRAY;
        public static final int CONTAINER_BOTTOM_WIDGETS_TRAY = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_BOTTOM_WIDGETS_TRAY;
        public static final int CONTAINER_PIN_WIDGETS = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_PIN_WIDGETS;
        public static final int CONTAINER_WALLPAPERS = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_WALLPAPERS;
        public static final int CONTAINER_SHORTCUTS = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_SHORTCUTS;
        public static final int CONTAINER_SETTINGS = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_SETTINGS;
        public static final int CONTAINER_TASKSWITCHER = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_TASKSWITCHER;
        public static final int CONTAINER_PRIVATESPACE = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_PRIVATESPACE;
        public static final int EXTENDED_CONTAINERS = com.sandboxr.launcher.LauncherSettings.Favorites.EXTENDED_CONTAINERS;
        public static final int CONTAINER_UNKNOWN = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_UNKNOWN;

        public static final String SCREEN = com.sandboxr.launcher.LauncherSettings.Favorites.SCREEN;
        public static final String CELLX = com.sandboxr.launcher.LauncherSettings.Favorites.CELLX;
        public static final String CELLY = com.sandboxr.launcher.LauncherSettings.Favorites.CELLY;
        public static final String SPANX = com.sandboxr.launcher.LauncherSettings.Favorites.SPANX;
        public static final String SPANY = com.sandboxr.launcher.LauncherSettings.Favorites.SPANY;
        public static final String PROFILE_ID = com.sandboxr.launcher.LauncherSettings.Favorites.PROFILE_ID;
        public static final String APPWIDGET_ID = com.sandboxr.launcher.LauncherSettings.Favorites.APPWIDGET_ID;
        public static final String APPWIDGET_PROVIDER = com.sandboxr.launcher.LauncherSettings.Favorites.APPWIDGET_PROVIDER;
        public static final String RESTORED = com.sandboxr.launcher.LauncherSettings.Favorites.RESTORED;
        public static final String RANK = com.sandboxr.launcher.LauncherSettings.Favorites.RANK;
        public static final String OPTIONS = com.sandboxr.launcher.LauncherSettings.Favorites.OPTIONS;
        public static final String APPWIDGET_SOURCE = com.sandboxr.launcher.LauncherSettings.Favorites.APPWIDGET_SOURCE;

        public static final Uri CONTENT_URI = com.sandboxr.launcher.LauncherSettings.Favorites.CONTENT_URI;

        public static Uri getContentUri(long id) {
            return com.sandboxr.launcher.LauncherSettings.Favorites.getContentUri(id);
        }

        public static String containerToString(int container) {
            return com.sandboxr.launcher.LauncherSettings.Favorites.containerToString(container);
        }

        public static String itemTypeToString(int type) {
            return com.sandboxr.launcher.LauncherSettings.Favorites.itemTypeToString(type);
        }

        public static void addTableToDb(SQLiteDatabase db, long myProfileId, boolean optional) {
            com.sandboxr.launcher.LauncherSettings.Favorites.addTableToDb(db, myProfileId, optional);
        }

        public static void addTableToDb(SQLiteDatabase db, long myProfileId, boolean optional, String tableName) {
            com.sandboxr.launcher.LauncherSettings.Favorites.addTableToDb(db, myProfileId, optional, tableName);
        }

        public static LinkedHashMap<String, String> getColumnsToTypes(long profileId) {
            return com.sandboxr.launcher.LauncherSettings.Favorites.getColumnsToTypes(profileId);
        }

        public static String getColumns(long profileId) {
            return com.sandboxr.launcher.LauncherSettings.Favorites.getColumns(profileId);
        }
    }

    public static final class WorkspaceScreens implements BaseColumns {
        public static final String TABLE_NAME = com.sandboxr.launcher.LauncherSettings.WorkspaceScreens.TABLE_NAME;
        public static final String SCREEN_RANK = com.sandboxr.launcher.LauncherSettings.WorkspaceScreens.SCREEN_RANK;
        public static final String MODIFIED = com.sandboxr.launcher.LauncherSettings.WorkspaceScreens.MODIFIED;
        public static final Uri CONTENT_URI = com.sandboxr.launcher.LauncherSettings.WorkspaceScreens.CONTENT_URI;

        public static void addTableToDb(SQLiteDatabase db, boolean optional) {
            com.sandboxr.launcher.LauncherSettings.WorkspaceScreens.addTableToDb(db, optional);
        }
    }

    public static final class Settings {
        public static final String METHOD_CLEAR_EMPTY_DB_FLAG = com.sandboxr.launcher.LauncherSettings.Settings.METHOD_CLEAR_EMPTY_DB_FLAG;
        public static final String METHOD_NEW_ITEM_ID = com.sandboxr.launcher.LauncherSettings.Settings.METHOD_NEW_ITEM_ID;
        public static final String METHOD_NEW_SCREEN_ID = com.sandboxr.launcher.LauncherSettings.Settings.METHOD_NEW_SCREEN_ID;
        public static final String METHOD_CREATE_EMPTY_DB = com.sandboxr.launcher.LauncherSettings.Settings.METHOD_CREATE_EMPTY_DB;
        public static final String EXTRA_VALUE = com.sandboxr.launcher.LauncherSettings.Settings.EXTRA_VALUE;
        public static final String EXTRA_DB_NAME = com.sandboxr.launcher.LauncherSettings.Settings.EXTRA_DB_NAME;

        public static final String LAYOUT_PROVIDER_KEY = com.sandboxr.launcher.LauncherSettings.Settings.LAYOUT_PROVIDER_KEY;
        public static final String LAYOUT_DIGEST_LABEL = com.sandboxr.launcher.LauncherSettings.Settings.LAYOUT_DIGEST_LABEL;
        public static final String LAYOUT_DIGEST_TAG = com.sandboxr.launcher.LauncherSettings.Settings.LAYOUT_DIGEST_TAG;
        public static final String BLOB_KEY_PREFIX = com.sandboxr.launcher.LauncherSettings.Settings.BLOB_KEY_PREFIX;

        public static String createBlobProviderKey(byte[] digest) {
            return com.sandboxr.launcher.LauncherSettings.Settings.createBlobProviderKey(digest);
        }
    }
}
