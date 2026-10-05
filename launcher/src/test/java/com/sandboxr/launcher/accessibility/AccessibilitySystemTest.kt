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

package com.sandboxr.launcher.accessibility

import android.content.Context
import android.view.View
import com.sandboxr.launcher.CellLayout
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherSettings
import com.sandboxr.launcher.PendingAddItemInfo
import com.sandboxr.launcher.R
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.FolderInfo
import com.sandboxr.launcher.model.data.ItemInfoWithIcon.FLAG_NOT_PINNABLE
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
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
class AccessibilitySystemTest {

    private lateinit var context: Context
    private lateinit var launcher: Launcher
    private lateinit var delegate: LauncherAccessibilityDelegate
    private lateinit var hostView: View

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        val controller = Robolectric.buildActivity(Launcher::class.java)
        controller.create()
        launcher = controller.get()

        delegate = launcher.getAccessibilityDelegate() ?: LauncherAccessibilityDelegate(launcher)
        hostView = View(launcher)
    }

    @Test
    fun testActionConstantsAndRegistration() {
        val actions = mutableListOf<BaseAccessibilityDelegate.LauncherAction>()
        val item = WorkspaceItemInfo().apply {
            id = 101
            screenId = 0
            container = LauncherSettings.Favorites.CONTAINER_DESKTOP
        }
        hostView.tag = item

        delegate.getSupportedActions(hostView, item, actions)

        // Move action should be supported for items with valid id on workspace
        assertTrue("Expected MOVE action", actions.any { it.accessibilityAction.id == LauncherAccessibilityDelegate.MOVE })

        // Check defined action IDs are valid
        assertNotNull(LauncherAccessibilityDelegate.REMOVE)
        assertNotNull(LauncherAccessibilityDelegate.UNINSTALL)
        assertNotNull(LauncherAccessibilityDelegate.DISMISS_PREDICTION)
        assertNotNull(LauncherAccessibilityDelegate.PIN_PREDICTION)
        assertNotNull(LauncherAccessibilityDelegate.RECONFIGURE)
        assertNotNull(LauncherAccessibilityDelegate.ADD_TO_WORKSPACE)
        assertNotNull(LauncherAccessibilityDelegate.MOVE)
        assertNotNull(LauncherAccessibilityDelegate.MOVE_TO_WORKSPACE)
        assertNotNull(LauncherAccessibilityDelegate.RESIZE)
        assertNotNull(LauncherAccessibilityDelegate.DEEP_SHORTCUTS)
        assertNotNull(LauncherAccessibilityDelegate.CLOSE)
    }

    @Test
    fun testSupportAddToWorkspaceWithAppInfo() {
        val appInfo = AppInfo().apply {
            container = LauncherSettings.Favorites.CONTAINER_ALL_APPS
            runtimeStatusFlags = 0
        }
        val actions = mutableListOf<BaseAccessibilityDelegate.LauncherAction>()
        delegate.getSupportedActions(hostView, appInfo, actions)
        assertTrue(
            "Expected ADD_TO_WORKSPACE for pinnable AppInfo",
            actions.any { it.accessibilityAction.id == LauncherAccessibilityDelegate.ADD_TO_WORKSPACE }
        )

        // With NOT_PINNABLE flag
        val unpinnableAppInfo = AppInfo().apply {
            container = LauncherSettings.Favorites.CONTAINER_ALL_APPS
            runtimeStatusFlags = FLAG_NOT_PINNABLE
        }
        val actionsUnpinnable = mutableListOf<BaseAccessibilityDelegate.LauncherAction>()
        delegate.getSupportedActions(hostView, unpinnableAppInfo, actionsUnpinnable)
        assertFalse(
            "Did not expect ADD_TO_WORKSPACE for unpinnable AppInfo",
            actionsUnpinnable.any { it.accessibilityAction.id == LauncherAccessibilityDelegate.ADD_TO_WORKSPACE }
        )

        // Already on desktop
        val desktopAppInfo = AppInfo().apply {
            container = LauncherSettings.Favorites.CONTAINER_DESKTOP
            runtimeStatusFlags = 0
        }
        val actionsDesktop = mutableListOf<BaseAccessibilityDelegate.LauncherAction>()
        delegate.getSupportedActions(hostView, desktopAppInfo, actionsDesktop)
        assertFalse(
            "Did not expect ADD_TO_WORKSPACE for desktop AppInfo",
            actionsDesktop.any { it.accessibilityAction.id == LauncherAccessibilityDelegate.ADD_TO_WORKSPACE }
        )
    }

    @Test
    fun testSupportAddToWorkspaceWithPendingAddItemInfo() {
        val pendingItem = PendingAddItemInfo().apply {
            container = LauncherSettings.Favorites.CONTAINER_ALL_APPS
        }
        val actions = mutableListOf<BaseAccessibilityDelegate.LauncherAction>()
        delegate.getSupportedActions(hostView, pendingItem, actions)
        assertTrue(
            "Expected ADD_TO_WORKSPACE for PendingAddItemInfo",
            actions.any { it.accessibilityAction.id == LauncherAccessibilityDelegate.ADD_TO_WORKSPACE }
        )
    }

    @Test
    fun testMoveToWorkspaceActionForFolderItems() {
        val itemInFolder = WorkspaceItemInfo().apply {
            id = 202
            screenId = 0
            container = 1 // Inside a folder with id 1
        }
        val actions = mutableListOf<BaseAccessibilityDelegate.LauncherAction>()
        delegate.getSupportedActions(hostView, itemInFolder, actions)
        assertTrue(
            "Expected MOVE_TO_WORKSPACE for item in folder",
            actions.any { it.accessibilityAction.id == LauncherAccessibilityDelegate.MOVE_TO_WORKSPACE }
        )
    }

    @Test
    fun testWidgetResizePopupDataSourceActions() {
        val incWidth = WidgetResizePopupDataSource.increaseWidthAction()
        assertEquals(R.string.action_increase_width, incWidth.labelResId)
        assertEquals(R.drawable.ic_widget_width_increase, incWidth.iconResId)

        val decWidth = WidgetResizePopupDataSource.decreaseWidthAction()
        assertEquals(R.string.action_decrease_width, decWidth.labelResId)
        assertEquals(R.drawable.ic_widget_width_decrease, decWidth.iconResId)

        val incHeight = WidgetResizePopupDataSource.increaseHeightAction()
        assertEquals(R.string.action_increase_height, incHeight.labelResId)
        assertEquals(R.drawable.ic_widget_height_increase, incHeight.iconResId)

        val decHeight = WidgetResizePopupDataSource.decreaseHeightAction()
        assertEquals(R.string.action_decrease_height, decHeight.labelResId)
        assertEquals(R.drawable.ic_widget_height_decrease, decHeight.iconResId)
    }

    @Test
    fun testFolderAccessibilityHelper() {
        val cellLayout = CellLayout(launcher)
        cellLayout.setGridSize(4, 4)
        val helper = FolderAccessibilityHelper(cellLayout)

        val desc = helper.getLocationDescriptionForIconDrop(0)
        assertNotNull(desc)
        assertTrue("Description should mention position", desc.contains("1"))

        val target = helper.intersectsValidDropTarget(2)
        assertEquals(2, target)
    }

    @Test
    fun testWorkspaceAccessibilityHelperDescriptionForDropOver() {
        val targetItem = WorkspaceItemInfo().apply {
            title = "Target App"
            cellX = 2
            cellY = 3
        }
        val targetView = View(launcher).apply { tag = targetItem }

        val desc = WorkspaceAccessibilityHelper.getDescriptionForDropOver(
            targetView,
            launcher,
            "Page 1"
        )
        assertNotNull(desc)
        assertTrue(desc.contains("Target App"))

        val folderItem = FolderInfo().apply {
            title = "Games"
            cellX = 0
            cellY = 0
        }
        val folderView = View(launcher).apply { tag = folderItem }
        val folderDesc = WorkspaceAccessibilityHelper.getDescriptionForDropOver(
            folderView,
            launcher,
            "Page 1"
        )
        assertNotNull(folderDesc)
        assertTrue(folderDesc.contains("Games"))
    }
}
