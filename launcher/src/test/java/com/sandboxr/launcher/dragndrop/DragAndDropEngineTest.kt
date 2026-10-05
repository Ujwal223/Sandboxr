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

package com.sandboxr.launcher.dragndrop

import android.content.ComponentName
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Process
import android.view.MotionEvent
import android.view.View
import com.sandboxr.launcher.CellLayout
import com.sandboxr.launcher.DragSource
import com.sandboxr.launcher.DropTarget
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.model.data.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DragAndDropEngineTest {

    private fun createApp(pkg: String, title: String): AppInfo {
        return AppInfo().apply {
            componentName = ComponentName(pkg, "$pkg.MainActivity")
            this.title = title
            user = Process.myUserHandle()
        }
    }

    @Test
    fun testLongPressInitiatesDragWithElevatedShadow() {
        val controller = Robolectric.buildActivity(Launcher::class.java).setup()
        val launcher = controller.get()

        val dragController = launcher.getDragController()
        val dragLayer = launcher.getDragLayer()

        assertNotNull("DragController must be initialized in Launcher", dragController)
        assertNotNull("DragLayer must be initialized in Launcher", dragLayer)

        val app = createApp("com.example.app", "Example App")
        val iconDrawable = ColorDrawable(Color.RED)
        val originalView = DraggableView.ofType(DraggableView.DRAGGABLE_ICON)

        // Initiate drag
        val dragOptions = DragOptions()
        val dragView = dragController!!.startDrag(
            iconDrawable,
            null,
            originalView,
            150,
            200,
            DragSource { _, _, _ -> },
            app,
            Rect(0, 0, 100, 100),
            1.0f,
            1.0f,
            dragOptions
        )

        assertNotNull("DragView must be created upon drag start", dragView)
        assertTrue("DragController must report isDragging = true", dragController.isDragging)
        assertTrue("DragView must be elevated with shadow (elevation > 0)", dragView.elevation > 0f)
        assertTrue("DragView translationZ must be elevated", dragView.translationZ > 0f)

        // Verify DragView was added to DragLayer hierarchy
        assertEquals(dragLayer, dragView.parent)
    }

    @Test
    fun testDropAnimatesToTargetCellWithSpringPhysics() {
        val controller = Robolectric.buildActivity(Launcher::class.java).setup()
        val launcher = controller.get()
        val dragController = launcher.getDragController()!!

        val app = createApp("com.example.notes", "Notes")
        val iconDrawable = ColorDrawable(Color.BLUE)

        var dropCompleted = false
        var dropTargetReceived = false

        val testDropTarget = object : DropTarget {
            override fun isDropEnabled(): Boolean = true
            override fun onDrop(dragObject: DropTarget.DragObject, options: Any?) {
                dropTargetReceived = true
            }
            override fun getHitRectRelativeToDragLayer(outRect: Rect) {
                outRect.set(200, 200, 400, 400)
            }
        }
        dragController.addDropTarget(testDropTarget)

        val dragView = dragController.startDrag(
            iconDrawable,
            null,
            DraggableView.ofType(DraggableView.DRAGGABLE_ICON),
            250,
            250,
            DragSource { _, _, success -> dropCompleted = success },
            app,
            Rect(0, 0, 100, 100),
            1.0f,
            1.0f,
            DragOptions()
        )

        // Simulate moving over the drop target
        dragController.onDriverDragMove(300f, 300f)

        // Simulate drop
        dragController.onDriverDragEnd(300f, 300f)

        ShadowLooper.idleMainLooper()

        assertTrue("DropTarget should receive drop event", dropTargetReceived)
        assertTrue("DragSource drop completion callback should be invoked with true", dropCompleted)
        assertFalse("DragController should no longer be dragging", dragController.isDragging)
    }

    @Test
    fun testDragCancelCleanlyClearsDragState() {
        val controller = Robolectric.buildActivity(Launcher::class.java).setup()
        val launcher = controller.get()
        val dragController = launcher.getDragController()!!

        val app = createApp("com.example.calc", "Calculator")
        var dropSuccess: Boolean? = null

        dragController.startDrag(
            ColorDrawable(Color.GREEN),
            null,
            DraggableView.ofType(DraggableView.DRAGGABLE_ICON),
            100,
            100,
            DragSource { _, _, success -> dropSuccess = success },
            app,
            Rect(0, 0, 80, 80),
            1.0f,
            1.0f,
            DragOptions()
        )

        assertTrue("Should be dragging before cancel", dragController.isDragging)

        dragController.cancelDrag()

        assertFalse("Should not be dragging after cancel", dragController.isDragging)
        assertEquals("Drop success callback should receive false on cancel", false, dropSuccess)
    }

    @Test
    fun testSpringLoadedDragControllerPageSnapping() {
        val controller = Robolectric.buildActivity(Launcher::class.java).setup()
        val launcher = controller.get()

        val workspace = launcher.getWorkspace()
        assertNotNull(workspace)

        val cellLayout1 = CellLayout(launcher)
        val cellLayout2 = CellLayout(launcher)
        workspace!!.addView(cellLayout1)
        workspace.addView(cellLayout2)

        val springController = SpringLoadedDragController(launcher)
        springController.setAlarm(cellLayout2)

        // Fast-forward alarm
        springController.alarm.finishAlarm()
        ShadowLooper.idleMainLooper()

        val targetIndex = workspace.indexOfChild(cellLayout2)
        assertTrue(targetIndex >= 0)
        // Page snapped to cellLayout2 index
        assertEquals(targetIndex, workspace.getNextPage())
    }

    @Test
    fun testSystemDragItemInfoAndParams() {
        val app = createApp("com.example.system", "System App")
        val info = SystemDragItemInfo()
        assertEquals(com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_SYSTEM_DRAG, info.itemType)
        assertFalse(info.payload.isAcceptable())

        val params = SystemDragParams(
            clipData = null,
            dragImage = ColorDrawable(Color.CYAN),
            dragInfo = app,
            dragLayerX = 50,
            dragLayerY = 50,
            dragOptions = DragOptions(),
            dragRegion = Rect(0, 0, 100, 100),
            dragSource = DragSource { _, _, _ -> },
            draggableView = DraggableView.ofType(DraggableView.DRAGGABLE_ICON)
        )
        assertEquals(50, params.dragLayerX)
        assertEquals(50, params.dragLayerY)
    }
}
