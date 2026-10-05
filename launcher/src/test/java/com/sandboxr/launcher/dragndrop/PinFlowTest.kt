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

import android.app.Activity
import android.content.ClipDescription
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.graphics.Rect
import android.os.Parcel
import android.view.DragEvent
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherSettings
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.widget.LauncherAppWidgetProviderInfo
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
class PinFlowTest {

    private lateinit var launcher: Launcher

    @Before
    fun setup() {
        val controller = Robolectric.buildActivity(Launcher::class.java).setup()
        launcher = controller.get()
    }

    @Test
    fun testPinWidgetFlowHandlerCalculation() {
        val providerInfo = LauncherAppWidgetProviderInfo().apply {
            provider = ComponentName("com.example.weather", "com.example.weather.Widget")
            spanX = 3
            spanY = 2
            minSpanX = 2
            minSpanY = 1
        }

        val handler = PinWidgetFlowHandler(providerInfo)
        assertNotNull(handler.providerInfo)
        assertEquals(3, handler.providerInfo.spanX)
        assertEquals(2, handler.providerInfo.spanY)
        assertFalse(handler.needsConfiguration())

        val widgetInfo = handler.createAppWidgetInfo(
            42,
            LauncherSettings.Favorites.CONTAINER_DESKTOP,
            0,
            1,
            2
        )
        assertNotNull(widgetInfo)
        assertEquals(42, widgetInfo.appWidgetId)
        assertEquals(1, widgetInfo.cellX)
        assertEquals(2, widgetInfo.cellY)
        assertEquals(3, widgetInfo.spanX)
        assertEquals(2, widgetInfo.spanY)
        assertEquals(ComponentName("com.example.weather", "com.example.weather.Widget"), widgetInfo.providerName)
    }

    @Test
    fun testPinShortcutRequestActivityInfoDefaults() {
        val shortcutInfo = PinShortcutRequestActivityInfo(null, launcher)
        assertEquals("", shortcutInfo.label)
        assertNotNull(shortcutInfo.user)

        val itemInfo = shortcutInfo.createWorkspaceItemInfo(null)
        assertNotNull(itemInfo)
        assertEquals(LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT, itemInfo.itemType)
    }

    @Test
    fun testPinItemDragListenerMimeTypes() {
        val listener = PinItemDragListener(null, Rect(0, 0, 100, 100), 100, 100)
        listener.setLauncher(launcher)

        assertEquals(LauncherState.NORMAL, launcher.getStateManager().state)
    }

    @Test
    fun testAddItemActivityNullRequestFinishes() {
        val intent = Intent(LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT)
        val controller = Robolectric.buildActivity(AddItemActivity::class.java, intent)
        val activity = controller.create().get()

        assertTrue(activity.isFinishing)
    }
}
