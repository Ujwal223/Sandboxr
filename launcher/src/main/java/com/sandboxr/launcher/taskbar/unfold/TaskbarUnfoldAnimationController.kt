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

package com.sandboxr.launcher.taskbar.unfold

import android.view.View

/**
 * Controller animating taskbar icons when unfolding a foldable device.
 */
class TaskbarUnfoldAnimationController {
    var isUnfolding: Boolean = false
        private set

    fun onTransitionProgress(progress: Float, iconViews: List<View>) {
        isUnfolding = progress < 1f && progress > 0f
        val center = iconViews.size / 2f
        iconViews.forEachIndexed { index, view ->
            val offsetFromCenter = index - center
            view.translationX = offsetFromCenter * (1f - progress) * 20f
        }
    }

    fun onTransitionFinished(iconViews: List<View>) {
        isUnfolding = false
        iconViews.forEach { it.translationX = 0f }
    }
}
