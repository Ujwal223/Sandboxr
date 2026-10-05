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

package com.sandboxr.launcher.cuebar.domain.interactor

import android.graphics.Rect
import com.sandboxr.launcher.cuebar.data.ActionModel
import com.sandboxr.launcher.cuebar.data.repository.AmbientCueRepository
import com.sandboxr.launcher.util.ListenableRef

/**
 * Interactor mediating between CueBar ViewModels and the [AmbientCueRepository].
 */
class AmbientCueInteractor(
    private val ambientCueRepository: AmbientCueRepository
) {

    /** ListenableRef of actions to be displayed in the CueBar. */
    val actions: ListenableRef<List<ActionModel>> = ambientCueRepository.actions

    /** ListenableRef indicating if the Input Method Editor (keyboard) is visible. */
    val isImeVisible: ListenableRef<Boolean> = ambientCueRepository.isImeVisible

    /** ListenableRef indicating if the CueBar is occluded by System UI. */
    val isOccludedBySystemUi: ListenableRef<Boolean> = ambientCueRepository.isOccludedBySystemUi

    /** ListenableRef providing the timeout duration for the Ambient Cue. */
    val ambientCueTimeoutMs: ListenableRef<Int> = ambientCueRepository.ambientCueTimeoutMs

    /** ListenableRef indicating if gesture navigation is enabled. */
    val isGestureNav: ListenableRef<Boolean> = ambientCueRepository.isGestureNav

    /** ListenableRef indicating if the Taskbar is fully visible and not stashed. */
    val isTaskBarVisible: ListenableRef<Boolean> = ambientCueRepository.isTaskBarVisible

    /** ListenableRef providing the position of the recents button (in 3-button nav). */
    val recentsButtonPosition: ListenableRef<Rect?> = ambientCueRepository.recentsButtonPosition

    /** ListenableRef indicating if the CueBar is deactivated. */
    val isDeactivated: ListenableRef<Boolean> = ambientCueRepository.isDeactivated

    /** ListenableRef indicating if Ambient Cue is enabled in settings. */
    val isAmbientCueEnabled: ListenableRef<Boolean> = ambientCueRepository.isAmbientCueEnabled

    /** ListenableRef indicating test mode. */
    val isTestMode: ListenableRef<Boolean> = ambientCueRepository.isTestMode

    /** Task ID which is globally focused on display. */
    val globallyFocusedTaskId: ListenableRef<Int> = ambientCueRepository.globallyFocusedTaskId

    /** Package name of the front task. */
    val frontTaskPackageName: ListenableRef<String> = ambientCueRepository.frontTaskPackageName

    /** Sets the deactivated state of the Ambient Cue. */
    fun setDeactivated(isDeactivated: Boolean) {
        ambientCueRepository.isDeactivated.dispatchValue(isDeactivated)
    }

    /** Reports a user close/dismiss event. */
    fun reportCloseEvent() {
        ambientCueRepository.reportCloseEvent()
    }
}
