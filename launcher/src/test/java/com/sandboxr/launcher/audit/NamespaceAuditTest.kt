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

package com.sandboxr.launcher.audit

import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherApplication
import com.sandboxr.launcher.uioverrides.QuickstepLauncher
import com.sandboxr.launcher.virtual.VirtualAppInfo
import com.sandboxr.launcher.virtual.VirtualEnvironmentBridge
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NamespaceAuditTest {

    @Test
    fun testCoreLauncherPackages() {
        val launcherClass = Launcher::class.java
        assertTrue(
            "Launcher must reside in com.sandboxr.launcher",
            launcherClass.`package`?.name?.startsWith("com.sandboxr.launcher") == true
        )

        val quickstepClass = QuickstepLauncher::class.java
        assertTrue(
            "QuickstepLauncher must reside in com.sandboxr.launcher.uioverrides",
            quickstepClass.`package`?.name?.startsWith("com.sandboxr.launcher") == true
        )

        val appClass = LauncherApplication::class.java
        assertTrue(
            "LauncherApplication must reside in com.sandboxr.launcher",
            appClass.`package`?.name?.startsWith("com.sandboxr.launcher") == true
        )
    }

    @Test
    fun testVirtualPackageNamespace() {
        val virtualAppClass = VirtualAppInfo::class.java
        assertTrue(
            "VirtualAppInfo must reside in com.sandboxr.launcher.virtual",
            virtualAppClass.`package`?.name == "com.sandboxr.launcher.virtual"
        )

        val bridgeClass = VirtualEnvironmentBridge::class.java
        assertTrue(
            "VirtualEnvironmentBridge must reside in com.sandboxr.launcher.virtual",
            bridgeClass.`package`?.name == "com.sandboxr.launcher.virtual"
        )
    }

    @Test
    fun testZeroTelemetryAndZeroTrackers() {
        // Assert that Google Play Services analytics or proprietary trackers are absent
        try {
            Class.forName("com.google.android.gms.analytics.GoogleAnalytics")
            org.junit.Assert.fail("GoogleAnalytics should not exist in FOSS Sandboxr Core")
        } catch (_: ClassNotFoundException) {
            // Success: Clean FOSS build
        }

        try {
            Class.forName("com.google.firebase.analytics.FirebaseAnalytics")
            org.junit.Assert.fail("FirebaseAnalytics should not exist in FOSS Sandboxr Core")
        } catch (_: ClassNotFoundException) {
            // Success: Clean FOSS build
        }
    }
}
