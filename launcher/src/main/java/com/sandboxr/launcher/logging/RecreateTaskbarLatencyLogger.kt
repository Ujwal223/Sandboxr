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

package com.sandboxr.launcher.logging

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.sandboxr.launcher.logging.StatsLogManager.LauncherLatencyEvent.LAUNCHER_LATENCY_RECREATE_TASKBAR

/**
 * Tracks latency recreating taskbar. Merges back-to-back latency windows within 5ms.
 */
class RecreateTaskbarLatencyLogger(
    private val handler: Handler = Handler(Looper.getMainLooper())
) {

    private var startTime: Long = 0
    private var pendingRunnable: Runnable? = null

    /** Starts tracking the latency. */
    fun logStart() {
        if (pendingRunnable != null) {
            handler.removeCallbacks(pendingRunnable!!)
            pendingRunnable = null
        } else if (startTime == 0L) {
            startTime = SystemClock.elapsedRealtime()
        }
    }

    /** Ends tracking the latency and schedules the log after a short delay. */
    fun logEnd(statsLogManager: StatsLogManager) {
        if (startTime == 0L) return

        val endTime = SystemClock.elapsedRealtime()
        val duration = endTime - startTime

        pendingRunnable = Runnable {
            statsLogManager
                .latencyLogger()
                .withLatency(duration)
                .log(LAUNCHER_LATENCY_RECREATE_TASKBAR)

            startTime = 0
            pendingRunnable = null
        }

        handler.postDelayed(pendingRunnable!!, MERGE_DELAY_MS)
    }

    companion object {
        private const val MERGE_DELAY_MS = 5L
    }
}
