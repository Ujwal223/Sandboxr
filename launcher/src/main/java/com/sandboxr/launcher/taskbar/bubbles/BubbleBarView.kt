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

package com.sandboxr.launcher.taskbar.bubbles

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * Visual container hosting active chat bubbles in the Taskbar.
 */
class BubbleBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val bubblesContainer = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private val bubbleItems = mutableListOf<BubbleBarItem>()
    var isExpanded: Boolean = false
        private set

    var selectedBubbleKey: String? = null
        private set

    init {
        addView(bubblesContainer, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
    }

    fun addBubble(item: BubbleBarItem) {
        bubbleItems.add(item)
        val lp = LinearLayout.LayoutParams(96, 96).apply {
            setMargins(8, 0, 8, 0)
        }
        item.view.setOnClickListener {
            selectBubble(item.key)
        }
        bubblesContainer.addView(item.view, lp)
        visibility = View.VISIBLE
    }

    fun removeBubble(key: String) {
        val item = bubbleItems.find { it.key == key } ?: return
        bubbleItems.remove(item)
        bubblesContainer.removeView(item.view)
        if (bubbleItems.isEmpty()) {
            visibility = View.GONE
        }
    }

    fun selectBubble(key: String) {
        selectedBubbleKey = key
        isExpanded = true
    }

    fun collapse() {
        isExpanded = false
    }

    fun getBubbles(): List<BubbleBarItem> = bubbleItems.toList()

    fun getBubbleCount(): Int = bubbleItems.size
}
