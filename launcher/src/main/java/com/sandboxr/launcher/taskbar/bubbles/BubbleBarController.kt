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

package com.sandboxr.launcher.taskbar.bubbles

import com.sandboxr.launcher.taskbar.bubbles.stashing.BubbleStashController

/**
 * Controller orchestrating bubble updates, user interactions, and lifecycle.
 */
class BubbleBarController(
    val bubbleBarView: BubbleBarView,
) {
    val bubbleStashController = BubbleStashController(bubbleBarView)

    fun onBubbleAdded(item: BubbleBarItem) {
        bubbleBarView.addBubble(item)
    }

    fun onBubbleRemoved(key: String) {
        bubbleBarView.removeBubble(key)
    }

    fun setStashed(stashed: Boolean, animate: Boolean = true) {
        if (stashed) {
            bubbleStashController.stash(animate)
        } else {
            bubbleStashController.unstash(animate)
        }
    }
}
