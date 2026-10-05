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

package com.sandboxr.launcher.cuebar.logger

import android.util.Log

/**
 * Logger for ambient context engine (ACE) interaction events.
 */
class AmbientCueAceLogger {

    var lastEvent: Int? = null
        private set

    var eventCount: Int = 0
        private set

    fun reportCloseEvent() {
        reportInsightEvent(EVENT_USER_DISMISS)
    }

    fun reportInsightEvent(event: Int) {
        lastEvent = event
        eventCount++
        Log.d(TAG, "reportInsightEvent: event=$event, totalCount=$eventCount")
    }

    fun reset() {
        lastEvent = null
        eventCount = 0
    }

    companion object {
        private const val TAG = "AmbientCueAceLogger"

        const val EVENT_SHOW = 1
        const val EVENT_HIDE = 2
        const val EVENT_USER_TAP = 3
        const val EVENT_USER_LONG_PRESS = 4
        const val EVENT_USER_DISMISS = 5
    }
}
