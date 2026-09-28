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

package com.sandboxr.launcher

import android.content.Context
import android.view.View
import com.sandboxr.launcher.celllayout.CellLayoutLayoutParams
import com.sandboxr.launcher.celllayout.CellPosMapper
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.pageindicators.PageIndicatorDots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkspaceTest {

    private lateinit var context: Context
    private lateinit var workspace: Workspace<PageIndicatorDots>
    private lateinit var pageIndicator: PageIndicatorDots

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        workspace = Workspace(context)
        pageIndicator = PageIndicatorDots(context)
        workspace.setPageIndicator(pageIndicator)

        // Measure and layout workspace with 1080x1920 dimension
        val wSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val hSpec = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        workspace.measure(wSpec, hSpec)
        workspace.layout(0, 0, 1080, 1920)
    }

    @Test
    fun testInitialWorkspaceHasFirstScreen() {
        assertEquals("Initial workspace must have at least 1 page", 1, workspace.pageCount)
        assertNotNull("First screen ID 0 must exist", workspace.getScreenWithId(Workspace.FIRST_SCREEN_ID))
        assertEquals(0, workspace.getCurrentPage())
    }

    @Test
    fun testMultiplePagesRenderingAndScreenManagement() {
        // Add screen 1 and screen 2
        val screen1 = workspace.insertNewWorkspaceScreen(1)
        val screen2 = workspace.insertNewWorkspaceScreen(2)

        assertNotNull(screen1)
        assertNotNull(screen2)
        assertEquals(3, workspace.pageCount)

        // Verify screen order
        assertEquals(0, workspace.getScreenIdForPageIndex(0))
        assertEquals(1, workspace.getScreenIdForPageIndex(1))
        assertEquals(2, workspace.getScreenIdForPageIndex(2))

        assertEquals(0, workspace.getPageIndexForScreenId(0))
        assertEquals(1, workspace.getPageIndexForScreenId(1))
        assertEquals(2, workspace.getPageIndexForScreenId(2))

        // Measure and layout with 3 screens
        val wSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val hSpec = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        workspace.measure(wSpec, hSpec)
        workspace.layout(0, 0, 1080, 1920)

        // Screen 0 at x=0..1080, Screen 1 at x=1080..2160, Screen 2 at x=2160..3240
        assertEquals(0, workspace.getPageAt(0)?.left)
        assertEquals(1080, workspace.getPageAt(1)?.left)
        assertEquals(2160, workspace.getPageAt(2)?.left)

        // Remove screen 1
        workspace.removeWorkspaceScreen(1)
        assertEquals(2, workspace.pageCount)
        assertNull("Screen 1 should be removed", workspace.getScreenWithId(1))
        assertEquals(2, workspace.getScreenIdForPageIndex(1))
    }

    @Test
    fun testPageNavigationAndSnapPhysics() {
        // Add 2 additional screens (total 3 pages)
        workspace.insertNewWorkspaceScreen(1)
        workspace.insertNewWorkspaceScreen(2)

        val wSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val hSpec = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        workspace.measure(wSpec, hSpec)
        workspace.layout(0, 0, 1080, 1920)

        assertEquals(0, workspace.getCurrentPage())

        // Snap to page 1
        val snapped = workspace.snapToPage(1)
        assertTrue("snapToPage should return true", snapped)
        assertEquals("Next page must be 1 while snapping", 1, workspace.getNextPage())

        // Set current page directly
        workspace.setCurrentPage(2)
        assertEquals(2, workspace.getCurrentPage())
        assertEquals(2160, workspace.scrollX)

        // Snap to destination from scroll offset 1100 (closest to page 1 at x=1080)
        workspace.scrollTo(1100, 0)
        assertEquals("Destination page closest to scroll 1100 should be 1", 1, workspace.getDestinationPage())
    }

    @Test
    fun testWorkspaceLayoutManagerItemPlacement() {
        val dummyView = View(context)
        val itemInfo = WorkspaceItemInfo().apply {
            id = 42
            container = LauncherSettings.Favorites.CONTAINER_DESKTOP
            screenId = Workspace.FIRST_SCREEN_ID
            cellX = 1
            cellY = 2
            spanX = 1
            spanY = 1
        }
        dummyView.tag = itemInfo

        // Add item using WorkspaceLayoutManager
        workspace.addInScreen(dummyView, itemInfo)

        val screen0 = workspace.getScreenWithId(Workspace.FIRST_SCREEN_ID)
        assertNotNull(screen0)

        val container = screen0?.shortcutsAndWidgets
        assertNotNull(container)
        assertEquals(1, container?.childCount)
        assertEquals(dummyView, container?.getChildAt(0))

        val lp = dummyView.layoutParams as? CellLayoutLayoutParams
        assertNotNull(lp)
        assertEquals(1, lp?.cellX)
        assertEquals(2, lp?.cellY)
        assertEquals(1, lp?.cellHSpan)
        assertEquals(1, lp?.cellVSpan)

        // Verify occupied cell in CellLayout grid
        assertTrue("Cell (1,2) must be occupied", screen0?.getOccupied()?.isRegionVacant(1, 2, 1, 1) == false)
    }

    @Test
    fun testSpringLoadedModeScalingAndExtraEmptyScreens() {
        assertEquals(1, workspace.pageCount)

        // Enter spring loaded mode
        workspace.enterSpringLoadedMode()

        // Should have added an extra empty screen
        assertTrue("Should have extra empty screen", workspace.mWorkspaceScreens.containsKey(Workspace.EXTRA_EMPTY_SCREEN_ID))
        assertEquals(2, workspace.pageCount)

        // Exit spring loaded mode
        workspace.exitSpringLoadedMode()

        // Should have removed the extra empty screen
        assertFalse("Extra empty screen should be removed", workspace.mWorkspaceScreens.containsKey(Workspace.EXTRA_EMPTY_SCREEN_ID))
        assertEquals(1, workspace.pageCount)
    }

    @Test
    fun testLauncherStateTransitions() {
        workspace.setState(LauncherState.NORMAL)
        assertEquals(View.VISIBLE, workspace.visibility)
        assertEquals(1f, workspace.alpha, 0.01f)
        assertEquals(1f, workspace.workspaceScale, 0.01f)

        workspace.setState(LauncherState.ALL_APPS)
        assertEquals(View.GONE, workspace.visibility)
        assertEquals(0f, workspace.alpha, 0.01f)

        workspace.setState(LauncherState.OVERVIEW)
        assertEquals(View.VISIBLE, workspace.visibility)
        assertEquals(0.8f, workspace.workspaceScale, 0.01f)

        workspace.setState(LauncherState.NORMAL)
        assertEquals(View.VISIBLE, workspace.visibility)
        assertEquals(1f, workspace.workspaceScale, 0.01f)
    }

    @Test
    fun testCellPosMapper() {
        val mapper = CellPosMapper(false, 5)
        val info = WorkspaceItemInfo().apply {
            cellX = 2
            cellY = 3
            screenId = 0
            container = LauncherSettings.Favorites.CONTAINER_DESKTOP
        }

        val presenter = mapper.mapModelToPresenter(info)
        assertEquals(2, presenter.cellX)
        assertEquals(3, presenter.cellY)
        assertEquals(0, presenter.screenId)

        val model = mapper.mapPresenterToModel(2, 3, 0, LauncherSettings.Favorites.CONTAINER_DESKTOP)
        assertEquals(2, model.cellX)
        assertEquals(3, model.cellY)
        assertEquals(0, model.screenId)
    }
}
