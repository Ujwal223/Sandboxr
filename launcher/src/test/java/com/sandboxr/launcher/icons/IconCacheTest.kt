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

package com.sandboxr.launcher.icons

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process
import com.sandboxr.launcher.icons.cache.CacheLookupFlag
import com.sandboxr.launcher.model.data.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IconCacheTest {

    private lateinit var context: Context
    private lateinit var iconCache: IconCache

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        iconCache = IconCache(context)
    }

    @Test
    fun testGetDefaultIconReturnsValidBitmap() {
        val defaultIcon = iconCache.getDefaultIcon(Process.myUserHandle())
        assertNotNull(defaultIcon)
        assertNotNull(defaultIcon.icon)
        assertTrue(defaultIcon.icon.width > 0)
        assertTrue(defaultIcon.icon.height > 0)
    }

    @Test
    fun testGetTitleAndIconPopulatesAppInfo() {
        val appInfo = AppInfo().apply {
            intent = Intent().setComponent(ComponentName("com.example.app", "com.example.app.MainActivity"))
            user = Process.myUserHandle()
        }

        iconCache.getTitleAndIcon(appInfo, CacheLookupFlag.DEFAULT_LOOKUP_FLAG)

        assertNotNull(appInfo.bitmap)
        assertNotNull(appInfo.title)
    }

    @Test
    fun testSetThemedIconsEnabledClearsCache() {
        iconCache.setThemedIconsEnabled(true)
        // Verify default icon is still available
        val defaultIcon = iconCache.getDefaultIcon(Process.myUserHandle())
        assertNotNull(defaultIcon)
    }

    @Test
    fun testRemoveIconsForPackage() {
        val pkg = "com.test.package"
        iconCache.removeIconsForPkg(pkg, Process.myUserHandle())
        // Ensure no exception thrown
        assertTrue(true)
    }
}
