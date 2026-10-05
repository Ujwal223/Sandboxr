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

package com.sandboxr.launcher.taskbar.bubbles.flyout

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Flyout view displaying new message alerts above the bubble bar.
 */
class BubbleBarFlyoutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val textView = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 14f
        gravity = Gravity.CENTER_VERTICAL
        setPadding(24, 16, 24, 16)
    }

    init {
        addView(textView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        visibility = GONE
    }

    fun showMessage(message: BubbleBarFlyoutMessage) {
        textView.text = message.message
        visibility = VISIBLE
        alpha = 0f
        animate().alpha(1f).setDuration(200).start()
    }

    fun hide() {
        animate().alpha(0f).setDuration(150).withEndAction {
            visibility = GONE
        }.start()
    }
}
