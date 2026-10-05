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

package com.sandboxr.launcher.task.apptimer

import android.content.Context
import java.time.Duration
import javax.inject.Inject

sealed class TaskAppTimerUiState {
    data object None : TaskAppTimerUiState()

    data class Timer(
        val taskPackageName: String,
        val taskDescription: String,
        val timeRemaining: Duration,
    ) : TaskAppTimerUiState()
}

class DurationFormatter @Inject constructor() {
    fun formatDuration(duration: Duration, context: Context): String {
        val totalMinutes = duration.toMinutes()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m left"
            hours > 0 -> "${hours}h left"
            minutes > 0 -> "${minutes}m left"
            else -> "< 1m left"
        }
    }
}

class TimerTextHelper @Inject constructor(
    private val durationFormatter: DurationFormatter,
) {
    fun getFormattedDuration(duration: Duration, context: Context): String {
        return durationFormatter.formatDuration(duration, context)
    }
}

class TaskAppTimerViewModel @Inject constructor(
    private val timerTextHelper: TimerTextHelper,
) {
    fun getFormattedDuration(duration: Duration, context: Context): String {
        return timerTextHelper.getFormattedDuration(duration, context)
    }
}
