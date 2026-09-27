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

import com.sandboxr.launcher.statemanager.BaseState
import com.sandboxr.launcher.views.ActivityContext

/**
 * Base state for various states used in the Launcher.
 */
open class LauncherState(
    @JvmField val ordinal: Int,
    @JvmField val statsLogOrdinal: Int,
    flags: Int
) : BaseState<LauncherState> {

    @JvmField
    val isRecentsViewVisible: Boolean = (flags and FLAG_RECENTS_VIEW_VISIBLE) != 0

    private val mFlags: Int = flags

    init {
        if (ordinal in 0 until sAllStates.size) {
            sAllStates[ordinal] = this
        }
    }

    override fun hasFlag(flagMask: Int): Boolean {
        return (mFlags and flagMask) != 0
    }

    override fun getTransitionDuration(context: ActivityContext, isToState: Boolean): Int {
        return 300
    }

    override fun getHistoryForState(previousState: LauncherState): LauncherState {
        return NORMAL
    }

    open fun getVisibleElements(context: ActivityContext): Int {
        return HOTSEAT_ICONS or WORKSPACE_PAGE_INDICATOR
    }

    override fun toString(): String {
        return when (this) {
            NORMAL -> "NORMAL"
            SPRING_LOADED -> "SPRING_LOADED"
            ALL_APPS -> "ALL_APPS"
            OVERVIEW -> "OVERVIEW"
            EDIT_MODE -> "EDIT_MODE"
            BACKGROUND_APP -> "BACKGROUND_APP"
            HINT_STATE -> "HINT_STATE"
            QUICK_SWITCH_FROM_HOME -> "QUICK_SWITCH_FROM_HOME"
            DESKTOP_DRAG_MODE -> "DESKTOP_DRAG_MODE"
            OVERVIEW_MODAL_TASK -> "OVERVIEW_MODAL_TASK"
            OVERVIEW_SPLIT_SELECT -> "OVERVIEW_SPLIT_SELECT"
            else -> "LauncherState($ordinal)"
        }
    }

    companion object {
        const val NONE = 0
        const val HOTSEAT_ICONS = 1 shl 0
        const val ALL_APPS_CONTENT = 1 shl 1
        const val VERTICAL_SWIPE_INDICATOR = 1 shl 2
        const val OVERVIEW_ACTIONS = 1 shl 3
        const val CLEAR_ALL_BUTTON = 1 shl 4
        const val WORKSPACE_PAGE_INDICATOR = 1 shl 5
        const val SPLIT_PLACHOLDER_VIEW = 1 shl 6
        const val FLOATING_SEARCH_BAR = 1 shl 7
        const val ADD_DESK_BUTTON = 1 shl 8

        @JvmField
        val FLAG_MULTI_PAGE = BaseState.getFlag(0)
        @JvmField
        val FLAG_WORKSPACE_INACCESSIBLE = BaseState.getFlag(1)
        @JvmField
        val FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED = BaseState.getFlag(2)
        @JvmField
        val FLAG_WORKSPACE_HAS_BACKGROUNDS = BaseState.getFlag(3)
        @JvmField
        val FLAG_HAS_SYS_UI_SCRIM = BaseState.getFlag(4)
        @JvmField
        val FLAG_CLOSE_POPUPS = BaseState.getFlag(5)
        @JvmField
        val FLAG_RECENTS_VIEW_VISIBLE = BaseState.getFlag(6)
        @JvmField
        val FLAG_HOTSEAT_INACCESSIBLE = BaseState.getFlag(7)
        @JvmField
        val FLAG_SKIP_STATE_ANNOUNCEMENT = BaseState.getFlag(8)
        @JvmField
        val FLAG_WORKSPACE_ICONS_BEING_DRAGGED = BaseState.getFlag(9)

        const val NO_OFFSET = 0f
        const val NO_SCALE = 1f

        const val NORMAL_STATE_ORDINAL = 0
        const val SPRING_LOADED_STATE_ORDINAL = 1
        const val OVERVIEW_STATE_ORDINAL = 2
        const val ALL_APPS_STATE_ORDINAL = 3
        const val EDIT_MODE_STATE_ORDINAL = 4
        const val HINT_STATE_ORDINAL = 5
        const val BACKGROUND_APP_STATE_ORDINAL = 6
        const val DESKTOP_DRAG_MODE_ORDINAL = 7
        const val OVERVIEW_MODAL_TASK_STATE_ORDINAL = 8
        const val QUICK_SWITCH_STATE_ORDINAL = 9
        const val OVERVIEW_SPLIT_SELECT_ORDINAL = 10

        private val sAllStates = arrayOfNulls<LauncherState>(12)

        @JvmField
        val NORMAL: LauncherState = object : LauncherState(
            NORMAL_STATE_ORDINAL,
            1 /* LAUNCHER_STATE_HOME */,
            BaseState.FLAG_DISABLE_RESTORE_EXCEPT_UI_MODE_CHANGE or
                    FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED or
                    FLAG_HAS_SYS_UI_SCRIM
        ) {
            override fun getTransitionDuration(context: ActivityContext, isToState: Boolean): Int = 0
            override fun getVisibleElements(context: ActivityContext): Int =
                HOTSEAT_ICONS or WORKSPACE_PAGE_INDICATOR
        }

        @JvmField
        val SPRING_LOADED: LauncherState = object : LauncherState(
            SPRING_LOADED_STATE_ORDINAL,
            1,
            FLAG_WORKSPACE_INACCESSIBLE or FLAG_HOTSEAT_INACCESSIBLE
        ) {
            override fun getVisibleElements(context: ActivityContext): Int = NONE
        }

        @JvmField
        val ALL_APPS: LauncherState = object : LauncherState(
            ALL_APPS_STATE_ORDINAL,
            2,
            FLAG_CLOSE_POPUPS or FLAG_WORKSPACE_INACCESSIBLE
        ) {
            override fun getVisibleElements(context: ActivityContext): Int = ALL_APPS_CONTENT
            override fun getHistoryForState(previousState: LauncherState): LauncherState = NORMAL
        }

        @JvmField
        val OVERVIEW: LauncherState = object : LauncherState(
            OVERVIEW_STATE_ORDINAL,
            3,
            FLAG_RECENTS_VIEW_VISIBLE or FLAG_WORKSPACE_INACCESSIBLE
        ) {
            override fun isInOverview(): Boolean = true
            override fun getVisibleElements(context: ActivityContext): Int =
                OVERVIEW_ACTIONS or CLEAR_ALL_BUTTON
        }

        @JvmField
        val EDIT_MODE: LauncherState = object : LauncherState(
            EDIT_MODE_STATE_ORDINAL,
            4,
            FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED
        ) {}

        @JvmField
        val BACKGROUND_APP: LauncherState = object : LauncherState(
            BACKGROUND_APP_STATE_ORDINAL,
            5,
            BaseState.FLAG_NON_INTERACTIVE or FLAG_WORKSPACE_INACCESSIBLE
        ) {}

        @JvmField
        val HINT_STATE: LauncherState = object : LauncherState(
            HINT_STATE_ORDINAL,
            6,
            BaseState.FLAG_NON_INTERACTIVE
        ) {}

        @JvmField
        val QUICK_SWITCH_FROM_HOME: LauncherState = object : LauncherState(
            QUICK_SWITCH_STATE_ORDINAL,
            7,
            FLAG_RECENTS_VIEW_VISIBLE
        ) {}

        @JvmField
        val DESKTOP_DRAG_MODE: LauncherState = object : LauncherState(
            DESKTOP_DRAG_MODE_ORDINAL,
            8,
            FLAG_WORKSPACE_INACCESSIBLE
        ) {}

        @JvmField
        val OVERVIEW_MODAL_TASK: LauncherState = object : LauncherState(
            OVERVIEW_MODAL_TASK_STATE_ORDINAL,
            9,
            FLAG_RECENTS_VIEW_VISIBLE
        ) {
            override fun isInOverview(): Boolean = true
        }

        @JvmField
        val OVERVIEW_SPLIT_SELECT: LauncherState = object : LauncherState(
            OVERVIEW_SPLIT_SELECT_ORDINAL,
            10,
            FLAG_RECENTS_VIEW_VISIBLE
        ) {
            override fun isInOverview(): Boolean = true
        }

        @JvmStatic
        fun values(): Array<LauncherState> {
            return sAllStates.filterNotNull().toTypedArray()
        }
    }
}
