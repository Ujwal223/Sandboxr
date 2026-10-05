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

package com.sandboxr.launcher.accessibility

import com.sandboxr.launcher.CellLayout
import com.sandboxr.launcher.R
import com.sandboxr.launcher.folder.FolderPagedView

/**
 * Implementation of [DragAndDropAccessibilityDelegate] to support DnD in a folder.
 */
class FolderAccessibilityHelper(layout: CellLayout) : DragAndDropAccessibilityDelegate(layout) {

    /**
     * 0-index position for the first cell in [mView] in [mParent].
     */
    private val mStartPosition: Int
    private val mParent: FolderPagedView? = layout.parent as? FolderPagedView

    init {
        val index = mParent?.indexOfChild(layout) ?: 0
        mStartPosition = index * layout.countX * layout.countY
    }

    override fun intersectsValidDropTarget(id: Int): Int {
        val allocated = mParent?.allocatedContentSize ?: (mView.countX * mView.countY)
        return Math.min(id, allocated - mStartPosition - 1)
    }

    override fun getLocationDescriptionForIconDrop(id: Int): String {
        return mContext.getString(R.string.move_to_position, (id + mStartPosition + 1).toString())
    }
}
