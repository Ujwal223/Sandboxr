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

package com.sandboxr.launcher.dragndrop

import android.content.ComponentName
import android.content.Intent
import android.graphics.Rect
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.sandboxr.launcher.DeleteDropTarget
import com.sandboxr.launcher.DropTarget
import com.sandboxr.launcher.DropTargetBar
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.R
import com.sandboxr.launcher.SecondaryDropTarget
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.ItemInfoWithIcon
import com.sandboxr.launcher.model.data.LauncherAppWidgetInfo
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
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DropTargetsTest {

    private lateinit var launcher: Launcher
    private lateinit var dropTargetBar: DropTargetBar
    private lateinit var deleteTarget: DeleteDropTarget
    private lateinit var secondaryTarget: SecondaryDropTarget
    private lateinit var dragController: LauncherDragController

    @Before
    fun setup() {
        val activityController = Robolectric.buildActivity(Launcher::class.java).setup()
        launcher = activityController.get()
        dragController = launcher.getDragController()!!
        dropTargetBar = launcher.getDropTargetBar()!!
        deleteTarget = dropTargetBar.findViewById(R.id.delete_target_text)
        secondaryTarget = dropTargetBar.findViewById(R.id.uninstall_target_text)
    }

    @Test
    fun testDropTargetBarSlideDownOnDragStartAndSlideUpOnDragEnd() {
        // Initially, drop target bar is invisible / hidden
        assertFalse(dropTargetBar.isBarVisible)

        val item = WorkspaceItemInfo().apply {
            id = 101
            title = "Test App"
        }
        val dragObject = DropTarget.DragObject().apply {
            dragInfo = item
        }

        // On drag start: drop target bar slides down and becomes visible
        dropTargetBar.onDragStart(dragObject, DragOptions())
        assertTrue(dropTargetBar.isBarVisible)
        assertEquals(View.VISIBLE, dropTargetBar.visibility)

        // On drag end: drop target bar slides back up
        dropTargetBar.onDragEnd()
        assertFalse(dropTargetBar.isBarVisible)
    }

    @Test
    fun testDeleteDropTargetLabelTransitionsAndAcceptance() {
        val removableItem = WorkspaceItemInfo().apply {
            id = 202
            title = "Removable Item"
        }
        val removableDragObject = DropTarget.DragObject().apply {
            dragInfo = removableItem
        }

        // Removable item should display "Remove"
        deleteTarget.onDragStart(removableDragObject, DragOptions())
        assertTrue(deleteTarget.isDropEnabled())
        assertTrue(deleteTarget.acceptDrop(removableDragObject))
        assertEquals(
            launcher.getString(R.string.remove_drop_target_label),
            deleteTarget.text.toString()
        )

        // Non-removable item (id == NO_ID) should display "Cancel"
        val nonRemovableItem = AppInfo().apply {
            id = ItemInfo.NO_ID
            title = "App From Drawer"
        }
        val nonRemovableDragObject = DropTarget.DragObject().apply {
            dragInfo = nonRemovableItem
        }

        deleteTarget.onDragStart(nonRemovableDragObject, DragOptions())
        assertEquals(
            launcher.getString(R.string.cancel_drop_target_label),
            deleteTarget.text.toString()
        )
    }

    @Test
    fun testDeleteDropTargetHoverFeedbackAndCompletion() {
        val item = WorkspaceItemInfo().apply {
            id = 303
            title = "App to Delete"
            cellX = 0
            cellY = 0
            screenId = 0
        }
        val dragObject = DropTarget.DragObject().apply {
            dragInfo = item
        }

        deleteTarget.onDragStart(dragObject, DragOptions())

        // Hover enter applies visual feedback
        deleteTarget.onDragEnter(dragObject)
        assertTrue(deleteTarget.isSelected)
        assertNotNull(deleteTarget.background)

        // Hover exit clears visual feedback
        deleteTarget.onDragExit(dragObject)
        assertFalse(deleteTarget.isSelected)

        // Drop on delete target triggers complete drop
        deleteTarget.onDrop(dragObject, null)
        assertFalse(deleteTarget.isSelected)
    }

    @Test
    fun testSecondaryDropTargetAdaptsBetweenUninstallAndAppInfo() {
        // Third-party app -> UNINSTALL mode
        val thirdPartyApp = AppInfo().apply {
            componentName = ComponentName("com.example.thirdparty", "com.example.thirdparty.MainActivity")
            runtimeStatusFlags = ItemInfoWithIcon.FLAG_SYSTEM_NO
        }
        val thirdPartyDragObject = DropTarget.DragObject().apply {
            dragInfo = thirdPartyApp
        }

        secondaryTarget.onDragStart(thirdPartyDragObject, DragOptions())
        assertEquals(SecondaryDropTarget.TargetMode.UNINSTALL, secondaryTarget.currentMode)
        assertEquals(
            launcher.getString(R.string.uninstall_drop_target_label),
            secondaryTarget.text.toString()
        )
        assertTrue(secondaryTarget.isDropEnabled())

        // System app -> APP_INFO mode
        val systemApp = AppInfo().apply {
            componentName = ComponentName("com.android.settings", "com.android.settings.Settings")
            runtimeStatusFlags = ItemInfoWithIcon.FLAG_SYSTEM_YES
        }
        val systemDragObject = DropTarget.DragObject().apply {
            dragInfo = systemApp
        }

        secondaryTarget.onDragStart(systemDragObject, DragOptions())
        assertEquals(SecondaryDropTarget.TargetMode.APP_INFO, secondaryTarget.currentMode)
        assertEquals(
            launcher.getString(R.string.app_info_drop_target_label),
            secondaryTarget.text.toString()
        )

        // Configurable widget -> RECONFIGURE mode
        val widgetInfo = LauncherAppWidgetInfo().apply {
            appWidgetId = 42
        }
        val widgetDragObject = DropTarget.DragObject().apply {
            dragInfo = widgetInfo
        }

        secondaryTarget.onDragStart(widgetDragObject, DragOptions())
        assertEquals(SecondaryDropTarget.TargetMode.RECONFIGURE, secondaryTarget.currentMode)
        assertEquals(
            launcher.getString(R.string.reconfigure_drop_target_label),
            secondaryTarget.text.toString()
        )
    }

    @Test
    fun testDropTargetBarDeferOnDragEndKeepsBarVisible() {
        val item = WorkspaceItemInfo().apply {
            id = 505
            title = "Deferred Item"
        }
        val dragObject = DropTarget.DragObject().apply {
            dragInfo = item
        }

        dropTargetBar.onDragStart(dragObject, DragOptions())
        assertTrue(dropTargetBar.isBarVisible)

        // Drop on delete target triggers deferOnDragEnd()
        deleteTarget.onDrop(dragObject, null)

        // Immediate onDragEnd does NOT hide the bar because deferOnDragEnd was set
        dropTargetBar.onDragEnd()
        assertTrue(dropTargetBar.isBarVisible)

        // Subsequent onDragEnd hides the bar
        dropTargetBar.onDragEnd()
        assertFalse(dropTargetBar.isBarVisible)
    }
}
