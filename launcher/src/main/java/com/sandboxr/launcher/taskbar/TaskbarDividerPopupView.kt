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

package com.sandboxr.launcher.taskbar

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Popup menu displayed on interacting with the Taskbar divider.
 */
class TaskbarDividerPopupView(context: Context) : FrameLayout(context) {
    val pinOption = TextView(context).apply {
        text = "Always show Taskbar"
        setTextColor(Color.WHITE)
        setPadding(32, 24, 32, 24)
        gravity = Gravity.CENTER_VERTICAL
    }

    init {
        addView(pinOption, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }
}
