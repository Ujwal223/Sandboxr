/*
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
import android.content.ContentValues
import android.content.Context
import android.graphics.Point
import android.net.Uri
import com.sandboxr.launcher.LauncherSettings.Favorites
import com.sandboxr.launcher.LauncherSettings.Settings
import com.sandboxr.launcher.LauncherSettings.WorkspaceScreens
import com.sandboxr.launcher.model.DatabaseHelper
import com.sandboxr.launcher.provider.ImportDataTask
import com.sandboxr.launcher.provider.LauncherDbUtils
import com.sandboxr.launcher.provider.RestoreDbTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LauncherProviderTest {

    private lateinit var context: Context
    private lateinit var provider: LauncherProvider
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var authority: String

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        authority = LauncherSettings.getAuthority(context)

        // Set up in-memory / unique test database
        dbHelper = DatabaseHelper(context, "test_launcher.db")
        provider = Robolectric.setupContentProvider(LauncherProvider::class.java, authority)
        provider.setDatabaseHelperForTesting(dbHelper)
    }

    @Test
    fun testDatabaseCreatesOnFirstLaunchWithCorrectSchema() {
        val db = dbHelper.writableDatabase
        assertNotNull(db)
        assertTrue(db.isOpen)

        assertTrue("favorites table must exist", LauncherDbUtils.tableExists(db, Favorites.TABLE_NAME))
        assertTrue("workspaceScreens table must exist", LauncherDbUtils.tableExists(db, WorkspaceScreens.TABLE_NAME))

        // Check columns in favorites
        val cursor = db.rawQuery("PRAGMA table_info(${Favorites.TABLE_NAME})", null)
        val columns = mutableSetOf<String>()
        cursor.use {
            val nameIdx = it.getColumnIndex("name")
            while (it.moveToNext()) {
                columns.add(it.getString(nameIdx))
            }
        }

        assertTrue(columns.contains(Favorites._ID))
        assertTrue(columns.contains(Favorites.TITLE))
        assertTrue(columns.contains(Favorites.INTENT))
        assertTrue(columns.contains(Favorites.CONTAINER))
        assertTrue(columns.contains(Favorites.SCREEN))
        assertTrue(columns.contains(Favorites.CELLX))
        assertTrue(columns.contains(Favorites.CELLY))
        assertTrue(columns.contains(Favorites.SPANX))
        assertTrue(columns.contains(Favorites.SPANY))
        assertTrue(columns.contains(Favorites.ITEM_TYPE))
        assertTrue(columns.contains(Favorites.APPWIDGET_ID))
        assertTrue(columns.contains(Favorites.PROFILE_ID))
        assertTrue(columns.contains(Favorites.RANK))
        assertTrue(columns.contains(Favorites.OPTIONS))
    }

    @Test
    fun testInsertAndQueryFavorite() {
        val uri = Uri.parse("content://$authority/${Favorites.TABLE_NAME}")
        val values = ContentValues().apply {
            put(Favorites.TITLE, "Sandboxr Pro")
            put(Favorites.INTENT, "#Intent;action=android.intent.action.MAIN;end")
            put(Favorites.CONTAINER, Favorites.CONTAINER_DESKTOP)
            put(Favorites.SCREEN, 0)
            put(Favorites.CELLX, 1)
            put(Favorites.CELLY, 2)
            put(Favorites.SPANX, 1)
            put(Favorites.SPANY, 1)
            put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_APPLICATION)
        }

        val insertedUri = provider.insert(uri, values)
        assertNotNull(insertedUri)
        val insertedId = ContentUris.parseId(insertedUri!!)
        assertTrue("Inserted ID should be positive", insertedId > 0)

        // Query by table URI
        val cursor = provider.query(uri, null, "${Favorites._ID} = ?", arrayOf(insertedId.toString()), null)
        assertNotNull(cursor)
        cursor!!.use {
            assertTrue(it.moveToFirst())
            assertEquals("Sandboxr Pro", it.getString(it.getColumnIndexOrThrow(Favorites.TITLE)))
            assertEquals(Favorites.CONTAINER_DESKTOP, it.getInt(it.getColumnIndexOrThrow(Favorites.CONTAINER)))
            assertEquals(1, it.getInt(it.getColumnIndexOrThrow(Favorites.CELLX)))
            assertEquals(2, it.getInt(it.getColumnIndexOrThrow(Favorites.CELLY)))
        }

        // Query by item ID URI
        val singleItemUri = Uri.parse("content://$authority/${Favorites.TABLE_NAME}/$insertedId")
        val singleCursor = provider.query(singleItemUri, null, null, null, null)
        assertNotNull(singleCursor)
        singleCursor!!.use {
            assertTrue(it.moveToFirst())
            assertEquals(insertedId, it.getLong(it.getColumnIndexOrThrow(Favorites._ID)))
        }
    }

    @Test
    fun testUpdateAndDeleteFavorite() {
        val uri = Uri.parse("content://$authority/${Favorites.TABLE_NAME}")
        val values = ContentValues().apply {
            put(Favorites.TITLE, "Original Title")
            put(Favorites.CONTAINER, Favorites.CONTAINER_DESKTOP)
            put(Favorites.SCREEN, 0)
        }
        val insertedUri = provider.insert(uri, values)!!
        val id = ContentUris.parseId(insertedUri)

        // Update
        val updateValues = ContentValues().apply {
            put(Favorites.TITLE, "Updated Title")
            put(Favorites.CELLX, 3)
        }
        val updatedRows = provider.update(insertedUri, updateValues, null, null)
        assertEquals(1, updatedRows)

        val cursor = provider.query(insertedUri, null, null, null, null)!!
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("Updated Title", it.getString(it.getColumnIndexOrThrow(Favorites.TITLE)))
            assertEquals(3, it.getInt(it.getColumnIndexOrThrow(Favorites.CELLX)))
        }

        // Delete
        val deletedRows = provider.delete(insertedUri, null, null)
        assertEquals(1, deletedRows)

        val verifyCursor = provider.query(insertedUri, null, null, null, null)!!
        verifyCursor.use {
            assertFalse("Deleted row should not exist", it.moveToFirst())
        }
    }

    @Test
    fun testCallMethodIdGeneration() {
        val bundleItem = provider.call(Settings.METHOD_NEW_ITEM_ID, null, null)
        assertNotNull(bundleItem)
        val itemId = bundleItem!!.getLong(Settings.EXTRA_VALUE)
        assertTrue(itemId > 0)

        val bundleItem2 = provider.call(Settings.METHOD_NEW_ITEM_ID, null, null)
        assertEquals(itemId + 1, bundleItem2!!.getLong(Settings.EXTRA_VALUE))

        val bundleScreen = provider.call(Settings.METHOD_NEW_SCREEN_ID, null, null)
        assertNotNull(bundleScreen)
        val screenId = bundleScreen!!.getLong(Settings.EXTRA_VALUE)
        assertTrue(screenId > 0)
    }

    @Test
    fun testRestoreDbTaskSanitization() {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(Favorites._ID, 101L)
            put(Favorites.TITLE, "App 1")
            put(Favorites.CONTAINER, Favorites.CONTAINER_DESKTOP)
            put(Favorites.SCREEN, 5) // Gap in screens
            put(Favorites.PROFILE_ID, 999L) // Old profile
            put(Favorites.RESTORED, 0)
        }
        db.insert(Favorites.TABLE_NAME, null, values)

        val restoreTask = RestoreDbTask()
        val count = restoreTask.sanitizeDB(context, db, myProfileId = 0L)
        assertEquals(1, count)

        db.query(Favorites.TABLE_NAME, null, "_id = 101", null, null, null, null).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0L, c.getLong(c.getColumnIndexOrThrow(Favorites.PROFILE_ID)))
            assertEquals(1, c.getInt(c.getColumnIndexOrThrow(Favorites.RESTORED)))
            assertEquals(0, c.getInt(c.getColumnIndexOrThrow(Favorites.SCREEN))) // Screen 5 shifted to 0
        }
    }

    @Test
    fun testImportDataTaskGridMigration() {
        val srcDbHelper = DatabaseHelper(context, "src_grid.db")
        val destDbHelper = DatabaseHelper(context, "dest_grid.db")

        val srcDb = srcDbHelper.writableDatabase
        val destDb = destDbHelper.writableDatabase

        // Add 2 items to srcDb (4x4 grid)
        val item1 = ContentValues().apply {
            put(Favorites._ID, 1L)
            put(Favorites.TITLE, "Icon 1")
            put(Favorites.CONTAINER, Favorites.CONTAINER_DESKTOP)
            put(Favorites.SCREEN, 0)
            put(Favorites.CELLX, 0)
            put(Favorites.CELLY, 0)
            put(Favorites.SPANX, 1)
            put(Favorites.SPANY, 1)
        }
        val item2 = ContentValues().apply {
            put(Favorites._ID, 2L)
            put(Favorites.TITLE, "Hotseat Icon")
            put(Favorites.CONTAINER, Favorites.CONTAINER_HOTSEAT)
            put(Favorites.SCREEN, 0)
            put(Favorites.CELLX, 0)
            put(Favorites.CELLY, 0)
            put(Favorites.SPANX, 1)
            put(Favorites.SPANY, 1)
        }
        srcDb.insert(Favorites.TABLE_NAME, null, item1)
        srcDb.insert(Favorites.TABLE_NAME, null, item2)

        val task = ImportDataTask(
            context = context,
            srcDb = srcDb,
            destDb = destDb,
            srcGridSize = Point(4, 4),
            destGridSize = Point(5, 5)
        )
        val migrated = task.importData()
        assertEquals(2, migrated)

        destDb.query(Favorites.TABLE_NAME, null, null, null, null, null, null).use { cursor ->
            assertEquals(2, cursor.count)
        }
    }
}
