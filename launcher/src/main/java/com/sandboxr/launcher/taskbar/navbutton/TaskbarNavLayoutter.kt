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

package com.sandboxr.launcher.taskbar.navbutton

import android.content.res.Resources
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import com.sandboxr.launcher.R
import com.sandboxr.launcher.taskbar.TaskbarActivityContext

/**
 * Layoutter for nav buttons on large screen devices.
 */
class TaskbarNavLayoutter(
    val resources: Resources,
    val navButtonContainer: LinearLayout,
    val endContextualContainer: ViewGroup,
    val startContextualContainer: ViewGroup,
    val imeSwitcher: ImageView?,
    val a11yButton: ImageView?,
    val moreOptionsButton: ImageView?,
    val space: Space?,
    val backButton: ImageView?,
    val homeButton: ImageView?,
    val recentsButton: ImageView?,
) : NavButtonLayoutter {

    override fun layoutButtons(context: TaskbarActivityContext, isA11yButtonPersistent: Boolean) {
        layoutButtons(context, isA11yButtonPersistent, isA11yVisible = false, isMoreOptionsVisible = false)
    }

    override fun layoutButtons(
        context: TaskbarActivityContext,
        isA11yButtonPersistent: Boolean,
        isA11yVisible: Boolean,
        isMoreOptionsVisible: Boolean,
    ) {
        startContextualContainer.removeAllViews()
        endContextualContainer.removeAllViews()

        navButtonContainer.orientation = LinearLayout.HORIZONTAL
        navButtonContainer.gravity = Gravity.CENTER_VERTICAL or Gravity.END

        addThreeButtons()

        imeSwitcher?.let {
            if (it.parent == null) {
                endContextualContainer.addView(it)
            }
        }
        a11yButton?.let {
            if (it.parent == null) {
                endContextualContainer.addView(it)
            }
        }
    }

    override fun addThreeButtons() {
        navButtonContainer.removeAllViews()
        backButton?.let { if (it.parent == null) navButtonContainer.addView(it) }
        homeButton?.let { if (it.parent == null) navButtonContainer.addView(it) }
        recentsButton?.let { if (it.parent == null) navButtonContainer.addView(it) }
    }
}
