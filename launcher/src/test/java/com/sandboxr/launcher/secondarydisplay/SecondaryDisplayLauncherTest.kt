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

package com.sandboxr.launcher.secondarydisplay

import android.content.ComponentName
import android.content.Intent
import android.os.Process
import android.view.View
import androidx.lifecycle.Lifecycle
import com.sandboxr.launcher.DropTarget
import com.sandboxr.launcher.allapps.AllAppsStore
import com.sandboxr.launcher.dragndrop.DragOptions
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.ItemInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecondaryDisplayLauncherTest {

    @Test
    fun testSecondaryDisplayLauncherLaunchesSuccessfully() {
        val controller = Robolectric.buildActivity(SecondaryDisplayLauncher::class.java).setup()
        val launcher = controller.get()

        assertNotNull(launcher)
        assertEquals(Lifecycle.State.RESUMED, launcher.lifecycle.currentState)

        // Verify primary layout elements
        assertNotNull("RootView must be non-null", launcher.getRootView())
        assertNotNull("DragLayer must be non-null", launcher.getDragLayer())
        assertNotNull("AppsView must be non-null", launcher.getAppsView())
        assertNotNull("DragController must be non-null", launcher.getDragController())
        assertNotNull("SecondaryDisplayDelegate must be non-null", launcher.secondaryDisplayDelegate)

        // App drawer should initially be hidden
        assertFalse("App drawer should start hidden", launcher.isAppDrawerShown())
    }

    @Test
    fun testShowAppDrawerTogglesState() {
        val controller = Robolectric.buildActivity(SecondaryDisplayLauncher::class.java).setup()
        val launcher = controller.get()

        assertFalse(launcher.isAppDrawerShown())

        // Show drawer
        launcher.showAppDrawer(true)
        assertTrue(launcher.isAppDrawerShown())

        // Hide drawer
        launcher.showAppDrawer(false)
        assertFalse(launcher.isAppDrawerShown())
    }

    @Test
    fun testBackPressedClosesAppDrawer() {
        val controller = Robolectric.buildActivity(SecondaryDisplayLauncher::class.java).setup()
        val launcher = controller.get()

        launcher.showAppDrawer(true)
        assertTrue(launcher.isAppDrawerShown())

        launcher.onBackPressed()
        assertFalse("Back press should close app drawer", launcher.isAppDrawerShown())
    }

    @Test
    fun testPinnedAppsAdapterAddAndRemove() {
        val controller = Robolectric.buildActivity(SecondaryDisplayLauncher::class.java).setup()
        val launcher = controller.get()

        val allAppsStore = AllAppsStore()
        val adapter = PinnedAppsAdapter(
            launcher,
            allAppsStore,
            View.OnLongClickListener { true }
        )

        val component = ComponentName("com.example.test", "com.example.test.MainActivity")
        val item = AppInfo().apply {
            componentName = component
            user = Process.myUserHandle()
            intent = Intent().setComponent(component)
        }

        assertFalse("Item should not be pinned initially", adapter.isPinned(item))

        // Add pinned app
        adapter.addPinnedApp(item)
        assertTrue("Item should be pinned after adding", adapter.isPinned(item))

        // Remove pinned app
        adapter.removePinnedApp(item)
        assertFalse("Item should not be pinned after removal", adapter.isPinned(item))

        adapter.destroy()
    }

    @Test
    fun testPinnedAppsAdapterSystemShortcut() {
        val controller = Robolectric.buildActivity(SecondaryDisplayLauncher::class.java).setup()
        val launcher = controller.get()

        val allAppsStore = AllAppsStore()
        val adapter = PinnedAppsAdapter(
            launcher,
            allAppsStore,
            View.OnLongClickListener { true }
        )

        val dummyView = View(launcher)
        val component = ComponentName("com.example.test", "com.example.test.MainActivity")
        val item = AppInfo().apply {
            componentName = component
            user = Process.myUserHandle()
        }

        val shortcut = adapter.getSystemShortcut(item, dummyView)
        assertNotNull("System shortcut should be created", shortcut)
    }

    @Test
    fun testSecondaryDragControllerDropTarget() {
        val controller = Robolectric.buildActivity(SecondaryDisplayLauncher::class.java).setup()
        val launcher = controller.get()

        val dragController = SecondaryDragController(launcher)
        val dropTarget = dragController.getDefaultDropTarget(null)

        assertNotNull(dropTarget)
        assertTrue(dropTarget.isDropEnabled())

        val dragObject = DropTarget.DragObject()
        assertTrue(dropTarget.acceptDrop(dragObject))
    }

    @Test
    fun testSecondaryDisplayDelegateDefaults() {
        val delegate = SecondaryDisplayDelegate()
        assertFalse(delegate.enableTaskbarConnectedDisplays())
    }

    @Test
    fun testWorkspaceGridConfiguredWithPinnedAppsAdapter() {
        val controller = Robolectric.buildActivity(SecondaryDisplayLauncher::class.java).setup()
        val launcher = controller.get()

        val workspaceGrid = launcher.findViewById<android.widget.GridView>(com.sandboxr.launcher.R.id.workspace_grid)
        assertNotNull("Workspace GridView must exist in secondary launcher layout", workspaceGrid)
        assertNotNull("Workspace GridView must have an adapter attached", workspaceGrid.adapter)
        assertTrue("Device profile columns should be positive", workspaceGrid.numColumns > 0)
    }

    @Test
    fun testStartActivitySafelyLaunchesApp() {
        val controller = Robolectric.buildActivity(SecondaryDisplayLauncher::class.java).setup()
        val launcher = controller.get()

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setClassName("com.example.test", "com.example.test.MainActivity")
        }
        val item = AppInfo().apply {
            this.intent = intent
        }

        val success = launcher.startActivitySafely(null, intent, item)
        assertTrue("startActivitySafely should return true on successful start", success)

        val nextIntent = org.robolectric.Shadows.shadowOf(launcher).nextStartedActivity
        assertNotNull("Intent should be dispatched to system", nextIntent)
        assertEquals("com.example.test", nextIntent.component?.packageName)
        assertEquals("com.example.test.MainActivity", nextIntent.component?.className)
    }

    @Test
    fun testIconClickLaunchesAppOnSecondaryDisplay() {
        val controller = Robolectric.buildActivity(SecondaryDisplayLauncher::class.java).setup()
        val launcher = controller.get()

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setClassName("com.example.test", "com.example.test.SecondaryApp")
        }
        val item = AppInfo().apply {
            this.intent = intent
        }

        val view = View(launcher).apply {
            tag = item
        }
        launcher.getDragLayer()?.addView(view)

        launcher.getItemOnClickListener().onClick(view)

        val nextIntent = org.robolectric.Shadows.shadowOf(launcher).nextStartedActivity
        assertNotNull("Intent should be dispatched on icon click", nextIntent)
        assertEquals("com.example.test", nextIntent.component?.packageName)
        assertEquals("com.example.test.SecondaryApp", nextIntent.component?.className)
    }
}

