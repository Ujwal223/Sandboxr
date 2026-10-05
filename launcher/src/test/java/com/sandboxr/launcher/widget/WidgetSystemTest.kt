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

package com.sandboxr.launcher.widget

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.os.UserHandle
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.LauncherAppState
import com.android.launcher3.model.WidgetItem
import com.android.launcher3.widget.LauncherAppWidgetProviderInfo as Launcher3WidgetProviderInfo
import com.android.launcher3.widget.model.WidgetsListBaseEntriesBuilder
import com.android.launcher3.widget.model.WidgetsListHeaderEntry
import com.sandboxr.launcher.CellLayout
import com.sandboxr.launcher.InvariantDeviceProfile
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.celllayout.CellLayoutLayoutParams
import com.sandboxr.launcher.dragndrop.DraggableView
import com.sandboxr.launcher.model.data.LauncherAppWidgetInfo
import com.sandboxr.launcher.model.data.PackageItemInfo
import com.sandboxr.launcher.widgetpicker.WidgetCell
import com.sandboxr.launcher.widgetpicker.WidgetsFullSheet
import com.sandboxr.launcher.widgetpicker.WidgetsListAdapter
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
class WidgetSystemTest {

    private lateinit var launcher: Launcher

    @Before
    fun setup() {
        val controller = Robolectric.buildActivity(Launcher::class.java).setup()
        launcher = controller.get()
    }

    @Test
    fun testLauncherAppWidgetProviderInfoSpans() {
        val providerInfo = LauncherAppWidgetProviderInfo().apply {
            provider = ComponentName("com.example.app", "com.example.app.Widget")
            minWidth = 140
            minHeight = 140
            minResizeWidth = 70
            minResizeHeight = 70
            maxResizeWidth = 280
            maxResizeHeight = 280
            resizeMode = AppWidgetProviderInfo.RESIZE_BOTH
        }

        val idp = InvariantDeviceProfile.INSTANCE(launcher)
        providerInfo.initSpans(launcher, idp)

        assertEquals(2, providerInfo.spanX)
        assertEquals(2, providerInfo.spanY)
        assertEquals(1, providerInfo.minSpanX)
        assertEquals(1, providerInfo.minSpanY)
        assertEquals(4, providerInfo.maxSpanX)
        assertEquals(4, providerInfo.maxSpanY)

        val minSpans = providerInfo.minSpans
        assertEquals(1, minSpans.x)
        assertEquals(1, minSpans.y)
    }

    @Test
    fun testPendingAddWidgetInfo() {
        val providerInfo = LauncherAppWidgetProviderInfo().apply {
            provider = ComponentName("com.example.app", "com.example.app.Widget")
            spanX = 3
            spanY = 2
            minSpanX = 2
            minSpanY = 1
            maxSpanX = 4
            maxSpanY = 3
        }

        val pendingInfo = PendingAddWidgetInfo(providerInfo, -100)
        assertEquals(3, pendingInfo.spanX)
        assertEquals(2, pendingInfo.spanY)
        assertEquals(2, pendingInfo.minSpanX)
        assertEquals(1, pendingInfo.minSpanY)
        assertEquals(4, pendingInfo.maxSpanX)
        assertEquals(3, pendingInfo.maxSpanY)
        assertEquals("com.example.app", pendingInfo.componentName.packageName)

        val cloned = pendingInfo.clone()
        assertEquals(cloned.spanX, pendingInfo.spanX)
        assertEquals(cloned.spanY, pendingInfo.spanY)
    }

    @Test
    fun testWidgetsListBaseEntriesBuilder() {
        val builder = WidgetsListBaseEntriesBuilder(launcher)
        val widgetsMap = HashMap<PackageItemInfo, List<WidgetItem>>()

        val pkgA = PackageItemInfo("com.alpha.app", UserHandle.getUserHandleForUid(0)).apply {
            title = "Alpha App"
        }
        val pkgB = PackageItemInfo("com.beta.app", UserHandle.getUserHandleForUid(0)).apply {
            title = "Beta App"
        }

        val widgetInfoA = Launcher3WidgetProviderInfo().apply {
            provider = ComponentName("com.alpha.app", "com.alpha.app.Widget")
            spanX = 2
            spanY = 2
        }
        val itemA = WidgetItem(
            widgetInfoA,
            InvariantDeviceProfile.INSTANCE(launcher),
            LauncherAppState.getInstance(launcher).getIconCache(),
            launcher
        )

        val widgetInfoB = Launcher3WidgetProviderInfo().apply {
            provider = ComponentName("com.beta.app", "com.beta.app.Widget")
            spanX = 4
            spanY = 1
        }
        val itemB = WidgetItem(
            widgetInfoB,
            InvariantDeviceProfile.INSTANCE(launcher),
            LauncherAppState.getInstance(launcher).getIconCache(),
            launcher
        )

        widgetsMap[pkgB] = listOf(itemB)
        widgetsMap[pkgA] = listOf(itemA)

        val entries = builder.build(widgetsMap)
        assertEquals(2, entries.size)
        // Check sorted alphabetically: Alpha then Beta
        assertEquals("Alpha App", entries[0].mPkgItem.title.toString())
        assertEquals("Beta App", entries[1].mPkgItem.title.toString())
        assertEquals("A", entries[0].mTitleSectionName)
        assertEquals("B", entries[1].mTitleSectionName)
    }

