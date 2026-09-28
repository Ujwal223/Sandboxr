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

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.robolectric.RuntimeEnvironment
import com.android.launcher3.AutoInstallsLayout.LayoutParserCallback
import com.android.launcher3.LauncherSettings.Favorites
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DefaultLayoutParserTest {

    private lateinit var context: Context
    private val insertedItems = mutableListOf<ContentValues>()
    private val idCounter = AtomicInteger(100)

    private val callback = object : LayoutParserCallback {
        override fun generateNewItemId(): Int = idCounter.getAndIncrement()

        override fun insertAndCheck(db: SQLiteDatabase?, values: ContentValues?): Int {
            if (values != null) {
                insertedItems.add(ContentValues(values))
            }
            return values?.getAsInteger(Favorites._ID) ?: idCounter.get()
        }
    }

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        insertedItems.clear()
        idCounter.set(100)
    }

    @Test
    fun testParseDefaultWorkspace5x5() {
        val parser = DefaultLayoutParser(
            context,
            null,
            callback,
            context.resources,
            R.xml.default_workspace_5x5,
        )
        assertNotNull(parser)
    }

    @Test
    fun testParseDefaultWorkspace4x4() {
        val parser = DefaultLayoutParser(
            context,
            null,
            callback,
            context.resources,
            R.xml.default_workspace_4x4,
        )
        assertNotNull(parser)
    }

    @Test
    fun testParseDefaultWorkspace6x5() {
        val parser = DefaultLayoutParser(
            context,
            null,
            callback,
            context.resources,
            R.xml.default_workspace_6x5,
        )
        assertNotNull(parser)
    }

    @Test
    fun testParseDefaultWorkspace2x2() {
        val parser = DefaultLayoutParser(
            context,
            null,
            callback,
            context.resources,
            R.xml.default_workspace_2x2,
        )
        assertNotNull(parser)
    }

    @Test
    fun testParseDefaultWorkspace3x3() {
        val parser = DefaultLayoutParser(
            context,
            null,
            callback,
            context.resources,
            R.xml.default_workspace_3x3,
        )
        assertNotNull(parser)
    }

    @Test
    fun testParseDefaultWorkspace4x5() {
        val parser = DefaultLayoutParser(
            context,
            null,
            callback,
            context.resources,
            R.xml.default_workspace_4x5,
        )
        assertNotNull(parser)
    }

    @Test
    fun testParseDefaultWorkspace5x8() {
        val parser = DefaultLayoutParser(
            context,
            null,
            callback,
            context.resources,
            R.xml.default_workspace_5x8,
        )
        assertNotNull(parser)
    }

    @Test
    fun testParseDefaultWorkspace7x3() {
        val parser = DefaultLayoutParser(
            context,
            null,
            callback,
            context.resources,
            R.xml.default_workspace_7x3,
        )
        assertNotNull(parser)
    }

    @Test
    fun testParseDefaultWorkspace8x3() {
        val parser = DefaultLayoutParser(
            context,
            null,
            callback,
            context.resources,
            R.xml.default_workspace_8x3,
        )
        assertNotNull(parser)
    }
}
