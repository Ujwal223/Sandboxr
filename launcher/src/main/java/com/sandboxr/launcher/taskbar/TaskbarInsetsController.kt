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

import android.graphics.Rect
import android.graphics.Region

/**
 * Controller computing window insets, navigation bar frame heights, and touchable regions.
 */
class TaskbarInsetsController(
    val activity: TaskbarActivityContext,
) {
    val touchableRegion = Region()
    val contentInsets = Rect()

    fun updateInsets() {
        val height = activity.resources.getDimensionPixelSize(com.sandboxr.launcher.R.dimen.taskbar_size)
        contentInsets.set(0, 0, 0, height)
    }

    fun onTaskbarWindowBoundsChanged(bounds: Rect) {
        touchableRegion.set(bounds)
    }
}
