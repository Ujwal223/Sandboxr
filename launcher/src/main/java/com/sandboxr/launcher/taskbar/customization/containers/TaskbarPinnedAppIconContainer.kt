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

package com.sandboxr.launcher.taskbar.customization.containers

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.taskbar.customization.TaskbarContainer
import com.sandboxr.launcher.taskbar.customization.TaskbarIconSpecs

/**
 * Container holding pinned app icons inside the Taskbar.
 */
class TaskbarPinnedAppIconContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), TaskbarContainer {

    override var taskbarIconViewSize: Int = TaskbarIconSpecs.defaultPersistentIconSize.size
    override var taskbarIconViewPadding: Int = 6

    override val spaceNeeded: Int
        get() {
            val count = childCount
            return if (count == 0) 0 else count * (taskbarIconViewSize + taskbarIconViewPadding)
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun addIconView(view: View) {
        val lp = LayoutParams(taskbarIconViewSize, taskbarIconViewSize).apply {
            setMargins(taskbarIconViewPadding / 2, 0, taskbarIconViewPadding / 2, 0)
        }
        addView(view, lp)
    }

    fun removeIconView(view: View) {
        removeView(view)
    }

    fun getIconViews(): List<BubbleTextView> {
        val list = mutableListOf<BubbleTextView>()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is BubbleTextView) {
                list.add(child)
            }
        }
        return list
    }
}
