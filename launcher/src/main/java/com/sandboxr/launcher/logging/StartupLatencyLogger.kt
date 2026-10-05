/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.util.SparseLongArray
import androidx.annotation.VisibleForTesting
import com.sandboxr.launcher.logging.StatsLogManager.LauncherLatencyEvent
import com.sandboxr.launcher.logging.StatsLogManager.LauncherLatencyEvent.LAUNCHER_LATENCY_STARTUP_TOTAL_DURATION
import com.sandboxr.launcher.logging.StatsLogManager.LauncherLatencyEvent.LAUNCHER_LATENCY_STARTUP_WORKSPACE_LOADER_ASYNC
import com.sandboxr.launcher.util.Executors

/** Interface to log launcher startup latency metrics. */
sealed interface StartupLatencyLogger {

    fun logWorkspaceLoadStartTime() {}

    /** Notes the end of an event. Final logs are pushed on [finishLogs] */
    fun logStart(event: LauncherLatencyEvent) {}

    /** Notes the start of an event. Final logs are pushed on [finishLogs] */
    fun logEnd(event: LauncherLatencyEvent) {}

    /**
     * Finishes the current logging session and returns a new logger to be used for the next session
     */
    fun finishLogs(workspaceCount: Int, isBindSync: Boolean): StartupLatencyLogger = this

    object NoOpLogger : StartupLatencyLogger

    @VisibleForTesting
    class ColdRebootStartupLogger(
        private val statsLogManager: StatsLogManager,
        private val timeProvider: () -> Long = { SystemClock.elapsedRealtime() }
    ) : StartupLatencyLogger {

        @VisibleForTesting val startTimeByEvent = SparseLongArray()
        @VisibleForTesting val endTimeByEvent = SparseLongArray()

        private var cardinality: Int = -1

        init {
            logStart(LAUNCHER_LATENCY_STARTUP_TOTAL_DURATION)
        }

        override fun logWorkspaceLoadStartTime() =
            logStart(LAUNCHER_LATENCY_STARTUP_WORKSPACE_LOADER_ASYNC)

        override fun logStart(event: LauncherLatencyEvent) {
            startTimeByEvent.put(event.id, timeProvider.invoke())
        }

        override fun logEnd(event: LauncherLatencyEvent) {
            endTimeByEvent.put(event.id, timeProvider.invoke())
        }

        override fun finishLogs(workspaceCount: Int, isBindSync: Boolean): StartupLatencyLogger {
            if (!isBindSync) {
                cardinality = workspaceCount
                logEnd(LAUNCHER_LATENCY_STARTUP_WORKSPACE_LOADER_ASYNC)
            }

            Executors.MAIN_EXECUTOR.handler.postAtFrontOfQueue {
                Log.i(
                    TAG,
                    "LauncherReady. User: ${Process.myUserHandle()} TS: ${SystemClock.uptimeMillis()}"
                )
                logEnd(LAUNCHER_LATENCY_STARTUP_TOTAL_DURATION)
                commitLogs()
            }
            return NoOpLogger
        }

        private fun commitLogs() {
            val latencyLogger = statsLogManager.latencyLogger()
            for (i in 0 until startTimeByEvent.size()) {
                val eventId = startTimeByEvent.keyAt(i)
                val startTime = startTimeByEvent.valueAt(i)
                val endTime = endTimeByEvent.get(eventId, 0L)
                if (endTime > startTime) {
                    val duration = endTime - startTime
                    val event = LauncherLatencyEvent.values().firstOrNull { it.id == eventId }
                    if (event != null) {
                        latencyLogger
                            .withLatency(duration)
                            .withType(StatsLogManager.StatsLatencyLogger.LatencyType.COLD)
                            .log(event)
                    }
                }
            }
        }

        companion object {
            private const val TAG = "StartupLatencyLogger"
        }
    }
}
