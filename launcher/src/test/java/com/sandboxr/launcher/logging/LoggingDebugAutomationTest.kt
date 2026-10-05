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

package com.sandboxr.launcher.logging

import android.content.Context
import android.os.Bundle
import android.os.Process
import android.view.KeyEvent
import android.view.MotionEvent
import org.robolectric.RuntimeEnvironment
import com.android.launcher3.testing.shared.TestProtocol
import com.sandboxr.launcher.automation.AutomationNoOpRepository
import com.sandboxr.launcher.automation.AutomationTestHelper
import com.sandboxr.launcher.debug.DebugTools
import com.sandboxr.launcher.debug.FlagDebugUtils
import com.sandboxr.launcher.debug.TestEventEmitter
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.testing.TestInformationHandler
import com.sandboxr.launcher.testing.TestInformationProvider
import com.sandboxr.launcher.testing.TestLogging
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LoggingDebugAutomationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        StatsLogManager.clearHistory()
        TestEventEmitter.clearEvents()
        TestLogging.clearEvents()
        DebugTools.setDebugOverride(true)
    }

    @After
    fun tearDown() {
        StatsLogManager.clearHistory()
        TestEventEmitter.clearEvents()
        TestLogging.clearEvents()
        DebugTools.setDebugOverride(false)
        DebugTools.setGridBoundsOverlayEnabled(false)
        DebugTools.setTouchVisualizerEnabled(false)
        DebugTools.setPerformanceLoggingEnabled(false)
    }

    @Test
    fun testStatsLogManagerCapturesUserInteractions() {
        val manager = StatsLogManager.newInstance(context)
        val dummyItem = object : ItemInfo() {
            init {
                id = 42
            }
        }

        // 1. Log app launch tap
        manager.logger()
            .withItemInfo(dummyItem)
            .withRank(2)
            .withSrcState(1)
            .withDstState(2)
            .log(StatsLogManager.LauncherEvent.LAUNCHER_APP_LAUNCH_TAP)

        // 2. Log swipe up gesture
        manager.logger()
            .log(StatsLogManager.LauncherEvent.LAUNCHER_ALLAPPS_SWIPE_UP)

        // 3. Log folder open
        manager.logger()
            .log(StatsLogManager.LauncherEvent.LAUNCHER_FOLDER_OPEN)

        val recorded = StatsLogManager.getRecordedEvents()
        assertEquals("Three events should be recorded", 3, recorded.size)

        val tapEvent = recorded[0]
        assertEquals(StatsLogManager.LauncherEvent.LAUNCHER_APP_LAUNCH_TAP, tapEvent.event)
        assertEquals(2, tapEvent.rank)
        assertEquals(1, tapEvent.srcState)
        assertEquals(2, tapEvent.dstState)
        assertEquals(dummyItem, tapEvent.itemInfo)

        assertTrue(StatsLogManager.hasLoggedEvent(StatsLogManager.LauncherEvent.LAUNCHER_ALLAPPS_SWIPE_UP))
        assertTrue(StatsLogManager.hasLoggedEvent(StatsLogManager.LauncherEvent.LAUNCHER_FOLDER_OPEN))
        assertFalse(StatsLogManager.hasLoggedEvent(StatsLogManager.LauncherEvent.LAUNCHER_WIDGET_DELETED))
    }

    @Test
    fun testStatsLogManagerLatencyTracking() {
        val manager = StatsLogManager.newInstance(context)

        manager.latencyLogger()
            .withLatency(45)
            .withType(StatsLogManager.StatsLatencyLogger.LatencyType.HOT)
            .log(StatsLogManager.LauncherLatencyEvent.LAUNCHER_LATENCY_APP_LAUNCH_ANIMATION)

        val latencies = StatsLogManager.getRecordedLatencies()
        assertEquals(1, latencies.size)
        assertEquals(StatsLogManager.LauncherLatencyEvent.LAUNCHER_LATENCY_APP_LAUNCH_ANIMATION, latencies[0].event)
        assertEquals(45L, latencies[0].latencyMs)
    }

    @Test
    fun testStartupLatencyLoggerColdStartup() {
        val statsManager = StatsLogManager.newInstance(context)
        var currentTime = 1000L

        val coldLogger = StartupLatencyLogger.ColdRebootStartupLogger(statsManager) { currentTime }
        coldLogger.logWorkspaceLoadStartTime()

        currentTime = 1250L
        val nextLogger = coldLogger.finishLogs(workspaceCount = 15, isBindSync = false)

        assertEquals("Next logger should be NoOpLogger", StartupLatencyLogger.NoOpLogger, nextLogger)
    }

    @Test
    fun testDebugToolsAccessibleInDebugBuilds() {
        assertTrue("Debug tools should be enabled via override in tests", DebugTools.isDebugBuild(context))

        // Verify overlay toggles
        assertFalse(DebugTools.isGridBoundsOverlayEnabled())
        DebugTools.setGridBoundsOverlayEnabled(true)
        assertTrue(DebugTools.isGridBoundsOverlayEnabled())

        assertFalse(DebugTools.isTouchVisualizerEnabled())
        DebugTools.setTouchVisualizerEnabled(true)
        assertTrue(DebugTools.isTouchVisualizerEnabled())

        assertFalse(DebugTools.isPerformanceLoggingEnabled())
        DebugTools.setPerformanceLoggingEnabled(true)
        assertTrue(DebugTools.isPerformanceLoggingEnabled())

        // Verify diagnostic summary
        val summary = DebugTools.getDiagnosticsSummary(context)
        assertNotNull(summary)
        assertTrue(summary.containsKey("debuggable"))
        assertTrue(summary.containsKey("memory_used_mb"))
        assertTrue(summary.containsKey("grid_bounds_overlay"))
        assertEquals(true, summary["grid_bounds_overlay"])

        // Verify dump
        val stringWriter = StringWriter()
        DebugTools.dump(PrintWriter(stringWriter), context)
        val dumpOutput = stringWriter.toString()
        assertTrue(dumpOutput.contains("SANDBOXR LAUNCHER DEBUG DIAGNOSTICS"))
        assertTrue(dumpOutput.contains("debuggable: true"))
    }

    @Test
    fun testTestEventEmitterAndListeners() {
        val eventReceived = AtomicBoolean(false)
        val listener = TestEventEmitter.EventListener { event ->
            if (event == TestEventEmitter.TestEvent.LAUNCHER_STATE_COMPLETED) {
                eventReceived.set(true)
            }
        }

        TestEventEmitter.registerListener(listener)
        assertFalse(TestEventEmitter.hasEmitted(TestEventEmitter.TestEvent.LAUNCHER_STATE_COMPLETED))

        TestEventEmitter.sendEvent(TestEventEmitter.TestEvent.LAUNCHER_STATE_COMPLETED)

        assertTrue(eventReceived.get())
        assertTrue(TestEventEmitter.hasEmitted(TestEventEmitter.TestEvent.LAUNCHER_STATE_COMPLETED))
        assertEquals(1, TestEventEmitter.getEmittedEvents().size)

        TestEventEmitter.unregisterListener(listener)
    }

    @Test
    fun testFlagDebugUtilsFormatting() {
        val FLAG_A = 1
        val FLAG_B = 2
        val FLAG_C = 4

        val serializer = java.util.function.IntFunction<String> { flag ->
            when (flag) {
                1 -> "FLAG_A"
                2 -> "FLAG_B"
                4 -> "FLAG_C"
                else -> "UNKNOWN"
            }
        }

        val previous = FLAG_A
        val current = FLAG_A or FLAG_B

        val result = FlagDebugUtils.formatFlagChange(current, previous, serializer)
        assertTrue(result.contains("+[FLAG_B]"))
    }

    @Test
    fun testAutomationRepositoryAndTestHelper() {
        val noOpRepo = AutomationNoOpRepository()
        assertFalse(noOpRepo.isPackageAutomated(Process.myUserHandle(), "com.test.app"))

        val helper = AutomationTestHelper()
        val user = Process.myUserHandle()
        val packageName = "com.sandboxr.testapp"

        assertFalse(helper.isPackageAutomated(user, packageName))

        helper.setPackageAutomated(user, packageName, true)
        assertTrue(helper.isPackageAutomated(user, packageName))

        helper.setPackageAutomated(user, packageName, false)
        assertFalse(helper.isPackageAutomated(user, packageName))
    }

    @Test
    fun testTestInformationProviderAndHandler() {
        val provider = TestInformationProvider()
        val proxy = provider.getProxy(context)
        assertNotNull("TestInformationProvider should provide proxy in debug build", proxy)

        // Test PID request
        val pidBundle = proxy?.call(TestProtocol.REQUEST_PID, null, null)
        assertNotNull(pidBundle)
        assertEquals(Process.myPid(), pidBundle?.getInt(TestProtocol.TEST_INFO_RESPONSE_FIELD))

        // Test GC request
        val gcBundle = proxy?.call(TestProtocol.REQUEST_FORCE_GC, null, null)
        assertNotNull(gcBundle)

        // Test diagnostics summary request
        val diagBundle = proxy?.call("request_diagnostics_summary", null, null)
        assertNotNull(diagBundle)
        assertTrue(diagBundle!!.containsKey("debuggable"))

        // Test event emission query
        TestEventEmitter.sendEvent(TestEventEmitter.TestEvent.ALL_APPS_SHOWN)
        val eventsBundle = proxy.call("request_get_emitted_test_events", null, null)
        assertNotNull(eventsBundle)
        val eventList = eventsBundle?.getStringArrayList(TestProtocol.TEST_INFO_RESPONSE_FIELD)
        assertTrue(eventList?.contains("ALL_APPS_SHOWN") == true)
    }

    @Test
    fun testTestLoggingCapturesTaplEvents() {
        val capturedEvents = mutableListOf<String>()
        TestLogging.setEventConsumer { seq, evt ->
            capturedEvents.add("$seq::$evt")
        }

        TestLogging.recordEvent("TEST_SEQ_1", "WORKSPACE_LOADED")
        assertEquals(1, capturedEvents.size)
        assertEquals("TEST_SEQ_1::WORKSPACE_LOADED", capturedEvents[0])

        // Verify key and motion event logging
        val downKey = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HOME)
        TestLogging.recordKeyEvent("KEY_SEQ", "HOME_KEY", downKey)

        val recorded = TestLogging.getRecordedEvents()
        assertTrue(recorded.any { it.contains("WORKSPACE_LOADED") })
        assertTrue(recorded.any { it.contains("HOME_KEY") })

        TestLogging.setEventConsumer(null)
    }
}
