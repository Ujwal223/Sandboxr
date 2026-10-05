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

package com.sandboxr.launcher.folder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.view.View
import android.view.inputmethod.EditorInfo
import com.sandboxr.launcher.DropTarget
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.icons.BitmapInfo
import com.sandboxr.launcher.model.data.FolderInfo
import com.sandboxr.launcher.model.data.ItemInfo
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FolderSystemTest {

    private lateinit var launcher: Launcher

    @Before
    fun setup() {
        val controller = Robolectric.buildActivity(Launcher::class.java).setup()
        launcher = controller.get()
    }

    @Test
    fun testFolderGridOrganizerCalculations() {
        val organizer = FolderGridOrganizer(4, 4)
        organizer.calculateGridSize(8)
        assertEquals(3, organizer.countX)
        assertEquals(3, organizer.countY)
        assertEquals(9, organizer.maxItemsPerPage)
        assertEquals(1, organizer.getNumPages(8))

        // Multi-page calculation
        organizer.calculateGridSize(16)
        assertEquals(4, organizer.countX)
        assertEquals(4, organizer.countY)
        assertEquals(16, organizer.maxItemsPerPage)
        assertEquals(2, organizer.getNumPages(20))

        val pt = Point()
        organizer.getPosForRank(5, pt)
        assertEquals(1, pt.x) // 5 % 4
        assertEquals(1, pt.y) // 5 / 4
    }

    @Test
    fun testFolderInfoListenerLifecycle() {
        val folderInfo = FolderInfo().apply {
            title = "Utilities"
        }

        var addCalled = false
        var removeCalled = false
        var titleChanged = false

        folderInfo.addListener(object : FolderInfo.FolderListener {
            override fun onAdd(item: ItemInfo, rank: Int) {
                addCalled = true
            }

            override fun onRemove(items: MutableList<ItemInfo>) {
                removeCalled = true
            }

            override fun onTitleChanged(title: CharSequence) {
                titleChanged = true
            }

            override fun onItemsChanged(animate: Boolean) {}
        })

        val item1 = WorkspaceItemInfo().apply {
            id = 1
            title = "Browser"
        }
        val item2 = WorkspaceItemInfo().apply {
            id = 2
            title = "Files"
        }

        folderInfo.add(item1)
        assertTrue("onAdd callback must fire on item addition", addCalled)
        assertEquals(1, folderInfo.getContents().size)

        folderInfo.add(item2)
        assertEquals(2, folderInfo.getContents().size)

        folderInfo.remove(item1, false)
        assertTrue("onRemove callback must fire on item removal", removeCalled)
        assertEquals(1, folderInfo.getContents().size)

        folderInfo.setTitle("Tools", null)
        assertTrue("onTitleChanged callback must fire on title change", titleChanged)
        assertEquals("Tools", folderInfo.title.toString())
    }

    @Test
    fun testPreviewBackgroundAcceptAndRest() {
        val background = PreviewBackground()
        val dummyView = View(launcher)
        background.setup(launcher, dummyView, 60)

        assertEquals(30, background.radius)
        assertFalse(background.isAccepting)

        background.animateToAccept()
        assertTrue(background.isAccepting)

        background.animateToRest()
        assertFalse(background.isAccepting)
    }

    @Test
    fun testFolderIconCreationAndPreviewRendering() {
        val folderInfo = FolderInfo().apply {
            title = "Productivity"
        }
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val bitmapInfo = BitmapInfo.fromBitmap(bitmap)

        val item1 = WorkspaceItemInfo().apply {
            id = 10
            title = "App 1"
            this.bitmap = bitmapInfo
        }
        val item2 = WorkspaceItemInfo().apply {
            id = 11
            title = "App 2"
            this.bitmap = bitmapInfo
        }
        folderInfo.add(item1)
        folderInfo.add(item2)

        val folderIcon = FolderIcon.inflateFolderIcon(launcher, null, folderInfo)
        assertNotNull(folderIcon)
        assertEquals("Productivity", folderIcon.text.toString())

        // Measure and draw folder icon
        folderIcon.measure(
            View.MeasureSpec.makeMeasureSpec(120, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(120, View.MeasureSpec.EXACTLY)
        )
        folderIcon.layout(0, 0, 120, 120)

        val canvasBitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)
        folderIcon.draw(canvas)

        // Drag enter / exit visual feedback
        val dragObject = DropTarget.DragObject().apply {
            dragInfo = WorkspaceItemInfo().apply { id = 12 }
        }
        folderIcon.onDragEnter(dragObject)
        assertTrue(folderIcon.previewBackground.isAccepting)

        folderIcon.onDragExit(dragObject)
        assertFalse(folderIcon.previewBackground.isAccepting)
    }

    @Test
    fun testFolderNameEditTextCommit() {
        val editText = FolderNameEditText(launcher)
        var committedTitle: String? = null
        editText.setOnTitleChangeListener { newTitle ->
            committedTitle = newTitle.toString()
        }

        editText.setText("New Folder")
        editText.onEditorAction(editText, EditorInfo.IME_ACTION_DONE, null)
        assertEquals("New Folder", committedTitle)
    }

    @Test
    fun testFolderOpenAndCloseWithClipReveal() {
        val folderInfo = FolderInfo().apply {
            title = "My Games"
        }
        val folderIcon = FolderIcon.inflateFolderIcon(launcher, null, folderInfo)
        val folder = Folder.fromXml(launcher)
        folder.setFolderIcon(folderIcon)
        folder.bind(folderInfo)

        assertFalse(folder.isOpen)

        // Open folder dialog
        folder.open()
        assertTrue(folder.isOpen)

        // Verify folder contents bound
        assertEquals(folderInfo, folder.info)
        assertNotNull(folder.content)

        // Close folder dialog
        folder.close(false)
        assertFalse(folder.isOpen)
    }

    @Test
    fun testDroppingIconOntoIconCreatesFolderWithSpringAnimation() {
        // Create 2 workspace icons
        val targetIconInfo = WorkspaceItemInfo().apply {
            id = 1001
            title = "App A"
            cellX = 0
            cellY = 0
        }
        val draggedIconInfo = WorkspaceItemInfo().apply {
            id = 1002
            title = "App B"
            cellX = 1
            cellY = 0
        }

        // Simulate folder creation: combining two apps into a FolderInfo
        val newFolderInfo = FolderInfo().apply {
            title = "Folder"
            cellX = targetIconInfo.cellX
            cellY = targetIconInfo.cellY
        }
        newFolderInfo.add(targetIconInfo)
        newFolderInfo.add(draggedIconInfo)

        val folderIcon = FolderIcon.inflateFolderIcon(launcher, null, newFolderInfo)
        assertEquals(2, newFolderInfo.getContents().size)
        assertEquals("Folder", folderIcon.text.toString())

        // Hover feedback onto the newly created folder
        val dragObject = DropTarget.DragObject().apply {
            dragInfo = WorkspaceItemInfo().apply {
                id = 1003
                title = "App C"
            }
        }
        assertTrue(folderIcon.acceptDrop(dragObject))

        folderIcon.onDrop(dragObject, null)
        assertEquals(3, newFolderInfo.getContents().size)
    }
}
