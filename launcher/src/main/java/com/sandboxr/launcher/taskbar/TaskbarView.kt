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
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.Insettable
import com.sandboxr.launcher.R
import com.sandboxr.launcher.model.data.ItemInfo

/**
 * Main container view displaying pinned icons, all apps button, and divider in the Taskbar.
 */
class TaskbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), Insettable {

    private val iconLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }

    var allAppsButton: View? = null
        private set

    var dividerView: View? = null
        private set

    private val iconViews = mutableListOf<View>()
    var itemClickListener: ((ItemInfo) -> Unit)? = null

    init {
        addView(iconLayout, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, Gravity.CENTER))
    }

    fun init(activity: TaskbarActivityContext) {
        val dp = activity.getDeviceProfile()
        iconLayout.removeAllViews()
        iconViews.clear()

        // All-apps button
        val allApps = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            contentDescription = "All Apps"
            setOnClickListener {
                activity.sharedState.isAllAppsVisible = true
            }
        }
        val iconSize = activity.resources.getDimensionPixelSize(R.dimen.taskbar_icon_size)
        val lp = LinearLayout.LayoutParams(iconSize, iconSize).apply {
            setMargins(8, 0, 8, 0)
        }
        iconLayout.addView(allApps, lp)
        allAppsButton = allApps

        // Divider
        val divider = ImageView(context).apply {
            setImageResource(R.drawable.taskbar_divider_button)
            contentDescription = "Divider"
        }
        iconLayout.addView(divider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT))
        dividerView = divider
    }

    fun updateIcons(items: List<ItemInfo>) {
        // Keep all-apps and divider, remove old app icons
        while (iconLayout.childCount > 2) {
            iconLayout.removeViewAt(2)
        }
        iconViews.clear()

        for (item in items) {
            val btv = BubbleTextView(context).apply {
                applyFromWorkspaceItem(item as? com.sandboxr.launcher.model.data.WorkspaceItemInfo ?: return@apply)
                setOnClickListener {
                    itemClickListener?.invoke(item)
                }
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(8, 0, 8, 0)
            }
            iconLayout.addView(btv, lp)
            iconViews.add(btv)
        }
    }

    fun getIconViews(): List<View> = iconViews.toList()

    override fun setInsets(insets: Rect) {
        // Draw behind insets
    }
}
