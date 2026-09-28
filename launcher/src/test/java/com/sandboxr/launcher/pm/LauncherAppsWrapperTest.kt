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

package com.sandboxr.launcher.pm

import android.content.Context
import android.os.Process
import com.sandboxr.launcher.compat.LauncherAppsCompat
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
class LauncherAppsWrapperTest {

    private lateinit var context: Context
    private lateinit var userCache: UserCache
    private lateinit var compat: LauncherAppsCompat
    private lateinit var wrapper: LauncherAppsWrapper

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        userCache = UserCache.getInstance(context)
        compat = LauncherAppsCompat(context, userCache)
        wrapper = LauncherAppsWrapper(context, userCache, compat)
    }

    @Test
    fun testGetPersonalAppsNotNull() {
        val personalApps = wrapper.getPersonalApps()
        assertNotNull(personalApps)
    }

    @Test
    fun testWorkProfileIsolationWhenNoWorkProfile() {
        // Without work profile, work apps list is empty
        val workApps = wrapper.getWorkProfileApps()
        assertNotNull(workApps)
        assertTrue(workApps.isEmpty())
    }

    @Test
    fun testPrivateSpaceIsolationWhenNoPrivateSpace() {
        // Without private space, private apps list is empty
        val privateApps = wrapper.getPrivateSpaceApps()
        assertNotNull(privateApps)
        assertTrue(privateApps.isEmpty())
    }

    @Test
    fun testHasProfilesOnDefaultDevice() {
        // Default device environment has personal profile (myUserHandle)
        assertFalse(wrapper.hasWorkProfile())
        assertFalse(wrapper.hasPrivateSpace())
    }
}
