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
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.sandboxr.launcher.BubbleTextView

/**
 * View presenting overflowed pinned apps when the taskbar icon limit is reached.
 */
class TaskbarOverflowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val overflowContainer = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    init {
        addView(overflowContainer, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    fun addOverflowApp(view: BubbleTextView) {
        overflowContainer.addView(view)
    }

    fun clearOverflowApps() {
        overflowContainer.removeAllViews()
    }

    fun getOverflowApps(): List<BubbleTextView> {
        val list = mutableListOf<BubbleTextView>()
        for (i in 0 until overflowContainer.childCount) {
            val child = overflowContainer.getChildAt(i)
            if (child is BubbleTextView) {
                list.add(child)
            }
        }
        return list
    }
}
