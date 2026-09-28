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

package com.sandboxr.launcher.celllayout

import android.graphics.Canvas

/**
 * A delegated drawing hook for drawing under or over items in CellLayout.
 */
abstract class DelegatedCellDrawing {
    @JvmField var mDelegateCellX: Int = 0
    @JvmField var mDelegateCellY: Int = 0

    /** Draw under CellLayout child items */
    abstract fun drawUnderItem(canvas: Canvas)

    /** Draw over CellLayout child items */
    abstract fun drawOverItem(canvas: Canvas)
}
