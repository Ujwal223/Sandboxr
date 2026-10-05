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

package com.sandboxr.launcher.taskbar.customization

import android.content.Context
import com.sandboxr.launcher.DeviceProfile

/**
 * Evaluates features available to the Taskbar given the current device and profile.
 */
class TaskbarFeatureEvaluator(
    val context: Context,
    val deviceProfile: DeviceProfile,
    var isPinned: Boolean = false,
    var isThreeButtonNav: Boolean = false,
) {
    val isPrimaryDisplay: Boolean = true
    val hasBubbles: Boolean = false
    val hasNavButtons: Boolean get() = isThreeButtonNav

    val isTransient: Boolean
        get() = !isPinned && !isThreeButtonNav && deviceProfile.isTablet

    val supportsTransitionToTransientTaskbar: Boolean
        get() = deviceProfile.isTablet || deviceProfile.isTwoPanels
}
