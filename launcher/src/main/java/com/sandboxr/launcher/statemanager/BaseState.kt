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

package com.sandboxr.launcher.statemanager

import com.sandboxr.launcher.views.ActivityContext

/**
 * Interface representing a state of a [StatefulContainer].
 */
interface BaseState<T> {

    fun getTransitionDuration(context: ActivityContext, isToState: Boolean): Int

    fun getHistoryForState(previousState: T): T

    fun hasFlag(flagMask: Int): Boolean

    fun shouldDisableRestore(isUiModeChange: Boolean = false): Boolean {
        return if (isUiModeChange) {
            hasFlag(FLAG_DISABLE_RESTORE_ABSOLUTE)
        } else {
            hasFlag(FLAG_DISABLE_RESTORE_ABSOLUTE or FLAG_DISABLE_RESTORE_EXCEPT_UI_MODE_CHANGE)
        }
    }

    fun isTaskViewInteractive(): Boolean {
        return hasFlag(FLAG_IS_TASK_VIEW_INTERACTIVE)
    }

    fun isInOverview(): Boolean {
        return false
    }

    fun shouldPreserveDataStateOnReapply(): Boolean {
        return false
    }

    companion object {
        const val FLAG_NON_INTERACTIVE = 1 shl 0
        const val FLAG_DISABLE_RESTORE_ABSOLUTE = 1 shl 1
        const val FLAG_DISABLE_RESTORE_EXCEPT_UI_MODE_CHANGE = 1 shl 2
        const val FLAG_IS_TASK_VIEW_INTERACTIVE = 1 shl 3

        @JvmStatic
        fun getFlag(index: Int): Int {
            return 1 shl (index + 4)
        }
    }
}
