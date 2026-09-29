/*
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

package com.sandboxr.launcher.appprediction

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.UserHandle
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.util.ComponentKey
import com.sandboxr.launcher.util.Executors
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages contextual application usage prediction, recency scoring, launch frequency tracking,
 * and prediction event broadcast to All Apps prediction row and Hybrid Hotseat.
 */
class AppPredictionManager private constructor(private val context: Context) {

    fun interface PredictionListener {
        fun onPredictionsUpdated(predictions: List<WorkspaceItemInfo>)
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val listeners = CopyOnWriteArrayList<PredictionListener>()
    private var cachedPredictions: List<WorkspaceItemInfo> = emptyList()

    fun addListener(listener: PredictionListener) {
        listeners.add(listener)
        if (cachedPredictions.isNotEmpty()) {
            listener.onPredictionsUpdated(cachedPredictions)
        }
    }

    fun removeListener(listener: PredictionListener) {
        listeners.remove(listener)
    }

    /**
     * Clears all recorded app launch statistics and resets cached predictions.
     */
    fun clearStats() {
        prefs.edit().clear().apply()
        cachedPredictions = emptyList()
    }

    /**
     * Records a user launch event for [itemInfo] to update frequency and recency heuristics.
     */
    fun logAppLaunch(itemInfo: ItemInfo) {
        val cn = itemInfo.targetComponent ?: return
        logAppLaunch(cn, itemInfo.user)
    }

    fun logAppLaunch(component: ComponentName, user: UserHandle) {
        val key = "${component.flattenToString()}#${user.hashCode()}"
        val countKey = "count_$key"
        val timeKey = "time_$key"

        val count = prefs.getInt(countKey, 0) + 1
        val now = System.currentTimeMillis()

        prefs.edit()
            .putInt(countKey, count)
            .putLong(timeKey, now)
            .apply()
    }

    /**
     * Synchronously computes predicted apps based on historical launch counts and recency decay.
     */
    fun computePredictions(allApps: Array<AppInfo>, maxCount: Int = 10): List<WorkspaceItemInfo> {
        val scored = ArrayList<Pair<AppInfo, Double>>()
        val now = System.currentTimeMillis()

        for (app in allApps) {
            val cn = app.componentName ?: continue
            val key = "${cn.flattenToString()}#${app.user.hashCode()}"
            val count = prefs.getInt("count_$key", 0)
            val lastLaunch = prefs.getLong("time_$key", 0L)

            // Recency decay: halflife of 48 hours
            val hoursSinceLaunch = if (lastLaunch > 0) (now - lastLaunch) / (1000.0 * 3600.0) else 1000.0
            val recencyMultiplier = 1.0 / (1.0 + (hoursSinceLaunch / 48.0))

            val score = (count * 10.0) + (recencyMultiplier * 50.0)
            scored.add(Pair(app, score))
        }

        // Sort descending by score
        scored.sortByDescending { it.second }

        val resultList = ArrayList<WorkspaceItemInfo>()
        val limit = minOf(scored.size, maxCount)
        for (i in 0 until limit) {
            val app = scored[i].first
            val wItem = app.makeWorkspaceItem(context)
            resultList.add(wItem)
        }
        return resultList
    }

    /**
     * Generates predicted apps based on historical launch counts and recency decay.
     * If no launch history exists yet, falls back to default common apps in the drawer.
     */
    fun updatePredictions(allApps: Collection<AppInfo>, maxCount: Int = 10) {
        updatePredictions(allApps.toTypedArray(), maxCount)
    }

    fun updatePredictions(allApps: Array<AppInfo>, maxCount: Int = 10) {
        Executors.MODEL_EXECUTOR.execute {
            val resultList = computePredictions(allApps, maxCount)
            cachedPredictions = resultList
            Executors.MAIN_EXECUTOR.execute {
                for (listener in listeners) {
                    listener.onPredictionsUpdated(resultList)
                }
            }
        }
    }

    fun getCachedPredictions(): List<WorkspaceItemInfo> = cachedPredictions

    companion object {
        private const val PREFS_NAME = "sandboxr_app_predictions"

        @Volatile
        private var instance: AppPredictionManager? = null

        @JvmStatic
        fun get(context: Context): AppPredictionManager {
            return instance ?: synchronized(this) {
                instance ?: AppPredictionManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
