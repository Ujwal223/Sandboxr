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

package com.sandboxr.launcher

import android.view.View
import com.sandboxr.launcher.model.data.ItemInfo

/**
 * Interface defining an object that can receive a drag.
 */
interface DropTarget {

    open class DragObject {
        @JvmField var x: Int = -1
        @JvmField var y: Int = -1
        @JvmField var xOffset: Int = -1
        @JvmField var yOffset: Int = -1
        @JvmField var dragView: View? = null
        @JvmField var dragInfo: ItemInfo? = null
        @JvmField var dragSource: DragSource? = null
        @JvmField var postAnimationRunnable: Runnable? = null
        @JvmField var cancelled: Boolean = false
        @JvmField var dragComplete: Boolean = false
    }

    fun isDropEnabled(): Boolean = true

    fun onDrop(dragObject: DragObject, options: Any? = null) {}

    fun onDragEnter(dragObject: DragObject) {}

    fun onDragOver(dragObject: DragObject) {}

    fun onDragExit(dragObject: DragObject) {}

    fun acceptDrop(dragObject: DragObject): Boolean = true

    fun prepareAccessibilityDrop() {}

    fun getHitRectRelativeToDragLayer(outRect: android.graphics.Rect) {}
}
