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

package com.sandboxr.launcher.debug

import android.util.Log
import androidx.annotation.VisibleForTesting
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Emits and records test events for TAPL and UI Automator test synchronization.
 */
object TestEventEmitter {

    private const val TAG = "TestEventEmitter"

    enum class TestEvent {
        RESIZE_FRAME_SHOWING,
        FIXED_LANDSCAPE,
        LAUNCHER_STATE_COMPLETED,
        GESTURE_TUTORIAL_STEP_COMPLETED,
        POPUP_OPENED,
        POPUP_CLOSED,
        DRAG_STARTED,
        DRAG_ENDED,
        ALL_APPS_SHOWN,
        ALL_APPS_DISMISSED,
        SECONDARY_DISPLAY_ATTACHED
    }

    fun interface EventListener {
        fun onEvent(event: TestEvent)
    }

    private val listeners = CopyOnWriteArrayList<EventListener>()
    private val emittedEvents = CopyOnWriteArrayList<TestEvent>()

    @JvmStatic
    fun sendEvent(event: TestEvent) {
        Log.d(TAG, "TestEvent emitted: $event")
        emittedEvents.add(event)
        listeners.forEach { it.onEvent(event) }
    }

    @JvmStatic
    fun registerListener(listener: EventListener) {
        listeners.add(listener)
    }

    @JvmStatic
    fun unregisterListener(listener: EventListener) {
        listeners.remove(listener)
    }

    @VisibleForTesting
    @JvmStatic
    fun getEmittedEvents(): List<TestEvent> = emittedEvents.toList()

    @VisibleForTesting
    @JvmStatic
    fun hasEmitted(event: TestEvent): Boolean = emittedEvents.contains(event)

    @VisibleForTesting
    @JvmStatic
    fun clearEvents() {
        emittedEvents.clear()
    }
}
