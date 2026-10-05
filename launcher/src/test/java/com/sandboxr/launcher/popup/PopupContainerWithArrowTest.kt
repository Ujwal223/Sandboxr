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

package com.sandboxr.launcher.popup

import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import com.sandboxr.launcher.AbstractFloatingView
import com.sandboxr.launcher.R
import com.sandboxr.launcher.TestBaseActivity
import com.sandboxr.launcher.icons.BitmapInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.shortcuts.DeepShortcutView
import org.junit.Assert.assertEquals
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
class PopupContainerWithArrowTest {

    private lateinit var activity: TestBaseActivity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(TestBaseActivity::class.java).setup().get()
        activity.setTheme(R.style.AppTheme)
    }

    @Test
    fun testPopupContainerWithArrowInstantiation() {
        val originalView = View(activity)
        val itemInfo = WorkspaceItemInfo()
        val container = PopupContainerWithArrow.create<TestBaseActivity>(activity, originalView, itemInfo)
        assertNotNull(container)
        assertEquals(originalView, container.originalIcon)
        assertEquals(itemInfo, container.itemInfo)
        assertTrue(container.isOfType(AbstractFloatingView.TYPE_ACTION_POPUP))
    }

    @Test
    fun testDeepShortcutViewPopulation() {
        val deepShortcutView = DeepShortcutView(activity)
        val info = WorkspaceItemInfo().apply {
            title = "Test Shortcut"
            intent = Intent(Intent.ACTION_MAIN)
            bitmap = BitmapInfo.fromBitmap(Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888))
        }

        deepShortcutView.applyShortcutInfo(info, null)
        assertEquals(info, deepShortcutView.finalInfo)
        assertEquals(info, deepShortcutView.tag)
    }

    @Test
    fun testPopupPopulatorSortAndFilterShortcuts() {
        val filtered = PopupPopulator.sortAndFilterShortcuts(emptyList())
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun testPopupContainerProperties() {
        val originalView = View(activity)
        val itemInfo = WorkspaceItemInfo()
        val container = PopupContainerWithArrow.create<TestBaseActivity>(activity, originalView, itemInfo)
        assertNotNull(container.itemClickListener)
        assertNotNull(container.layoutParams)
    }
}
