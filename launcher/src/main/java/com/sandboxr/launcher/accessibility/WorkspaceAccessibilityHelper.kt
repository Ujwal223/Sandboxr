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

import android.content.Context
import android.text.TextUtils
import android.view.View
import com.sandboxr.launcher.CellLayout
import com.sandboxr.launcher.R
import com.sandboxr.launcher.accessibility.BaseAccessibilityDelegate.DragType
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.FolderInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo

/**
 * Implementation of [DragAndDropAccessibilityDelegate] to support DnD on workspace.
 */
class WorkspaceAccessibilityHelper(layout: CellLayout) : DragAndDropAccessibilityDelegate(layout) {

    override fun intersectsValidDropTarget(id: Int): Int {
        val countX = mView.countX
        val countY = mView.countY
        val x = id % countX
        val y = id / countX
        val dragInfo = mDelegate?.getDragInfo() ?: return INVALID_POSITION

        if (dragInfo.dragType == DragType.WIDGET && !mView.acceptsWidget()) {
            return INVALID_POSITION
        }

        if (dragInfo.dragType == DragType.WIDGET) {
            val spanX = dragInfo.info?.spanX ?: 1
            val spanY = dragInfo.info?.spanY ?: 1

            for (m in 0 until spanX) {
                for (n in 0 until spanY) {
                    var fits = true
                    val x0 = x - m
                    val y0 = y - n
                    if (x0 < 0 || y0 < 0) continue

                    for (i in x0 until x0 + spanX) {
                        if (!fits) break
                        for (j in y0 until y0 + spanY) {
                            if (i >= countX || j >= countY || mView.isOccupied(i, j)) {
                                fits = false
                                break
                            }
                        }
                    }
                    if (fits) {
                        return x0 + countX * y0
                    }
                }
            }
            return INVALID_POSITION
        } else {
            val child = mView.getChildAt(x, y)
            if (child == null || child === dragInfo.item) {
                return id
            } else if (dragInfo.dragType != DragType.FOLDER && dragInfo.dragType != DragType.FILESYSTEM_ICON) {
                val info = child.tag as? ItemInfo
                if (info is AppInfo || info is FolderInfo || info is WorkspaceItemInfo) {
                    return id
                }
            }
            return INVALID_POSITION
        }
    }

    override fun getLocationDescriptionForIconDrop(id: Int): String {
        val x = id % mView.countX
        val y = id / mView.countX
        val dragInfo = mDelegate?.getDragInfo()

        val child = mView.getChildAt(x, y)
        return if (child == null || child === dragInfo?.item) {
            mView.getItemMoveDescription(x, y)
        } else {
            val pageDescription = mView.getContainerPageDescription()
            getDescriptionForDropOver(child, mContext, pageDescription)
        }
    }

    companion object {
        @JvmStatic
        fun getDescriptionForDropOver(overChild: View, context: Context, pageDescription: String?): String {
            val info = overChild.tag as? ItemInfo ?: return ""
            val row = info.cellY + 1
            val col = info.cellX + 1

            return when (info) {
                is WorkspaceItemInfo -> {
                    context.getString(
                        R.string.create_folder_with_position,
                        info.title ?: "",
                        row, col, pageDescription ?: ""
                    )
                }
                is FolderInfo -> {
                    if (TextUtils.isEmpty(info.title)) {
                        val firstItem = info.getContents().filterIsInstance<WorkspaceItemInfo>().minByOrNull { it.rank }
                        if (firstItem != null) {
                            context.getString(
                                R.string.add_to_folder_with_app_with_position,
                                firstItem.title ?: "",
                                row, col, pageDescription ?: ""
                            )
                        } else {
                            context.getString(
                                R.string.add_to_folder_with_position,
                                info.title ?: "",
                                row, col, pageDescription ?: ""
                            )
                        }
                    } else {
                        context.getString(
                            R.string.add_to_folder_with_position,
                            info.title ?: "",
                            row, col, pageDescription ?: ""
                        )
                    }
                }
                else -> ""
            }
        }
    }
}
