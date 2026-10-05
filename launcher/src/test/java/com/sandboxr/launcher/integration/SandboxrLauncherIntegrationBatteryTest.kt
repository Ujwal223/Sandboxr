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

package com.sandboxr.launcher.integration

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.folder.Folder
import com.sandboxr.launcher.folder.FolderInfo
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.ui.AppItem
import com.sandboxr.launcher.uioverrides.QuickstepLauncher
import com.sandboxr.launcher.virtual.EnvironmentFilterMode
import com.sandboxr.launcher.virtual.VirtualAllAppsFilter
import com.sandboxr.launcher.virtual.VirtualAppInfo
import com.sandboxr.launcher.virtual.VirtualIconBadgeRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Master end-to-end integration test battery for SANDBOXR Launcher.
 * Validates workspace CRUD, folder lifecycle, drawer search/filtering,
 * QuickStep state transitions, and virtual container badging.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SandboxrLauncherIntegrationBatteryTest {

    @Test
    fun testWorkspaceItemLifecycle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val item = WorkspaceItemInfo().apply {
            title = "Test Item"
            cellX = 0
            cellY = 1
            spanX = 1
            spanY = 1
            screenId = 0
        }

        assertEquals("Test Item", item.title)
        assertEquals(0, item.cellX)
        assertEquals(1, item.cellY)

        // Modify position (Drag & Drop simulation)
        item.cellX = 2
        item.cellY = 3
        assertEquals(2, item.cellX)
        assertEquals(3, item.cellY)
    }

    @Test
    fun testFolderCreationAndContentManagement() {
        val folderInfo = FolderInfo().apply {
            title = "Privacy Tools"
        }

        val item1 = WorkspaceItemInfo().apply { title = "Signal" }
        val item2 = WorkspaceItemInfo().apply { title = "Tor Browser" }

        folderInfo.add(item1, false)
        folderInfo.add(item2, false)

        assertEquals("Privacy Tools", folderInfo.title)
        assertEquals(2, folderInfo.contents.size)
        assertEquals("Signal", folderInfo.contents[0].title)
        assertEquals("Tor Browser", folderInfo.contents[1].title)

        folderInfo.remove(item1, false)
        assertEquals(1, folderInfo.contents.size)
        assertEquals("Tor Browser", folderInfo.contents[0].title)
    }

    @Test
    fun testAllAppsSearchAndEnvironmentFiltering() {
        val apps = listOf(
            AppItem(packageName = "com.android.settings", label = "Settings", envId = "system", isSystemApp = true),
            AppItem(packageName = "com.whatsapp", label = "WhatsApp Personal", envId = "env-personal", isSystemApp = false),
            AppItem(packageName = "com.slack", label = "Slack Work", envId = "env-work", isSystemApp = false)
        )

        val filter = VirtualAllAppsFilter()

        // Test All mode
        filter.setFilterMode(EnvironmentFilterMode.All)
        assertEquals(3, filter.filterAppItems(apps).size)

        // Test System mode
        filter.setFilterMode(EnvironmentFilterMode.SystemOnly)
        val systemApps = filter.filterAppItems(apps)
        assertEquals(1, systemApps.size)
        assertEquals("Settings", systemApps[0].label)

        // Test Environment Work mode
        filter.setFilterMode(EnvironmentFilterMode.Environment("env-work"))
        val workApps = filter.filterAppItems(apps)
        assertEquals(1, workApps.size)
        assertEquals("Slack Work", workApps[0].label)

        // Test Search within environment
        val searchResults = filter.filterAppItems(apps, searchQuery = "slack")
        assertEquals(1, searchResults.size)
    }

    @Test
    fun testQuickstepStateTransitions() {
        val controller = Robolectric.buildActivity(QuickstepLauncher::class.java).setup()
        val launcher = controller.get()

        assertNotNull(launcher)
        assertEquals(LauncherState.NORMAL, launcher.stateManager.state)

        // Transition to ALL_APPS
        launcher.stateManager.goToState(LauncherState.ALL_APPS, false)
        assertEquals(LauncherState.ALL_APPS, launcher.stateManager.state)

        // Transition to OVERVIEW (Recents)
        launcher.stateManager.goToState(LauncherState.OVERVIEW, false)
        assertEquals(LauncherState.OVERVIEW, launcher.stateManager.state)

        // Transition back to NORMAL
        launcher.stateManager.goToState(LauncherState.NORMAL, false)
        assertEquals(LauncherState.NORMAL, launcher.stateManager.state)
    }

    @Test
    fun testVirtualContainerBadgingAndTaskRestoration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dummyBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)

        val virtualApp = VirtualAppInfo(
            packageName = "org.telegram.messenger",
            mainActivity = "org.telegram.ui.LaunchActivity",
            label = "Telegram Isolated",
            envId = "env-crypto-999",
            envName = "Vault",
            envColor = 0xFF9C27B0, // Purple
            iconBitmap = dummyBitmap
        )

        val badgedBitmap = VirtualIconBadgeRenderer.createBadgedBitmap(dummyBitmap, 0xFF9C27B0.toInt())
        assertNotNull(badgedBitmap)

        val view = BubbleTextView(context)
        view.environmentBadgeColor = 0xFF9C27B0.toInt()
        assertEquals(0xFF9C27B0.toInt(), view.environmentBadgeColor)

        val appInfo = virtualApp.toLauncherAppInfo(context)
        assertEquals("Telegram Isolated", appInfo.title)
        assertTrue(appInfo.intent.flags and Intent.FLAG_ACTIVITY_NEW_DOCUMENT != 0)
        assertTrue(appInfo.intent.flags and Intent.FLAG_ACTIVITY_MULTIPLE_TASK != 0)
    }
}
