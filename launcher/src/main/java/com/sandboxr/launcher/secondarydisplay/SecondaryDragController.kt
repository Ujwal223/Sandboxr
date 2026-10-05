/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.sandboxr.launcher.secondarydisplay

import android.graphics.Rect
import android.view.HapticFeedbackConstants
import com.sandboxr.launcher.AbstractFloatingView
import com.sandboxr.launcher.DropTarget
import com.sandboxr.launcher.R
import com.sandboxr.launcher.dragndrop.DragController
import com.sandboxr.launcher.dragndrop.DragOptions
import com.sandboxr.launcher.util.Executors.MAIN_EXECUTOR

/**
 * Drag controller for Secondary Launcher activity.
 */
class SecondaryDragController(
    private val mActivity: SecondaryDisplayLauncher
) : DragController(mActivity) {

    override fun onDragViewInitialized() {
        mActivity.getDragLayer()?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        if (!isItemPinnable) {
            MAIN_EXECUTOR.post { cancelDrag() }
        }
    }

    public override fun getDefaultDropTarget(dropCoordinates: IntArray?): DropTarget {
        return object : DropTarget {
            override fun isDropEnabled(): Boolean = true

            override fun onDrop(dragObject: DropTarget.DragObject, options: Any?) {
                val item = dragObject.dragInfo ?: return
                (mActivity.getDragLayer() as? SecondaryDragLayer)?.pinnedAppsAdapter?.addPinnedApp(item)
                val dv = dragObject.dragView
                if (dv is com.sandboxr.launcher.dragndrop.DragView) {
                    dv.remove()
                } else {
                    (dv?.parent as? android.view.ViewGroup)?.removeView(dv)
                }
            }

            override fun onDragEnter(dragObject: DropTarget.DragObject) {
                val threshold = try {
                    mActivity.resources.getDimensionPixelSize(R.dimen.drag_distanceThreshold)
                } catch (e: Exception) {
                    20
                }
                if (distanceDragged > threshold) {
                    mActivity.showAppDrawer(false)
                    AbstractFloatingView.closeAllOpenViews(mActivity)
                }
            }

            override fun onDragOver(dragObject: DropTarget.DragObject) {}

            override fun onDragExit(dragObject: DropTarget.DragObject) {}

            override fun acceptDrop(dragObject: DropTarget.DragObject): Boolean = true

            override fun prepareAccessibilityDrop() {}

            override fun getHitRectRelativeToDragLayer(outRect: Rect) {}
        }
    }
}
