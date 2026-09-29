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

package com.sandboxr.launcher.allapps

import android.content.ComponentName
import android.content.Context
import android.graphics.Rect
import android.os.Process
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.sandboxr.launcher.DeviceProfile
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.model.data.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AllAppsContainerViewTest {

    private lateinit var launcher: Launcher
    private lateinit var containerView: LauncherAllAppsContainerView

    @Before
    fun setUp() {
        val controller = Robolectric.buildActivity(Launcher::class.java).setup()
        launcher = controller.get()
        containerView = LauncherAllAppsContainerView(launcher).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    @Test
    fun testViewHierarchyInitialization() {
        assertNotNull(containerView.getRecyclerView())
        assertNotNull(containerView.getFastScroller())
        assertNotNull(containerView.getFastScrollerPopup())
        assertNotNull(containerView.getSearchContainer())
        assertNotNull(containerView.getAppsList())
        assertNotNull(containerView.getGridAdapter())
    }

    @Test
    fun testSetInsets() {
        val insets = Rect(0, 80, 0, 120)
        containerView.setInsets(insets)

        val rv = containerView.getRecyclerView()
        assertTrue(rv.paddingTop > insets.top)
        assertEquals(insets.bottom + 16, rv.paddingBottom)
    }

    @Test
    fun testFastScrollProgressMapping() {
        val store = containerView.getAppsStore()
        val appA = createApp("com.test.a", "Apple")
        val appB = createApp("com.test.b", "Banana")
        val appC = createApp("com.test.c", "Cherry")
        store.setApps(arrayOf(appA, appB, appC))

        val rv = containerView.getRecyclerView()
        // Scrub to start: fraction 0.0 should map to "A"
        val firstSection = rv.scrollToPositionAtProgress(0.0f)
        assertEquals("A", firstSection.toString())

        // Scrub to middle/end
        val lastSection = rv.scrollToPositionAtProgress(1.0f)
        assertEquals("C", lastSection.toString())
    }

    @Test
    fun testLauncherStateTransitionAllApps() {
        launcher.showAllApps(false)
        assertEquals(LauncherState.ALL_APPS, launcher.getStateManager().currentStableState)
        val appsView = launcher.getAppsView()
        assertNotNull(appsView)
        assertEquals(View.VISIBLE, appsView?.visibility)
    }

    private fun createApp(pkg: String, title: String): AppInfo {
        return AppInfo().apply {
            componentName = ComponentName(pkg, "$pkg.MainActivity")
            user = Process.myUserHandle()
            this.title = title
            this.appTitle = title
        }
    }
}
