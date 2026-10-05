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

package com.sandboxr.launcher.virtual

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.ui.AppItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VirtualEnvironmentIntegrationTest {

    @Test
    fun testVirtualAppInfoModelConversion() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dummyBitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)

        val virtualApp = VirtualAppInfo(
            packageName = "com.mock.guestapp",
            mainActivity = "com.mock.guestapp.MainActivity",
            label = "Mock Guest App",
            envId = "env-work-123",
            envName = "Work",
            envColor = 0xFF4CAF50,
            iconBitmap = dummyBitmap
        )

        assertEquals("com.mock.guestapp@env-work-123", virtualApp.compositeKey)

        // Convert to Launcher AppInfo
        val appInfo = virtualApp.toLauncherAppInfo(context)
        assertEquals("Mock Guest App", appInfo.title)
        assertNotNull(appInfo.intent)
        assertEquals(
            "env-work-123",
            appInfo.intent.getStringExtra(com.sandboxr.virtual.server.am.VActivityManagerService.EXTRA_ENV_ID)
        )
        assertEquals(
            "com.mock.guestapp",
            appInfo.intent.getStringExtra(com.sandboxr.virtual.server.am.VActivityManagerService.EXTRA_TARGET_PKG)
        )

        // Convert to Compose AppItem
        val appItem = virtualApp.toAppItem()
        assertEquals("com.mock.guestapp", appItem.packageName)
        assertEquals("Mock Guest App", appItem.label)
        assertEquals("env-work-123", appItem.envId)
        assertFalse(appItem.isSystemApp)
    }

    @Test
    fun testVirtualIconBadgeRenderer() {
        val baseBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        baseBitmap.eraseColor(Color.LTGRAY)

        val targetColor = Color.GREEN
        val badgedBitmap = VirtualIconBadgeRenderer.createBadgedBitmap(baseBitmap, targetColor)

        assertNotNull(badgedBitmap)
        assertEquals(100, badgedBitmap.width)
        assertEquals(100, badgedBitmap.height)
    }

    @Test
    fun testBubbleTextViewEnvironmentBadgeColor() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = BubbleTextView(context)

        assertEquals(0, view.environmentBadgeColor)

        view.environmentBadgeColor = Color.BLUE
        assertEquals(Color.BLUE, view.environmentBadgeColor)

        // Verify draw execution without throwing NPE or crash
        val canvas = Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
        view.onDraw(canvas)
    }

    @Test
    fun testVirtualAllAppsFilter() {
        val items = listOf(
            AppItem(
                packageName = "com.android.settings",
                label = "Settings",
                envId = "system",
                isSystemApp = true
            ),
            AppItem(
                packageName = "com.whatsapp",
                label = "WhatsApp Personal",
                envId = "env-personal",
                isSystemApp = false
            ),
            AppItem(
                packageName = "com.slack",
                label = "Slack Work",
                envId = "env-work",
                isSystemApp = false
            )
        )

        val filter = VirtualAllAppsFilter()

        // Mode: ALL
        filter.setFilterMode(EnvironmentFilterMode.All)
        val allResult = filter.filterAppItems(items)
        assertEquals(3, allResult.size)

        // Mode: SYSTEM_ONLY
        filter.setFilterMode(EnvironmentFilterMode.SystemOnly)
        val systemResult = filter.filterAppItems(items)
        assertEquals(1, systemResult.size)
        assertEquals("com.android.settings", systemResult[0].packageName)

        // Mode: ENVIRONMENT("env-work")
        filter.setFilterMode(EnvironmentFilterMode.Environment("env-work"))
        val workResult = filter.filterAppItems(items)
        assertEquals(1, workResult.size)
        assertEquals("com.slack", workResult[0].packageName)

        // Search query with active filter
        val searchResult = filter.filterAppItems(items, searchQuery = "slack")
        assertEquals(1, searchResult.size)

        val noMatchResult = filter.filterAppItems(items, searchQuery = "nonexistent")
        assertEquals(0, noMatchResult.size)
    }
}
