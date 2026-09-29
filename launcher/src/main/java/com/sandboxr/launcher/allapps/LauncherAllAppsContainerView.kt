/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.sandboxr.launcher.allapps

import android.content.Context
import android.util.AttributeSet
import android.view.WindowInsets
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherState

/**
 * AllAppsContainerView with Launcher-specific lifecycle callbacks,
 * state manager transitions, and nav bar insets.
 */
class LauncherAllAppsContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ActivityAllAppsContainerView<Launcher>(context, attrs, defStyleAttr) {

    override fun isInAllApps(): Boolean {
        return activityContext.getStateManager().isInStableState(LauncherState.ALL_APPS)
    }

    fun computeNavBarScrimHeight(insets: WindowInsets): Int {
        return insets.systemWindowInsetBottom
    }

    companion object {
        const val TAG = "LauncherAllAppsContainerView"
    }
}
