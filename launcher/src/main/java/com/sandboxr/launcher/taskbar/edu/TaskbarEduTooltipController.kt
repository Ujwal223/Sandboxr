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

package com.sandboxr.launcher.taskbar.edu

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Educational onboarding tooltip displayed above Taskbar elements.
 */
class TaskbarEduTooltip(context: Context) : FrameLayout(context) {
    val textView = TextView(context).apply {
        setTextColor(Color.WHITE)
        setBackgroundColor(0xCC000000.toInt())
        setPadding(32, 16, 32, 16)
        gravity = Gravity.CENTER
    }

    init {
        addView(textView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        visibility = View.GONE
    }

    fun show(text: String) {
        textView.text = text
        visibility = View.VISIBLE
        alpha = 0f
        animate().alpha(1f).setDuration(200).start()
    }

    fun hide() {
        animate().alpha(0f).setDuration(150).withEndAction {
            visibility = View.GONE
        }.start()
    }
}

/**
 * Controller managing educational tooltips for Taskbar gestures and features.
 */
class TaskbarEduTooltipController(
    val context: Context,
) {
    private var activeTooltip: TaskbarEduTooltip? = null

    fun showEduTooltip(anchor: View, text: String): TaskbarEduTooltip {
        val tooltip = TaskbarEduTooltip(context)
        activeTooltip = tooltip
        tooltip.show(text)
        return tooltip
    }

    fun hideEduTooltip() {
        activeTooltip?.hide()
        activeTooltip = null
    }
}
