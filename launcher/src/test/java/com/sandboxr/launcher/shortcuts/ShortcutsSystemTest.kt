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

package com.sandboxr.launcher.shortcuts

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Process
import android.view.View
import com.sandboxr.launcher.R
import com.sandboxr.launcher.TestBaseActivity
import com.sandboxr.launcher.icons.BitmapInfo
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
class ShortcutsSystemTest {

    private lateinit var activity: TestBaseActivity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(TestBaseActivity::class.java).setup().get()
        activity.setTheme(R.style.AppTheme)
    }

    @Test
    fun testShortcutKeyFromPackageAndId() {
        val user = Process.myUserHandle()
        val key = ShortcutKey("com.example.app", user, "shortcut_compose")

        assertEquals("com.example.app", key.packageName)
        assertEquals("shortcut_compose", key.id)
        assertEquals(user, key.user)
    }

    @Test
    fun testShortcutKeyMakeIntent() {
        val intent = ShortcutKey.makeIntent("shortcut_compose", "com.example.app")

        assertEquals("com.example.app", intent.`package`)
        assertEquals("shortcut_compose", intent.getStringExtra(ShortcutKey.EXTRA_SHORTCUT_ID))
        assertTrue(intent.categories.contains("com.android.launcher3.DEEP_SHORTCUT"))
    }

    @Test
    fun testShortcutKeyFromIntent() {
        val user = Process.myUserHandle()
        val intent = ShortcutKey.makeIntent("shortcut_compose", "com.example.app")
        val key = ShortcutKey.fromIntent(intent, user)

        assertEquals("com.example.app", key.packageName)
        assertEquals("shortcut_compose", key.id)
        assertEquals(user, key.user)
    }

    @Test
    fun testShortcutRequestBuilding() {
        val user = Process.myUserHandle()
        val request = ShortcutRequest(activity, user)
            .forPackage("com.example.app", "id_1", "id_2")
            .withContainer(ComponentName("com.example.app", "com.example.app.MainActivity"))

        assertNotNull(request)
        val queryResult = request.query(ShortcutRequest.PUBLISHED)
        assertNotNull(queryResult)
    }

    @Test
    fun testDeepShortcutViewBinding() {
        val view = DeepShortcutView(activity)
        val info = WorkspaceItemInfo().apply {
            title = "Test Shortcut"
            intent = Intent(Intent.ACTION_MAIN)
            bitmap = BitmapInfo.fromBitmap(Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888))
        }

        view.applyShortcutInfo(info, null)
        assertEquals(info, view.finalInfo)
        assertEquals(info, view.tag)

        val center = view.iconCenter
        assertNotNull(center)
    }

    @Test
    fun testDeepShortcutTextViewLoadingState() {
        val textView = DeepShortcutTextView(activity)
        assertNotNull(textView)

        textView.text = "Direct Messages"
        assertEquals("Direct Messages", textView.text.toString())
    }

    @Test
    fun testShortcutDragPreviewProviderDrawableCreation() {
        val dummyView = View(activity).apply {
            layout(0, 0, 48, 48)
        }
        val shift = Point(10, 10)
        val provider = ShortcutDragPreviewProvider(dummyView, shift)

        assertNotNull(provider)
        assertNotNull(provider.view)
        assertEquals(dummyView, provider.view)
    }
}
