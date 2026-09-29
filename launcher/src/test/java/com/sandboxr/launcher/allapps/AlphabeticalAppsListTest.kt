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
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.model.data.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Arrays

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlphabeticalAppsListTest {

    private lateinit var launcher: Launcher
    private lateinit var store: AllAppsStore
    private lateinit var appsList: AlphabeticalAppsList

    @Before
    fun setUp() {
        val controller = Robolectric.buildActivity(Launcher::class.java).setup()
        launcher = controller.get()
        store = AllAppsStore()
        appsList = AlphabeticalAppsList(launcher, store)
    }

    @Test
    fun testAlphabeticalSortingAndFastScrollSections() {
        val appZ = createApp("com.test.zebra", "Zebra")
        val appA = createApp("com.test.apple", "Apple")
        val appB = createApp("com.test.banana", "Banana")
        val app1 = createApp("com.test.one", "1Password")

        val apps = arrayOf(appZ, appA, appB, app1)
        Arrays.sort(apps, AppInfo.COMPONENT_KEY_COMPARATOR)
        store.setApps(apps)

        val sections = appsList.getFastScrollerSections()
        assertTrue(sections.isNotEmpty())

        // Ensure section headers contain mapped letters
        val sectionNames = sections.map { it.sectionName.toString() }
        assertTrue(sectionNames.contains("A"))
        assertTrue(sectionNames.contains("B"))
        assertTrue(sectionNames.contains("Z"))
        assertTrue(sectionNames.contains("#"))

        val sortedItems = appsList.getAdapterItems().filter { it.itemInfo != null }
        assertEquals("1Password", sortedItems[0].itemInfo?.title)
        assertEquals("Apple", sortedItems[1].itemInfo?.title)
        assertEquals("Banana", sortedItems[2].itemInfo?.title)
        assertEquals("Zebra", sortedItems[3].itemInfo?.title)
    }

    @Test
    fun testItemFilter() {
        val appA = createApp("com.test.apple", "Apple")
        val appB = createApp("com.test.banana", "Banana")
        store.setApps(arrayOf(appA, appB))

        assertEquals(2, appsList.getNumFilteredApps())

        // Filter out everything except Banana
        appsList.updateItemFilter { it.title == "Banana" }

        assertEquals(1, appsList.getNumFilteredApps())
        val items = appsList.getAdapterItems().filter { it.itemInfo != null }
        assertEquals(1, items.size)
        assertEquals("Banana", items[0].itemInfo?.title)
    }

    @Test
    fun testSearchResultsOverride() {
        val appA = createApp("com.test.apple", "Apple")
        store.setApps(arrayOf(appA))

        assertFalse(appsList.hasSearchResults())

        val searchItem = BaseAllAppsAdapter.AdapterItem.asApp(createApp("com.test.search", "Search Result"))
        appsList.setSearchResults(listOf(searchItem))

        assertTrue(appsList.hasSearchResults())
        assertEquals(1, appsList.getAdapterItems().size)
        assertEquals("Search Result", appsList.getAdapterItems()[0].itemInfo?.title)

        appsList.setSearchResults(null)
        assertFalse(appsList.hasSearchResults())
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
