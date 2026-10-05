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

package com.sandboxr.launcher.input

import android.view.Display

/**
 * State object representing an active navigation gesture.
 */
class GestureState(
    val gestureId: Int = nextGestureId(),
    val displayId: Int = Display.DEFAULT_DISPLAY,
    var runningTaskId: Int = -1
) {

    enum class GestureEndTarget(val isLauncher: Boolean) {
        HOME(true),
        RECENTS(true),
        NEW_TASK(false),
        LAST_TASK(false),
        CANCEL(false)
    }

    var endTarget: GestureEndTarget? = null
    var isRecentsAnimationRunning: Boolean = false
    var isTrackpadGesture: Boolean = false

    fun isHandlingAtomicEvent(): Boolean = endTarget != null

    companion object {
        private var idCounter = 1
        private fun nextGestureId(): Int = synchronized(this) { idCounter++ }
    }
}
