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

package com.sandboxr.launcher.search

import android.content.ComponentName
import android.os.Process
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import org.robolectric.RuntimeEnvironment
import com.sandboxr.launcher.allapps.AllAppsStore
import com.sandboxr.launcher.allapps.BaseAllAppsAdapter
import com.sandboxr.launcher.allapps.search.DefaultAppSearchAlgorithm
import com.sandboxr.launcher.model.data.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.ArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppSearchEngineTest {

    private fun createApp(pkg: String, label: String): AppInfo {
        return AppInfo().apply {
            componentName = ComponentName(pkg, "$pkg.MainActivity")
            title = label
            user = Process.myUserHandle()
        }
    }

    @Test
    fun testStringMatcherUtilityScoringAndMatching() {
        // Prefix matching
        assertTrue(StringMatcherUtility.matches("calc", "Calculator"))
        assertTrue(StringMatcherUtility.fuzzyScore("calc", "Calculator") > 1000)

        // Word boundary matching
        assertTrue(StringMatcherUtility.matches("cal", "Google Calendar"))
        assertTrue(StringMatcherUtility.fuzzyScore("cal", "Google Calendar") > 800)

        // Fuzzy acronym matching
        assertTrue(StringMatcherUtility.fuzzyScore("ps", "Play Store") > 500)

        // Subsequence matching
        assertTrue(StringMatcherUtility.fuzzyScore("msg", "Messages") > 200)

        // Non match returns -1
        assertEquals(-1, StringMatcherUtility.fuzzyScore("xyz", "Calculator"))
    }

    @Test
    fun testHighlightMatchesGeneratesSpans() {
        val highlighted = StringMatcherUtility.highlightMatches("Calculator", "calc", 0xFF4E80EE.toInt())
        assertTrue(highlighted is Spanned)
        val spanned = highlighted as Spanned

        val colorSpans = spanned.getSpans(0, spanned.length, ForegroundColorSpan::class.java)
        val styleSpans = spanned.getSpans(0, spanned.length, StyleSpan::class.java)

        assertTrue(colorSpans.isNotEmpty())
        assertTrue(styleSpans.isNotEmpty())
        assertEquals(0, spanned.getSpanStart(colorSpans[0]))
        assertEquals(4, spanned.getSpanEnd(colorSpans[0]))
    }

    @Test
    fun testDefaultAppSearchAlgorithmFindsAndRanksApps() {
        val context = RuntimeEnvironment.getApplication()
        val store = AllAppsStore()
        val appChrome = createApp("com.android.chrome", "Chrome")
        val appCalc = createApp("com.android.calculator", "Calculator")
        val appCamera = createApp("com.android.camera", "Camera")
        val appMaps = createApp("com.google.android.apps.maps", "Google Maps")

        store.setApps(arrayOf(appChrome, appCalc, appCamera, appMaps))

        val algorithm = DefaultAppSearchAlgorithm(context, store)
        var receivedQuery: String? = null
        var receivedItems: ArrayList<BaseAllAppsAdapter.AdapterItem>? = null

        algorithm.doSearch("cam", object : SearchCallback<BaseAllAppsAdapter.AdapterItem> {
            override fun onSearchResult(
                query: String,
                items: ArrayList<BaseAllAppsAdapter.AdapterItem>
            ) {
                receivedQuery = query
                receivedItems = items
            }

            override fun clearSearchResult() {}
        })

        // Wait for executor background thread and shadow looper
        Thread.sleep(200)
        ShadowLooper.idleMainLooper()

        assertNotNull(receivedItems)
        val items = receivedItems!!
        assertEquals("cam", receivedQuery)
        assertTrue(items.isNotEmpty())
        assertEquals("Camera", items[0].itemInfo?.title.toString())
        assertNotNull(items[0].highlightedTitle)
    }

    @Test
    fun testEmptySearchGeneratesEmptySearchItem() {
        val context = RuntimeEnvironment.getApplication()
        val store = AllAppsStore()
        store.setApps(arrayOf(createApp("com.app", "TestApp")))

        val algorithm = DefaultAppSearchAlgorithm(context, store, addNoResultsMessage = true)
        var receivedItems: ArrayList<BaseAllAppsAdapter.AdapterItem>? = null

        algorithm.doSearch("nonexistent", object : SearchCallback<BaseAllAppsAdapter.AdapterItem> {
            override fun onSearchResult(
                query: String,
                items: ArrayList<BaseAllAppsAdapter.AdapterItem>
            ) {
                receivedItems = items
            }

            override fun clearSearchResult() {}
        })

        Thread.sleep(200)
        ShadowLooper.idleMainLooper()

        assertNotNull(receivedItems)
        val emptyList = receivedItems!!
        assertEquals(1, emptyList.size)
        assertEquals(BaseAllAppsAdapter.VIEW_TYPE_EMPTY_SEARCH, emptyList[0].viewType)
    }
}
