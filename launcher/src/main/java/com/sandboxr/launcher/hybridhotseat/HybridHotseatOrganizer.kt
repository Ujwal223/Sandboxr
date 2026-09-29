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

package com.sandboxr.launcher.hybridhotseat

import android.content.ComponentName
import android.os.UserHandle
import android.view.View
import android.view.ViewGroup
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.Hotseat
import com.sandboxr.launcher.celllayout.CellLayoutLayoutParams
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.util.Executors
import com.sandboxr.launcher.views.ActivityContext
import com.sandboxr.launcher.views.PredictedAppIcon
import java.util.ArrayList

/**
 * Organizes and lays out the Hybrid Hotseat dock, blending user-pinned apps with AI-predicted
 * applications to fill vacant slots without displacing pinned shortcuts.
 */
class HybridHotseatOrganizer(
    private val activityContext: ActivityContext,
    private val hotseat: Hotseat,
    private val onItemLongClickListener: View.OnLongClickListener? = null
) : ViewGroup.OnHierarchyChangeListener {

    private var isUpdating = false

    var predictedItems: List<ItemInfo> = emptyList()
        set(value) {
            field = value
            fillGapsWithPrediction(false)
        }

    init {
        hotseat.shortcutsAndWidgets.setOnHierarchyChangeListener(this)
    }

    override fun onChildViewAdded(parent: View?, child: View?) {
        onHotseatHierarchyChanged()
    }

    override fun onChildViewRemoved(parent: View?, child: View?) {
        onHotseatHierarchyChanged()
    }

    private fun onHotseatHierarchyChanged() {
        if (!isUpdating) {
            Executors.MAIN_EXECUTOR.handler.post {
                fillGapsWithPrediction(true)
            }
        }
    }

    /**
     * Inspects all hotseat cell slots and fills empty locations with top predicted apps.
     */
    @JvmOverloads
    fun fillGapsWithPrediction(animate: Boolean = false) {
        if (isUpdating) return
        isUpdating = true

        try {
            val totalSlots = (hotseat.countX * hotseat.countY).coerceAtLeast(1)

            // 1. Gather all components currently pinned in hotseat to avoid duplicating them
            val pinnedComponents = HashSet<ComponentName>()
            for (rank in 0 until totalSlots) {
                val cx = hotseat.getCellXFromOrder(rank)
                val cy = hotseat.getCellYFromOrder(rank)
                val child = hotseat.getChildAt(cx, cy)
                if (child != null && !isPredictedIcon(child)) {
                    val tag = child.tag as? ItemInfo
                    tag?.targetComponent?.let { pinnedComponents.add(it) }
                }
            }

            // 2. Filter predictions to only those not already pinned
            val availablePredictions = ArrayList<WorkspaceItemInfo>()
            for (item in predictedItems) {
                val wItem = when (item) {
                    is WorkspaceItemInfo -> item
                    is AppInfo -> item.makeWorkspaceItem(hotseat.context)
                    else -> null
                } ?: continue

                val comp = wItem.targetComponent
                if (comp != null && !pinnedComponents.contains(comp)) {
                    availablePredictions.add(wItem)
                }
            }

            var predictionIndex = 0

            // 3. Populate empty slots or update existing predicted icons
            for (rank in 0 until totalSlots) {
                val cx = hotseat.getCellXFromOrder(rank)
                val cy = hotseat.getCellYFromOrder(rank)
                val child = hotseat.getChildAt(cx, cy)

                if (child != null && !isPredictedIcon(child)) {
                    // Pinned by user, do not touch
                    continue
                }

                if (predictionIndex < availablePredictions.size) {
                    val predItem = availablePredictions[predictionIndex++]
                    predItem.rank = rank
                    predItem.cellX = cx
                    predItem.cellY = cy

                    if (isPredictedIcon(child)) {
                        val predIcon = child as PredictedAppIcon
                        predIcon.applyFromWorkspaceItemWithAnimation(predItem, rank)
                    } else {
                        // Inflate and insert new PredictedAppIcon
                        val predIcon = PredictedAppIcon(hotseat.context).apply {
                            applyFromWorkspaceItemWithAnimation(predItem, rank)
                            setOnClickListener(activityContext.getItemOnClickListener())
                            setOnLongClickListener(onItemLongClickListener)
                        }
                        val lp = CellLayoutLayoutParams(cx, cy, 1, 1).apply {
                            canReorder = false
                        }
                        hotseat.shortcutsAndWidgets.addView(predIcon, lp)
                    }
                } else {
                    // No more predictions, clean up stale prediction icon if present
                    if (isPredictedIcon(child)) {
                        hotseat.shortcutsAndWidgets.removeView(child)
                    }
                }
            }
        } finally {
            isUpdating = false
        }
    }

    private fun isPredictedIcon(view: View?): Boolean {
        return view is PredictedAppIcon && view.isPredicted
    }

    fun onModelItemsRemoved(matcher: (ItemInfo) -> Boolean) {
        val updated = predictedItems.filterNot(matcher)
        if (updated.size != predictedItems.size) {
            predictedItems = updated
        }
    }

    fun destroy() {
        hotseat.shortcutsAndWidgets.setOnHierarchyChangeListener(null)
    }

    companion object {
        private const val TAG = "HybridHotseatOrganizer"
    }
}
