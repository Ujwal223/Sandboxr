/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import com.sandboxr.launcher.Hotseat
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.appprediction.AppPredictionManager
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.views.ActivityContext
import com.sandboxr.launcher.views.PredictedAppIcon

/**
 * Controller managing Hybrid Hotseat predictions, coordinating predictions delivery from
 * [AppPredictionManager], gap filling via [HybridHotseatOrganizer], and predicted icon pinning.
 */
class HotseatPredictionController(
    private val launcher: Launcher,
    private val hotseat: Hotseat
) {

    private val organizer: HybridHotseatOrganizer
    private val predictionManager = AppPredictionManager.get(launcher)

    private val predictionListener = AppPredictionManager.PredictionListener { predictions ->
        organizer.predictedItems = predictions
        hotseat.onPredictionsUpdated(predictions)
    }

    init {
        organizer = HybridHotseatOrganizer(
            activityContext = launcher,
            hotseat = hotseat,
            onItemLongClickListener = { v -> onPredictedItemLongClicked(v) }
        )

        hotseat.setPredictionListener { items ->
            organizer.predictedItems = items
        }

        predictionManager.addListener(predictionListener)
    }

    /**
     * Handles long-press on a predicted app in the hotseat dock.
     * Pins the item permanently to the hotseat or initiates drag-and-drop.
     */
    fun onPredictedItemLongClicked(view: View): Boolean {
        if (view !is PredictedAppIcon) return false

        val itemInfo = view.tag as? WorkspaceItemInfo ?: return false
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        // Convert the predictive item into a permanent pinned item
        view.pin()
        itemInfo.container = com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_HOTSEAT
        return true
    }

    /**
     * Manually updates the predictions set in the hybrid hotseat dock.
     */
    fun setPredictedItems(items: List<ItemInfo>) {
        organizer.predictedItems = items
        hotseat.onPredictionsUpdated(items)
    }

    fun destroy() {
        predictionManager.removeListener(predictionListener)
        hotseat.setPredictionListener(null)
        organizer.destroy()
    }

    companion object {
        private const val TAG = "HotseatPredictionController"
    }
}
