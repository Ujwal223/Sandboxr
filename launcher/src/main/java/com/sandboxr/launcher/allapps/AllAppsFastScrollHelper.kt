/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.view.HapticFeedbackConstants
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

/**
 * Coordinates fast scrolling and smooth section snapping in the All Apps recycler view.
 */
class AllAppsFastScrollHelper(private val recyclerView: AllAppsRecyclerView) {

    private var targetFastScrollPosition: Int = NO_POSITION

    fun smoothScrollToSection(info: AlphabeticalAppsList.FastScrollSectionInfo) {
        if (targetFastScrollPosition == info.position) {
            return
        }
        targetFastScrollPosition = info.position
        val layoutManager = recyclerView.layoutManager ?: return
        val scroller = object : LinearSmoothScroller(recyclerView.context) {
            init {
                targetPosition = info.position
            }

            override fun getVerticalSnapPreference(): Int {
                recyclerView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                return SNAP_TO_START
            }
        }
        layoutManager.startSmoothScroll(scroller)
    }

    fun onFastScrollCompleted() {
        targetFastScrollPosition = NO_POSITION
    }

    companion object {
        private const val NO_POSITION = -1
    }
}