    @Test
    fun testWidgetCellBindingAndType() {
        val cell = WidgetCell(launcher)
        assertEquals(DraggableView.DRAGGABLE_WIDGET, cell.getViewType())

        val widgetInfo = Launcher3WidgetProviderInfo().apply {
            provider = ComponentName("com.sample.widget", "com.sample.widget.Provider")
            spanX = 3
            spanY = 2
        }
        val item = WidgetItem(
            widgetInfo,
            InvariantDeviceProfile.INSTANCE(launcher),
            LauncherAppState.getInstance(launcher).getIconCache(),
            launcher
        )

        cell.applyFromCellItem(item, null)
        assertNotNull(cell.pendingInfo)
        assertEquals(3, cell.pendingInfo.spanX)
        assertEquals(2, cell.pendingInfo.spanY)
        assertEquals(cell.pendingInfo, cell.tag)
    }

    @Test
    fun testWidgetsListAdapterExpandAndFilter() {
        val adapter = WidgetsListAdapter(launcher, null)

        val pkg = PackageItemInfo("com.test.music", UserHandle.getUserHandleForUid(0)).apply {
            title = "Music Player"
        }
        val widgetInfo = Launcher3WidgetProviderInfo().apply {
            provider = ComponentName("com.test.music", "com.test.music.PlaybackWidget")
            spanX = 4
            spanY = 2
        }
        val item = WidgetItem(
            widgetInfo,
            InvariantDeviceProfile.INSTANCE(launcher),
            LauncherAppState.getInstance(launcher).getIconCache(),
            launcher
        )

        val headerEntry = WidgetsListHeaderEntry(pkg, "M", listOf(item), false)
        adapter.setWidgets(listOf(headerEntry))

        // Initially collapsed: 1 item (header only)
        assertEquals(1, adapter.itemCount)
        assertEquals(WidgetsListAdapter.VIEW_TYPE_HEADER, adapter.getItemViewType(0))

        // When filtered: automatically expands
        adapter.setFilter("Music")
        assertEquals(2, adapter.itemCount) // header + content
        assertEquals(WidgetsListAdapter.VIEW_TYPE_HEADER, adapter.getItemViewType(0))
        assertEquals(WidgetsListAdapter.VIEW_TYPE_CONTENT, adapter.getItemViewType(1))

        // When search does not match
        adapter.setFilter("NonExistentXYZ")
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun testAppWidgetResizeFrameSnapToGrid() {
        val cellLayout = CellLayout(launcher)
        val dragLayer = launcher.getDragLayer() ?: return

        val widgetInfo = LauncherAppWidgetInfo(1, ComponentName("com.test", "com.test.Widget")).apply {
            spanX = 2
            spanY = 2
        }
        val providerInfo = LauncherAppWidgetProviderInfo().apply {
            provider = ComponentName("com.test", "com.test.Widget")
            spanX = 2
            spanY = 2
            minSpanX = 1
            minSpanY = 1
            maxSpanX = 4
            maxSpanY = 4
        }

        val hostView = LauncherAppWidgetHostView(launcher)
        hostView.setAppWidget(1, providerInfo)
        hostView.tag = widgetInfo

        val clp = CellLayoutLayoutParams(0, 0, 2, 2)
        hostView.layoutParams = clp

        AppWidgetResizeFrame.showForWidget(hostView, cellLayout)

        val openFrame = AbstractFloatingView.getOpenView<AppWidgetResizeFrame>(
            launcher, AbstractFloatingView.TYPE_WIDGET_RESIZE_FRAME
        )
        assertNotNull(openFrame)
        assertEquals(2, openFrame.spanX)
        assertEquals(2, openFrame.spanY)
        assertTrue(openFrame.isOpen)

        openFrame.close(false)
        assertFalse(openFrame.isOpen)
    }

    @Test
    fun testWidgetsFullSheetLifecycle() {
        val sheet = WidgetsFullSheet.show(launcher, false)
        assertNotNull(sheet)
        assertTrue(sheet.isOpen)
        assertTrue(AbstractFloatingView.hasOpenView(launcher, AbstractFloatingView.TYPE_WIDGETS_BOTTOM_SHEET))

        sheet.close(false)
        assertFalse(sheet.isOpen)
    }
}
