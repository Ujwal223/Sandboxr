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

import com.sandboxr.launcher.taskbar.bubbles.flyout.BubbleBarFlyoutMessage
import com.sandboxr.launcher.taskbar.bubbles.model.BubbleIcon

/** An entity represented in the bubble bar. */
sealed class BubbleBarItem(open val key: String, open var view: BubbleView)

/** Contains state information about a specific bubble. */
data class BubbleBarBubble(
    val bubbleKey: String,
    override var view: BubbleView,
    var icon: BubbleIcon? = null,
    var appName: String = "",
    var dotColor: Int = 0,
    var flyoutMessage: BubbleBarFlyoutMessage? = null,
) : BubbleBarItem(bubbleKey, view)

/** Represents the overflow bubble in the bar. */
data class BubbleBarOverflow(
    override var view: BubbleView,
) : BubbleBarItem("Overflow", view)
