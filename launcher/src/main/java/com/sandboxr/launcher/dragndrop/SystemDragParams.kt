/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.sandboxr.launcher.dragndrop

import android.content.ClipData
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.sandboxr.launcher.DragSource
import com.sandboxr.launcher.model.data.ItemInfo

/**
 * Parameters for a system-level drag-and-drop operation sequence.
 */
data class SystemDragParams(
    val clipData: ClipData?,
    val extraDragFlags: Int = 0,
    val closeAllOpenViews: Boolean = false,
    val dragImage: Drawable,
    val dragInfo: ItemInfo,
    val dragLayerX: Int,
    val dragLayerY: Int,
    val dragOptions: DragOptions,
    val dragRegion: Rect,
    val dragSource: DragSource,
    val dragViewScaleOnDrop: Float = 1.0f,
    val draggableView: DraggableView,
    val initialDragViewScale: Float = 1.0f,
)
