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

package com.sandboxr.launcher.debug

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
import com.sandboxr.launcher.InvariantDeviceProfile
import com.sandboxr.launcher.logging.StatsLogManager
import java.io.PrintWriter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Suite of developer diagnostic utilities, visual debug overlays, and runtime inspection tools.
 * Accessible when running in debug builds or when explicitly enabled via developer settings.
 */
object DebugTools {

    private val sDebugOverride = AtomicBoolean(false)
    private val sGridBoundsOverlayEnabled = AtomicBoolean(false)
    private val sTouchVisualizerEnabled = AtomicBoolean(false)
    private val sPerformanceLoggingEnabled = AtomicBoolean(false)

    /**
     * Returns true if the application is currently running in a debuggable build
     * or if debug mode has been forced on via developer settings.
     */
    @JvmStatic
    fun isDebugBuild(context: Context): Boolean {
        if (sDebugOverride.get()) return true
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /** Forces debug tools on or off (useful for test harnesses and automation). */
    @JvmStatic
    fun setDebugOverride(enabled: Boolean) {
        sDebugOverride.set(enabled)
    }

    /** Returns whether grid cell boundaries overlay is enabled. */
    @JvmStatic
    fun isGridBoundsOverlayEnabled(): Boolean = sGridBoundsOverlayEnabled.get()

    /** Toggles the grid cell boundaries overlay on homescreen / workspace. */
    @JvmStatic
    fun setGridBoundsOverlayEnabled(enabled: Boolean) {
        sGridBoundsOverlayEnabled.set(enabled)
    }

    /** Returns whether the touch gesture visualizer overlay is active. */
    @JvmStatic
    fun isTouchVisualizerEnabled(): Boolean = sTouchVisualizerEnabled.get()

    /** Toggles the touch gesture visualizer overlay. */
    @JvmStatic
    fun setTouchVisualizerEnabled(enabled: Boolean) {
        sTouchVisualizerEnabled.set(enabled)
    }

    /** Returns whether performance latency logging is active. */
    @JvmStatic
    fun isPerformanceLoggingEnabled(): Boolean = sPerformanceLoggingEnabled.get()

    /** Toggles detailed performance latency logging. */
    @JvmStatic
    fun setPerformanceLoggingEnabled(enabled: Boolean) {
        sPerformanceLoggingEnabled.set(enabled)
    }

    /** Collects a rich diagnostic summary of the launcher's current runtime state. */
    @JvmStatic
    fun getDiagnosticsSummary(context: Context): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemMb = runtime.maxMemory() / (1024 * 1024)

        val idp = try {
            InvariantDeviceProfile.INSTANCE(context)
        } catch (e: Exception) {
            null
        }

        return linkedMapOf(
            "debuggable" to isDebugBuild(context),
            "pid" to Process.myPid(),
            "android_version" to Build.VERSION.RELEASE,
            "sdk_int" to Build.VERSION.SDK_INT,
            "memory_used_mb" to usedMemMb,
            "memory_max_mb" to maxMemMb,
            "grid_columns" to (idp?.numColumns ?: -1),
            "grid_rows" to (idp?.numRows ?: -1),
            "grid_bounds_overlay" to sGridBoundsOverlayEnabled.get(),
            "touch_visualizer" to sTouchVisualizerEnabled.get(),
            "recorded_events_count" to StatsLogManager.getRecordedEvents().size,
            "emitted_test_events_count" to TestEventEmitter.getEmittedEvents().size
        )
    }

    /** Dumps complete debug state into the provided writer. */
    @JvmStatic
    fun dump(writer: PrintWriter, context: Context) {
        writer.println("=== SANDBOXR LAUNCHER DEBUG DIAGNOSTICS ===")
        getDiagnosticsSummary(context).forEach { (key, value) ->
            writer.println("  $key: $value")
        }
        writer.println("===========================================")
    }
}
