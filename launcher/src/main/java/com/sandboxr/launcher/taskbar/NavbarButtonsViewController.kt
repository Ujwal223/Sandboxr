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

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.sandboxr.launcher.R
import com.sandboxr.launcher.taskbar.navbutton.NearestTouchFrame

/**
 * Controller managing navigation button views, icons, tinting, and click bindings.
 */
class NavbarButtonsViewController(
    val activity: TaskbarActivityContext,
    val navButtonsView: NearestTouchFrame,
) {
    private lateinit var controllers: TaskbarControllers

    var backButton: ImageView? = null
        private set
    var homeButton: ImageView? = null
        private set
    var recentsButton: ImageView? = null
        private set
    var imeSwitcherButton: ImageView? = null
        private set
    var a11yButton: ImageView? = null
        private set

    fun init(controllers: TaskbarControllers) {
        this.controllers = controllers

        val endNavContainer = navButtonsView.findViewById<LinearLayout>(R.id.end_nav_buttons)

        // Create buttons if not already in layout
        if (endNavContainer != null && endNavContainer.childCount == 0) {
            val back = ImageView(activity).apply {
                id = R.id.back
                setImageResource(R.drawable.ic_sysbar_back)
                contentDescription = "Back"
                setOnClickListener {
                    controllers.navButtonController.onButtonClick(TaskbarNavButtonController.BUTTON_BACK)
                }
                setOnLongClickListener {
                    controllers.navButtonController.onButtonLongClick(TaskbarNavButtonController.BUTTON_BACK)
                }
            }
            val home = ImageView(activity).apply {
                id = R.id.home
                setImageResource(R.drawable.ic_sysbar_home)
                contentDescription = "Home"
                setOnClickListener {
                    controllers.navButtonController.onButtonClick(TaskbarNavButtonController.BUTTON_HOME)
                }
                setOnLongClickListener {
                    controllers.navButtonController.onButtonLongClick(TaskbarNavButtonController.BUTTON_HOME)
                }
            }
            val recents = ImageView(activity).apply {
                id = R.id.recent_apps
                setImageResource(R.drawable.ic_sysbar_recent)
                contentDescription = "Overview"
                setOnClickListener {
                    controllers.navButtonController.onButtonClick(TaskbarNavButtonController.BUTTON_RECENTS)
                }
                setOnLongClickListener {
                    controllers.navButtonController.onButtonLongClick(TaskbarNavButtonController.BUTTON_RECENTS)
                }
            }

            val navSize = activity.resources.getDimensionPixelSize(R.dimen.taskbar_nav_buttons_size)
            val lp = LinearLayout.LayoutParams(navSize, navSize)
            endNavContainer.addView(back, lp)
            endNavContainer.addView(home, lp)
            endNavContainer.addView(recents, lp)

            backButton = back
            homeButton = home
            recentsButton = recents
        }
    }

    fun setNavButtonsVisible(visible: Boolean) {
        navButtonsView.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
