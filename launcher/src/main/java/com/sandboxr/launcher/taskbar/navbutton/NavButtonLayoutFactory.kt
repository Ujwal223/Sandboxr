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
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import com.sandboxr.launcher.DeviceProfile
import com.sandboxr.launcher.R

/**
 * Factory for creating the appropriate [NavButtonLayoutter] based on device configuration.
 */
class NavButtonLayoutFactory {
    companion object {
        fun getUiLayoutter(
            deviceProfile: DeviceProfile,
            navButtonsView: NearestTouchFrame,
            imeSwitcher: ImageView?,
            a11yButton: ImageView?,
            moreOptionsButton: ImageView?,
            space: Space?,
            resources: Resources,
            isKidsMode: Boolean = false,
            isInSetup: Boolean = false,
            isThreeButtonNav: Boolean = true,
            phoneMode: Boolean = false,
        ): NavButtonLayoutter {
            val navButtonContainer = navButtonsView.findViewById<LinearLayout>(R.id.end_nav_buttons)
                ?: LinearLayout(navButtonsView.context).also { navButtonsView.addView(it) }
            val endContextualContainer = navButtonsView.findViewById<ViewGroup>(R.id.end_contextual_buttons)
                ?: navButtonContainer
            val startContextualContainer = navButtonsView.findViewById<ViewGroup>(R.id.start_contextual_buttons)
                ?: navButtonContainer

            val backButton: ImageView? = navButtonContainer.findViewById(R.id.back)
            val homeButton: ImageView? = navButtonContainer.findViewById(R.id.home)
            val recentsButton: ImageView? = navButtonContainer.findViewById(R.id.recent_apps)

            return TaskbarNavLayoutter(
                resources = resources,
                navButtonContainer = navButtonContainer,
                endContextualContainer = endContextualContainer,
                startContextualContainer = startContextualContainer,
                imeSwitcher = imeSwitcher,
                a11yButton = a11yButton,
                moreOptionsButton = moreOptionsButton,
                space = space,
                backButton = backButton,
                homeButton = homeButton,
                recentsButton = recentsButton,
            )
        }
    }
}
