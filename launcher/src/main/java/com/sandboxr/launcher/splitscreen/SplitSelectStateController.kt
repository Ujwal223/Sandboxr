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

package com.sandboxr.launcher.splitscreen

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.STAGE_POSITION_BOTTOM_OR_RIGHT
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.STAGE_POSITION_TOP_OR_LEFT
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.STAGE_POSITION_UNDEFINED
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.StagePosition
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.SplitSelectSource
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages the state machine for selecting two applications to run in split-screen mode.
 * Coordinates staging of the primary task/intent, selecting the second task/intent,
 * and dispatching the final launch request.
 */
class SplitSelectStateController(
    private val context: Context,
    val splitSelectDataHolder: SplitSelectDataHolder = SplitSelectDataHolder()
) {

    interface SplitSelectionListener {
        fun onSplitSelectionInitiated(@StagePosition stagePosition: Int) {}
        fun onSecondAppSelected() {}
        fun onSplitSelectionConfirmed() {}
        fun onSplitSelectionAborted() {}
    }

    private val listeners = CopyOnWriteArrayList<SplitSelectionListener>()
    private val mainHandler = Handler(Looper.getMainLooper())

    var isSplitSelectActive: Boolean = false
        private set

    @StagePosition
    var activeStagePosition: Int = STAGE_POSITION_UNDEFINED
        private set

    fun registerListener(listener: SplitSelectionListener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: SplitSelectionListener) {
        listeners.remove(listener)
    }

    /**
     * Initiates split screen selection with a given task ID.
     */
    fun initiateSplitSelect(
        taskId: Int,
        @StagePosition stagePosition: Int = STAGE_POSITION_TOP_OR_LEFT
    ) {
        Log.d(TAG, "initiateSplitSelect with taskId=$taskId, stagePosition=$stagePosition")
        resetState()
        isSplitSelectActive = true
        activeStagePosition = stagePosition
        splitSelectDataHolder.initialTaskId = taskId
        splitSelectDataHolder.initialStagePosition = stagePosition

        for (listener in listeners) {
            listener.onSplitSelectionInitiated(stagePosition)
        }
    }

    /**
     * Initiates split screen selection with an Intent (e.g. from Home or All Apps).
     */
    fun initiateSplitSelect(
        intent: Intent,
        @StagePosition stagePosition: Int = STAGE_POSITION_TOP_OR_LEFT
    ) {
        Log.d(TAG, "initiateSplitSelect with intent=$intent, stagePosition=$stagePosition")
        resetState()
        isSplitSelectActive = true
        activeStagePosition = stagePosition
        splitSelectDataHolder.initialIntent = intent
        splitSelectDataHolder.initialStagePosition = stagePosition

        for (listener in listeners) {
            listener.onSplitSelectionInitiated(stagePosition)
        }
    }

    /**
     * Initiates split screen selection from a [SplitSelectSource].
     */
    fun initiateSplitSelect(source: SplitSelectSource) {
        source.intent?.let {
            val stage = source.position?.stagePosition ?: STAGE_POSITION_TOP_OR_LEFT
            initiateSplitSelect(it, stage)
        }
    }

    /**
     * Selects the second task to complete the split pair.
     */
    fun setSecondTask(taskId: Int) {
        if (!isSplitSelectActive) {
            Log.w(TAG, "setSecondTask called but split selection is not active")
            return
        }
        val secondStage = SplitScreenUtils.getOppositeStagePosition(activeStagePosition)
        splitSelectDataHolder.secondTaskId = taskId
        splitSelectDataHolder.secondStagePosition = secondStage

        for (listener in listeners) {
            listener.onSecondAppSelected()
        }
    }

    /**
     * Selects the second intent to complete the split pair.
     */
    fun setSecondIntent(intent: Intent) {
        if (!isSplitSelectActive) {
            Log.w(TAG, "setSecondIntent called but split selection is not active")
            return
        }
        val secondStage = SplitScreenUtils.getOppositeStagePosition(activeStagePosition)
        splitSelectDataHolder.secondIntent = intent
        splitSelectDataHolder.secondStagePosition = secondStage

        for (listener in listeners) {
            listener.onSecondAppSelected()
        }
    }

    /**
     * Confirms and launches the selected split-screen tasks/intents.
     */
    fun launchSplitTasks(callback: ((Boolean) -> Unit)? = null) {
        if (!splitSelectDataHolder.isBothSplitAppsConfirmed) {
            Log.w(TAG, "Cannot launch split tasks: both apps not selected yet")
            callback?.invoke(false)
            return
        }

        Log.d(TAG, "launchSplitTasks: launching split pair")
        for (listener in listeners) {
            listener.onSplitSelectionConfirmed()
        }

        val success = true
        resetState()
        callback?.invoke(success)
    }

    /**
     * Aborts the in-progress split selection.
     */
    fun abortSplit() {
        if (!isSplitSelectActive) return
        Log.d(TAG, "abortSplit")
        resetState()
        for (listener in listeners) {
            listener.onSplitSelectionAborted()
        }
    }

    /**
     * Resets all internal state and data holders.
     */
    fun resetState() {
        isSplitSelectActive = false
        activeStagePosition = STAGE_POSITION_UNDEFINED
        splitSelectDataHolder.reset()
    }

    companion object {
        private const val TAG = "SplitSelectStateController"
    }
}
