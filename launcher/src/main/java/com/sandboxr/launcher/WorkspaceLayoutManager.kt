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

import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.sandboxr.launcher.celllayout.CellLayoutLayoutParams
import com.sandboxr.launcher.celllayout.CellPosMapper
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.touch.ItemLongClickListener
import com.sandboxr.launcher.util.IntSet

/**
 * Interface and default methods for managing the placement, layout, and binding of items
 * (apps, widgets, folders) across workspace screens and the hotseat.
 */
interface WorkspaceLayoutManager {

    fun getCellPosMapper(): CellPosMapper

    fun getHotseat(): Hotseat?

    fun getScreenWithId(screenId: Int): CellLayout?

    fun onAddDropTarget(target: DropTarget) {}

    fun getWorkspaceChildOnLongClickListener(): View.OnLongClickListener {
        return ItemLongClickListener.INSTANCE_WORKSPACE
    }

    /**
     * At bind time, we use the rank (screenId) to compute x and y for hotseat items.
     */
    fun addInScreenFromBind(child: View, info: ItemInfo) {
        val presenterPos = getCellPosMapper().mapModelToPresenter(info)
        var x = presenterPos.cellX
        var y = presenterPos.cellY
        if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT ||
            info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION
        ) {
            val screenId = presenterPos.screenId
            // Default hotseat placement order
            x = screenId
            y = 0
        }

        if (child.tag == null) {
            child.tag = info
        }

        addInScreen(child, info.container, presenterPos.screenId, x, y, info.spanX, info.spanY)
    }

    /**
     * Adds the specified child in the specified screen based on the item info.
     */
    fun addInScreen(child: View, info: ItemInfo) {
        val presenterPos = getCellPosMapper().mapModelToPresenter(info)
        addInScreen(
            child,
            info.container,
            presenterPos.screenId,
            presenterPos.cellX,
            presenterPos.cellY,
            info.spanX,
            info.spanY
        )
    }

    /**
     * Adds the specified child in the specified screen.
     */
    fun addInScreen(
        child: View,
        container: Int,
        screenId: Int,
        x: Int,
        y: Int,
        spanX: Int,
        spanY: Int
    ) {
        if (container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            if (getScreenWithId(screenId) == null) {
                Log.e(TAG, "Skipping child, screenId $screenId not found")
                return
            }
        }
        if (EXTRA_EMPTY_SCREEN_IDS.contains(screenId)) {
            throw RuntimeException("Screen id should not be extra empty screen: $screenId")
        }

        val layout: CellLayout? = if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT ||
            container == LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION
        ) {
            // Hotseat child placement: hotseat may have internal cell layout
            null
        } else {
            getScreenWithId(screenId)
        }

        val genericLp = child.layoutParams
        val lp: CellLayoutLayoutParams = if (genericLp !is CellLayoutLayoutParams) {
            CellLayoutLayoutParams(x, y, spanX, spanY)
        } else {
            genericLp.cellX = x
            genericLp.cellY = y
            genericLp.cellHSpan = spanX
            genericLp.cellVSpan = spanY
            genericLp
        }

        if (spanX < 0 && spanY < 0) {
            lp.isLockedToGrid = false
        }

        val info = child.tag as? ItemInfo
        val childId = info?.viewId ?: View.NO_ID

        if (layout != null) {
            if (!layout.addViewToCellLayout(child, -1, childId, lp, true)) {
                Log.e(TAG, "Failed to add item at (${lp.cellX},${lp.cellY}) to CellLayout")
            }
        }

        child.isHapticFeedbackEnabled = false
        child.setOnLongClickListener(getWorkspaceChildOnLongClickListener())

        if (child is DropTarget) {
            onAddDropTarget(child)
        }
    }

    companion object {
        @JvmField val TAG: String = "Launcher.Workspace"
        @JvmField val EXTRA_EMPTY_SCREEN_ID: Int = -201
        @JvmField val EXTRA_EMPTY_SCREEN_SECOND_ID: Int = -200
        @JvmField val EXTRA_EMPTY_SCREEN_IDS: IntSet = IntSet.wrap(EXTRA_EMPTY_SCREEN_ID, EXTRA_EMPTY_SCREEN_SECOND_ID)
        @JvmField val FIRST_SCREEN_ID: Int = 0
    }
}
