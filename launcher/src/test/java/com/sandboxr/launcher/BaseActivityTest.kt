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

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import com.sandboxr.launcher.views.ActivityContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.PrintWriter
import java.io.StringWriter

class TestBaseActivity : BaseActivity() {
    override fun getRootView(): View? = window?.decorView
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BaseActivityTest {

    @Test
    fun testBaseActivityLifecycleAndFlags() {
        val controller = Robolectric.buildActivity(TestBaseActivity::class.java)
        val activity = controller.get()

        assertEquals(Lifecycle.State.INITIALIZED, activity.lifecycle.currentState)
        assertFalse(activity.isStarted())
        assertFalse(activity.hasBeenResumed())

        controller.create()
        assertEquals(Lifecycle.State.CREATED, activity.lifecycle.currentState)

        var startedCallbackRan = false
        var resumedCallbackRan = false
        var stoppedCallbackRan = false
        var destroyedCallbackRan = false

        activity.addEventCallback(BaseActivity.EVENT_STARTED) { startedCallbackRan = true }
        activity.addEventCallback(BaseActivity.EVENT_RESUMED) { resumedCallbackRan = true }
        activity.addEventCallback(BaseActivity.EVENT_STOPPED) { stoppedCallbackRan = true }
        activity.addEventCallback(BaseActivity.EVENT_DESTROYED) { destroyedCallbackRan = true }

        controller.start()
        assertEquals(Lifecycle.State.STARTED, activity.lifecycle.currentState)
        assertTrue(activity.isStarted())
        assertTrue(startedCallbackRan)

        controller.resume()
        assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
        assertTrue(activity.hasBeenResumed())
        assertTrue(activity.isUserActive())
        assertTrue(resumedCallbackRan)

        controller.pause()
        assertFalse(activity.hasBeenResumed())

        controller.stop()
        assertFalse(activity.isStarted())
        assertFalse(activity.isUserActive())
        assertTrue(stoppedCallbackRan)

        controller.destroy()
        assertEquals(Lifecycle.State.DESTROYED, activity.lifecycle.currentState)
        assertTrue(destroyedCallbackRan)
    }

    @Test
    fun testInvisibilityFlags() {
        val controller = Robolectric.buildActivity(TestBaseActivity::class.java).setup()
        val activity = controller.get()

        assertFalse(activity.isForceInvisible())

        activity.addForceInvisibleFlag(BaseActivity.INVISIBLE_BY_STATE_HANDLER)
        assertTrue(activity.isForceInvisible())
        assertTrue(activity.hasSomeInvisibleFlag(BaseActivity.INVISIBLE_BY_STATE_HANDLER))

        activity.clearForceInvisibleFlag(BaseActivity.INVISIBLE_BY_STATE_HANDLER)
        assertFalse(activity.isForceInvisible())
    }

    @Test
    fun testLookupContextHelper() {
        val controller = Robolectric.buildActivity(TestBaseActivity::class.java).setup()
        val activity = controller.get()

        val found: TestBaseActivity = ActivityContext.lookupContext(activity)
        assertNotNull(found)
        assertEquals(activity, found)

        val foundBase: TestBaseActivity = BaseActivity.fromContext(activity)
        assertEquals(activity, foundBase)
    }

    @Test
    fun testDumpMiscDoesNotCrash() {
        val controller = Robolectric.buildActivity(TestBaseActivity::class.java).setup()
        val activity = controller.get()

        val sw = StringWriter()
        activity.dumpMisc("  ", PrintWriter(sw))
        val dump = sw.toString()
        assertTrue(dump.contains("deviceProfile"))
        assertTrue(dump.contains("mActivityFlags"))
    }
}
