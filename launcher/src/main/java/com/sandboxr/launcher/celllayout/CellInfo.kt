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

import android.view.View
import com.sandboxr.launcher.celllayout.CellPosMapper.CellPos
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.util.CellAndSpan

/**
 * Stores cell and span information for dragged views and long-clicked cell locations.
 */
class CellInfo(
    @JvmField val cell: View?,
    @JvmField val screenId: Int,
    @JvmField val container: Int,
    cellX: Int,
    cellY: Int,
    spanX: Int,
    spanY: Int,
) : CellAndSpan(cellX, cellY, spanX, spanY) {

    constructor(
        cell: View?,
        info: ItemInfo,
        cellPos: CellPos,
    ) : this(
        cell,
        cellPos.screenId,
        info.container,
        cellPos.cellX,
        cellPos.cellY,
        info.spanX,
        info.spanY,
    )

    fun isSameAs(item: ItemInfo): Boolean =
        item.container == container &&
            item.screenId == screenId &&
            item.cellX == cellX &&
            item.cellY == cellY &&
            item.spanX == spanX &&
            item.spanY == spanY

    override fun toString(): String {
        return "CellInfo(cell=$cell, screenId=$screenId, container=$container)"
    }
}
