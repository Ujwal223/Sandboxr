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

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
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
class LauncherTest {

    @Test
    fun testLauncherLaunchesWithEmptyWorkspace() {
        val controller = Robolectric.buildActivity(Launcher::class.java).setup()
        val launcher = controller.get()

        assertNotNull(launcher)
        assertEquals(Lifecycle.State.RESUMED, launcher.lifecycle.currentState)

        // Verify root views and empty workspace hierarchy
        assertNotNull("RootView must be non-null", launcher.getRootView())
        assertNotNull("DragLayer must be non-null", launcher.getDragLayer())
        assertNotNull("Workspace must be non-null", launcher.getWorkspace())
        assertNotNull("Hotseat must be non-null", launcher.getHotseat())
        assertNotNull("AppsView must be non-null", launcher.getAppsView())
        assertNotNull("ScrimView must be non-null", launcher.getScrimView())

        // Initial state must be NORMAL
        assertTrue("Launcher should start in NORMAL state", launcher.isInState(LauncherState.NORMAL))
        assertEquals(View.VISIBLE, launcher.getWorkspace()?.visibility)
        assertEquals(View.VISIBLE, launcher.getHotseat()?.visibility)
        assertEquals(View.GONE, launcher.getAppsView()?.visibility)
    }

    @Test
    fun testStateTransitionsWithoutNPE() {
        val controller = Robolectric.buildActivity(Launcher::class.java).setup()
        val launcher = controller.get()

        // 1. Transition to ALL_APPS
        launcher.showAllApps(animated = false)
        assertTrue("Launcher should be in ALL_APPS", launcher.isInState(LauncherState.ALL_APPS))
        assertEquals(View.VISIBLE, launcher.getAppsView()?.visibility)
        assertEquals(View.GONE, launcher.getWorkspace()?.visibility)

        // 2. Transition to OVERVIEW
        launcher.showOverview(animated = false)
        assertTrue("Launcher should be in OVERVIEW", launcher.isInState(LauncherState.OVERVIEW))
        assertEquals(View.GONE, launcher.getAppsView()?.visibility)
        assertEquals(View.GONE, launcher.getWorkspace()?.visibility)

        // 3. Transition to SPRING_LOADED
        launcher.getStateManager().goToState(LauncherState.SPRING_LOADED, animated = false)
        assertTrue("Launcher should be in SPRING_LOADED", launcher.isInState(LauncherState.SPRING_LOADED))
        assertEquals(View.VISIBLE, launcher.getWorkspace()?.visibility)

        // 4. Transition to EDIT_MODE
        launcher.getStateManager().goToState(LauncherState.EDIT_MODE, animated = false)
        assertTrue("Launcher should be in EDIT_MODE", launcher.isInState(LauncherState.EDIT_MODE))

        // 5. Transition back to NORMAL
        launcher.showHomeScreen(animated = false)
        assertTrue("Launcher should return to NORMAL", launcher.isInState(LauncherState.NORMAL))
        assertEquals(View.VISIBLE, launcher.getWorkspace()?.visibility)
        assertEquals(View.VISIBLE, launcher.getHotseat()?.visibility)
        assertEquals(View.GONE, launcher.getAppsView()?.visibility)
    }

    @Test
    fun testBackPressNavigationBehavior() {
        val controller = Robolectric.buildActivity(Launcher::class.java).setup()
        val launcher = controller.get()

        // In ALL_APPS: back press returns true and transitions to NORMAL
        launcher.showAllApps(animated = false)
        assertTrue(launcher.isInState(LauncherState.ALL_APPS))
        val handledFromAllApps = launcher.onBackPressedLauncher()
        assertTrue("Back press should be handled in ALL_APPS", handledFromAllApps)
        assertTrue("State should be reset to NORMAL", launcher.isInState(LauncherState.NORMAL))

        // In OVERVIEW: back press returns true and transitions to NORMAL
        launcher.showOverview(animated = false)
        assertTrue(launcher.isInState(LauncherState.OVERVIEW))
        val handledFromOverview = launcher.onBackPressedLauncher()
        assertTrue("Back press should be handled in OVERVIEW", handledFromOverview)
        assertTrue("State should be reset to NORMAL", launcher.isInState(LauncherState.NORMAL))

        // In NORMAL: back press returns false (home screen does not exit)
        val handledAtHome = launcher.onBackPressedLauncher()
        assertFalse("Back press should not be handled at root NORMAL state", handledAtHome)
        assertTrue("State remains NORMAL", launcher.isInState(LauncherState.NORMAL))
    }

    @Test
    fun testHomeIntentReturnsToNormalState() {
        val controller = Robolectric.buildActivity(Launcher::class.java).setup()
        val launcher = controller.get()

        launcher.showAllApps(animated = false)
        assertTrue(launcher.isInState(LauncherState.ALL_APPS))

        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        controller.newIntent(homeIntent)

        assertTrue("Home intent must reset launcher to NORMAL state", launcher.isInState(LauncherState.NORMAL))
    }

    @Test
    fun testStateRestorationAcrossActivityRecreate() {
        val controller = Robolectric.buildActivity(Launcher::class.java).setup()
        val launcher = controller.get()

        launcher.showAllApps(animated = false)
        assertTrue(launcher.isInState(LauncherState.ALL_APPS))

        val bundle = Bundle()
        controller.saveInstanceState(bundle)

        val newController = Robolectric.buildActivity(Launcher::class.java)
        newController.create(bundle).start().restoreInstanceState(bundle).resume()
        val restoredLauncher = newController.get()

        assertTrue("Launcher should restore to ALL_APPS state", restoredLauncher.isInState(LauncherState.ALL_APPS))
    }

    @Test
    fun testConfigurationAndIdpChangeReappliesUi() {
        val controller = Robolectric.buildActivity(Launcher::class.java).setup()
        val launcher = controller.get()

        val newConfig = Configuration(launcher.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        }
        controller.configurationChange(newConfig)
        assertNotNull(launcher.getDeviceProfile())

        launcher.onIdpChanged(true)
        assertNotNull(launcher.getDeviceProfile())
    }
}
