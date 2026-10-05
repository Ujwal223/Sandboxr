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

package com.sandboxr.launcher.taskbar.handoff

import android.content.Context

/** Suggestion item for cross-device or cross-profile task handoff. */
data class HandoffSuggestion(
    val id: String,
    val packageName: String,
    val title: String,
)

/** Controller managing task handoff suggestions shown in the taskbar. */
class TaskbarHandoffController(
    val context: Context,
) {
    private val suggestions = mutableListOf<HandoffSuggestion>()

    fun setSuggestions(newSuggestions: List<HandoffSuggestion>) {
        suggestions.clear()
        suggestions.addAll(newSuggestions)
    }

    fun getSuggestions(): List<HandoffSuggestion> = suggestions.toList()
}
