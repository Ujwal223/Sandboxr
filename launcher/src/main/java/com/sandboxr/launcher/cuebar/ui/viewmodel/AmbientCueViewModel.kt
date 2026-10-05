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

package com.sandboxr.launcher.cuebar.ui.viewmodel

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import com.sandboxr.launcher.cuebar.data.ActionModel
import com.sandboxr.launcher.cuebar.domain.interactor.AmbientCueInteractor
import com.sandboxr.launcher.cuebar.logger.AmbientCueAceLogger
import com.sandboxr.launcher.cuebar.logger.AmbientCueLogger
import com.sandboxr.launcher.util.Executors
import com.sandboxr.launcher.util.SafeCloseable
import java.io.PrintWriter
import java.util.concurrent.Executor
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Compose-driven ViewModel for CueBar ambient actions, states, and user interaction lifecycle.
 */
class AmbientCueViewModel(
    private val ambientCueInteractor: AmbientCueInteractor,
    private val ambientCueLogger: AmbientCueLogger? = null,
    private val ambientCueAceLogger: AmbientCueAceLogger? = null,
    private val isDesktopFormFactor: Boolean = false,
    private val scope: CoroutineScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Main),
    private val uiExecutor: Executor = Executors.MAIN_EXECUTOR,
) {
    var isVisible: Boolean by mutableStateOf(false)
        private set

    var isExpanded: Boolean by mutableStateOf(false)
        private set

    var showFirstTimeEducation: Boolean by mutableStateOf(false)
        private set

    var showLongPressEducation: Boolean by mutableStateOf(false)
        private set

    var pillStyle: PillStyleViewModel by mutableStateOf(PillStyleViewModel.Uninitialized)
        private set

    var actions: List<ActionViewModel> by mutableStateOf(emptyList())
        private set

    private var currentUnfilteredActions: List<ActionModel> = emptyList()

    var targetTaskId: Int by mutableIntStateOf(-1)
        private set

    var onVisibilityChanged: (Boolean) -> Unit = {}
    private var isSessionStarted = false

    private val listeners = mutableListOf<SafeCloseable>()
    private var deactivateCueBarJob: Job? = null
    private var actionUpdateJob: Job? = null

    init {
        registerListeners()
        recalculateStates()
    }

    private fun registerListeners() {
        listeners.add(ambientCueInteractor.isImeVisible.forEach(uiExecutor) { recalculateStates() })
        listeners.add(ambientCueInteractor.isOccludedBySystemUi.forEach(uiExecutor) { recalculateStates() })
        listeners.add(ambientCueInteractor.isDeactivated.forEach(uiExecutor) { recalculateStates() })
        listeners.add(ambientCueInteractor.isAmbientCueEnabled.forEach(uiExecutor) { recalculateStates() })
        listeners.add(ambientCueInteractor.isGestureNav.forEach(uiExecutor) { recalculateStates() })
        listeners.add(ambientCueInteractor.isTaskBarVisible.forEach(uiExecutor) { recalculateStates() })
        listeners.add(ambientCueInteractor.recentsButtonPosition.forEach(uiExecutor) { recalculateStates() })
        listeners.add(ambientCueInteractor.globallyFocusedTaskId.forEach(uiExecutor) { recalculateStates() })
        listeners.add(ambientCueInteractor.actions.forEach(uiExecutor, ::onUnfilteredActionsChange))
    }

    fun recalculateStates() {
        val oldVisibility = isVisible
        val globallyFocusedTaskId = ambientCueInteractor.globallyFocusedTaskId.value

        val isRootAttached =
            ambientCueInteractor.isTestMode.value ||
                (currentUnfilteredActions.isNotEmpty() &&
                    ambientCueInteractor.isAmbientCueEnabled.value &&
                    !ambientCueInteractor.isDeactivated.value &&
                    (targetTaskId == -1 || globallyFocusedTaskId == targetTaskId))

        if (isRootAttached && !isSessionStarted) {
            isSessionStarted = true
            var maCount = 0
            var mrCount = 0
            val packageName = ambientCueInteractor.frontTaskPackageName.value
            currentUnfilteredActions.forEach { action ->
                when (action.actionType) {
                    "ma" -> maCount++
                    "mr" -> mrCount++
                }
            }
            ambientCueLogger?.setPackageName(packageName)
            ambientCueLogger?.setAmbientCueDisplayStatus(maCount, mrCount)
            ambientCueAceLogger?.reportInsightEvent(AmbientCueAceLogger.EVENT_SHOW)
        } else if (!isRootAttached && isSessionStarted) {
            if (globallyFocusedTaskId != targetTaskId) {
                ambientCueLogger?.setLoseFocusMillis()
            }
            ambientCueLogger?.flushAmbientCueEventReported()
            ambientCueLogger?.clear()
            isSessionStarted = false
            ambientCueAceLogger?.reportInsightEvent(AmbientCueAceLogger.EVENT_HIDE)
        }

        val isGestureNav = ambientCueInteractor.isGestureNav.value
        val isTaskBarVisible = ambientCueInteractor.isTaskBarVisible.value

        pillStyle = when {
            isDesktopFormFactor -> PillStyleViewModel.DesktopPillStyle
            isGestureNav && !isTaskBarVisible -> PillStyleViewModel.NavBarPillStyle
            else -> {
                val position = if (isGestureNav) null else ambientCueInteractor.recentsButtonPosition.value
                PillStyleViewModel.ShortPillStyle(position?.toComposeRect())
            }
        }

        if (isRootAttached) {
            if (isExpanded) {
                cancelDeactivation()
            } else {
                delayAndDeactivateCueBar()
            }
        } else {
            cancelDeactivation()
        }

        updateActionViewModelList()

        isVisible = isRootAttached &&
            !ambientCueInteractor.isOccludedBySystemUi.value &&
            actions.isNotEmpty()

        if (oldVisibility != isVisible) {
            onVisibilityChanged(isVisible)
        }
    }

    fun onUnfilteredActionsChange(newActions: List<ActionModel>) {
        val updateState = {
            currentUnfilteredActions = newActions
            targetTaskId = if (currentUnfilteredActions.isNotEmpty()) {
                currentUnfilteredActions[0].taskId
            } else {
                -1
            }
            recalculateStates()
        }

        actionUpdateJob?.cancel()
        if (newActions.isEmpty()) {
            actionUpdateJob = scope.launch {
                delay(ACTIONS_DEBOUNCE_MS)
                updateState()
            }
        } else {
            updateState()
        }
    }

    private fun updateActionViewModelList() {
        val isImeVisible = ambientCueInteractor.isImeVisible.value
        val filteredActions = if (isImeVisible) {
            currentUnfilteredActions.filter { it.isEnabledWithImeVisible }
        } else {
            currentUnfilteredActions
        }

        actions = filteredActions.map { action ->
            ActionViewModel(
                icon = IconViewModel(
                    small = action.icon.small,
                    large = action.icon.large,
                    iconId = action.icon.iconId,
                ),
                label = action.label,
                attribution = action.attribution,
                onClick = {
                    action.onPerformAction()
                    collapse()
                },
                onLongClick = {
                    action.onPerformLongClick()
                    disableLongPressHint()
                },
                actionType = when (action.actionType) {
                    "ma" -> ActionType.MA
                    "mr" -> ActionType.MR
                    else -> ActionType.Unknown
                },
                oneTapEnabled = action.oneTapEnabled,
                oneTapDelayMs = action.oneTapDelayMs,
            )
        }
    }

    fun expand() {
        if (!isExpanded) {
            isExpanded = true
            disableFirstTimeHint()
            cancelDeactivation()
        }
    }

    fun collapse() {
        if (isExpanded) {
            isExpanded = false
            disableLongPressHint()
            delayAndDeactivateCueBar()
        }
    }

    fun hide() {
        ambientCueInteractor.setDeactivated(true)
        isExpanded = false
        disableFirstTimeHint()
        ambientCueLogger?.setClickedCloseButtonStatus()
        ambientCueInteractor.reportCloseEvent()
    }

    fun cancelDeactivation() {
        deactivateCueBarJob?.cancel()
    }

    fun delayAndDeactivateCueBar() {
        deactivateCueBarJob?.cancel()
        val timeoutMs = ambientCueInteractor.ambientCueTimeoutMs.value.toLong()
        deactivateCueBarJob = scope.launch {
            delay(timeoutMs)
            ambientCueInteractor.setDeactivated(true)
            ambientCueLogger?.setReachedTimeoutStatus()
        }
    }

    fun activate() {
        recalculateStates()
    }

    fun deactivate() {
        listeners.forEach { it.close() }
        listeners.clear()
        cancelDeactivation()
        actionUpdateJob?.cancel()
        onVisibilityChanged = {}
    }

    fun disableFirstTimeHint() {
        showFirstTimeEducation = false
    }

    fun disableLongPressHint() {
        showLongPressEducation = false
    }

    fun dump(pw: PrintWriter, prefix: String) {
        pw.println("$prefix AmbientCueViewModel:")
        pw.println("$prefix   isVisible: $isVisible")
        pw.println("$prefix   isExpanded: $isExpanded")
        pw.println("$prefix   pillStyle: ${pillStyle::class.simpleName}")
        pw.println("$prefix   actions: ${actions.size} actions")
        pw.println("$prefix   targetTaskId: $targetTaskId")
        pw.println("$prefix   isSessionStarted: $isSessionStarted")
    }

    companion object {
        private const val TAG = "AmbientCueVM"
        const val ACTIONS_DEBOUNCE_MS = 300L
    }
}
