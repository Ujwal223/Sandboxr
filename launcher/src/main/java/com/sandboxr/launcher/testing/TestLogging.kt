/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.sandboxr.launcher.testing

import android.app.ActivityManager
import android.util.Log
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.VisibleForTesting
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.BiConsumer

/**
 * Event logging bridge for TAPL (Test Automation Protocol for Launcher) and UI Automator.
 */
object TestLogging {

    private const val TAPL_EVENTS_TAG = "TaplEvents"
    private const val LAUNCHER_EVENTS_TAG = "LauncherEvents"

    private var sEventConsumer: BiConsumer<String, String>? = null
    private var sHadEventsNotFromTest = false
    private var sEnableRegisterEventNotFromTest = false

    private val sRecordedEvents = CopyOnWriteArrayList<String>()

    @JvmStatic
    fun isRunningInTestHarness(): Boolean {
        return ActivityManager.isRunningInTestHarness() || sEnableRegisterEventNotFromTest || sEventConsumer != null
    }

    private fun recordEventSlow(sequence: String, event: String, reportToTapl: Boolean) {
        Log.d(if (reportToTapl) TAPL_EVENTS_TAG else LAUNCHER_EVENTS_TAG, "$sequence / $event")
        sRecordedEvents.add("$sequence / $event")
        sEventConsumer?.let {
            if (reportToTapl) {
                it.accept(sequence, event)
            }
        }
    }

    @JvmStatic
    fun recordEvent(sequence: String, event: String) {
        recordEventSlow(sequence, event, true)
    }

    @JvmStatic
    fun recordEvent(sequence: String, message: String, parameter: Any?) {
        recordEventSlow(sequence, "$message: $parameter", true)
    }

    @JvmStatic
    fun recordKeyEvent(sequence: String, message: String, event: KeyEvent) {
        val reportToTapl = event.action != KeyEvent.ACTION_DOWN
        recordEventSlow(sequence, "$message: $event", reportToTapl)
        registerEventNotFromTest(event)
    }

    @JvmStatic
    fun recordMotionEvent(sequence: String, message: String, event: MotionEvent) {
        val action = event.action
        if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_HOVER_MOVE) {
            recordEventSlow(sequence, "$message: $event", false)
            if (action != MotionEvent.ACTION_CANCEL) {
                registerEventNotFromTest(event)
            }
        }
    }

    private fun registerEventNotFromTest(event: InputEvent) {
        if (sEnableRegisterEventNotFromTest && !sHadEventsNotFromTest && event.deviceId != -1) {
            sHadEventsNotFromTest = true
            Log.d("TestProtocol", "First event not from test: $event")
        }
    }

    @JvmStatic
    fun setEnableRegisterEventNotFromTest(enable: Boolean) {
        sEnableRegisterEventNotFromTest = enable
    }

    @JvmStatic
    fun setEventConsumer(consumer: BiConsumer<String, String>?) {
        sEventConsumer = consumer
    }

    @VisibleForTesting
    @JvmStatic
    fun getRecordedEvents(): List<String> = sRecordedEvents.toList()

    @VisibleForTesting
    @JvmStatic
    fun clearEvents() {
        sRecordedEvents.clear()
        sHadEventsNotFromTest = false
    }
}
