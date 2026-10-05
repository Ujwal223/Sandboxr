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

package com.sandboxr.launcher.recents.fallback

import com.sandboxr.launcher.statemanager.BaseState
import com.sandboxr.launcher.views.ActivityContext

open class RecentsState(
    @JvmField val ordinal: Int,
    private val mFlags: Int = 0
) : BaseState<RecentsState> {

    init {
        if (ordinal in 0 until sAllStates.size) {
            sAllStates[ordinal] = this
        }
    }

    override fun hasFlag(flagMask: Int): Boolean = (mFlags and flagMask) != 0

    override fun getTransitionDuration(context: ActivityContext, isToState: Boolean): Int = 250

    override fun getHistoryForState(previousState: RecentsState): RecentsState = DEFAULT

    override fun toString(): String = when (ordinal) {
        DEFAULT_STATE_ORDINAL -> "RECENTS_DEFAULT"
        MODAL_TASK_ORDINAL -> "RECENTS_MODAL_TASK"
        BACKGROUND_APP_ORDINAL -> "RECENTS_BACKGROUND_APP"
        HOME_STATE_ORDINAL -> "RECENTS_HOME"
        else -> "RecentsState($ordinal)"
    }

    companion object {
        const val DEFAULT_STATE_ORDINAL = 0
        const val MODAL_TASK_ORDINAL = 1
        const val BACKGROUND_APP_ORDINAL = 2
        const val HOME_STATE_ORDINAL = 3

        private val sAllStates = arrayOfNulls<RecentsState>(4)

        @JvmField
        val DEFAULT: RecentsState = RecentsState(DEFAULT_STATE_ORDINAL)

        @JvmField
        val MODAL_TASK: RecentsState = RecentsState(MODAL_TASK_ORDINAL)

        @JvmField
        val BACKGROUND_APP: RecentsState = RecentsState(BACKGROUND_APP_ORDINAL)

        @JvmField
        val HOME_STATE: RecentsState = RecentsState(HOME_STATE_ORDINAL)
    }
}
