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

package com.sandboxr.launcher.allapps

import android.content.ComponentName
import android.os.Process
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.util.ComponentKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Arrays

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AllAppsStoreTest {

    private lateinit var store: AllAppsStore

    @Before
    fun setUp() {
        store = AllAppsStore()
    }

    @Test
    fun testSetAppsAndNotifyListener() {
        var notified = false
        val listener = AllAppsStore.OnUpdateListener { notified = true }
        store.addUpdateListener(listener)

        val app1 = createApp("com.test.alpha", "Alpha")
        val app2 = createApp("com.test.beta", "Beta")
        val appsArray = arrayOf(app1, app2).apply {
            Arrays.sort(this, AppInfo.COMPONENT_KEY_COMPARATOR)
        }

        store.setApps(appsArray)

        assertTrue(notified)
        assertEquals(2, store.getApps().size)
    }

    @Test
    fun testGetAppByComponentKey() {
        val app1 = createApp("com.test.alpha", "Alpha")
        val app2 = createApp("com.test.beta", "Beta")
        val appsArray = arrayOf(app1, app2).apply {
            Arrays.sort(this, AppInfo.COMPONENT_KEY_COMPARATOR)
        }
        store.setApps(appsArray)

        val key = ComponentKey(app1.componentName!!, app1.user)
        val found = store.getApp(key)
        assertNotNull(found)
        assertEquals("Alpha", found?.title)

        val missingKey = ComponentKey(ComponentName("com.test.missing", "Activity"), Process.myUserHandle())
        assertNull(store.getApp(missingKey))
    }

    @Test
    fun testDeferUpdates() {
        var updateCount = 0
        store.addUpdateListener { updateCount++ }

        store.deferUpdates(AllAppsStore.DEFER_UPDATES_NEXT_DRAW)
        val app = createApp("com.test.app", "App")
        store.setApps(arrayOf(app))

        // Update should be deferred
        assertEquals(0, updateCount)

        store.disableDeferUpdates(AllAppsStore.DEFER_UPDATES_NEXT_DRAW)
        // Update should now be dispatched
        assertEquals(1, updateCount)
        assertEquals(1, store.getApps().size)
    }

    private fun createApp(pkg: String, title: String): AppInfo {
        return AppInfo().apply {
            componentName = ComponentName(pkg, "$pkg.MainActivity")
            user = Process.myUserHandle()
            this.title = title
            this.appTitle = title
        }
    }
}
