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

package com.sandboxr.launcher.sysuiconnection

import android.os.Binder
import com.sandboxr.launcher.uioverrides.QuickstepLauncher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SysUIConnectionAndQuickstepTest {

    @Test
    fun testSysUIConnectionTrackerLifecycle() {
        val tracker = SysUIConnectionTracker.get()
        assertNotNull(tracker)

        var lastState: SysUIConnectionState? = null
        val listener: (SysUIConnectionState) -> Unit = { state ->
            lastState = state
        }

        tracker.addListener(listener)

        val binder = Binder()
        tracker.onConnected(binder, 0x1234L, 0)

        assertTrue(tracker.connectionState.value.isConnected)
        assertEquals(binder, tracker.connectionState.value.windowToken)
        assertEquals(0x1234L, tracker.connectionState.value.systemUiFlags)
        assertEquals(lastState?.windowToken, binder)

        tracker.onDisconnected()
        assertFalse(tracker.connectionState.value.isConnected)

        tracker.removeListener(listener)
    }

    @Test
    fun testQuickstepLauncherInstantiation() {
        val controller = Robolectric.buildActivity(QuickstepLauncher::class.java).create()
        val activity = controller.get()
        assertNotNull(activity)
    }
}
