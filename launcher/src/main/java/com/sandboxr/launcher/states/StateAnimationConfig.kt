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

package com.sandboxr.launcher.states

import android.view.animation.Interpolator

/**
 * Utility configuration class for configuring state animation properties, durations, and interpolators.
 */
open class StateAnimationConfig {

    @JvmField
    var duration: Long = 0

    @JvmField
    var animProps: Int = 0

    @JvmField
    var animFlags: Int = 0

    protected val interpolators: Array<Interpolator?> = arrayOfNulls(ANIM_TYPES_COUNT)

    open fun copyTo(target: StateAnimationConfig) {
        target.duration = duration
        target.animFlags = animFlags
        target.animProps = animProps
        for (i in 0 until ANIM_TYPES_COUNT) {
            target.interpolators[i] = interpolators[i]
        }
    }

    open fun isUserControlled(): Boolean = (animProps and USER_CONTROLLED) != 0

    open fun <T : Interpolator?> getInterpolator(animId: Int, fallback: T): T {
        return if (animId in 0 until ANIM_TYPES_COUNT && interpolators[animId] != null) {
            @Suppress("UNCHECKED_CAST")
            interpolators[animId] as T
        } else {
            fallback
        }
    }

    open fun setInterpolator(animId: Int, interpolator: Interpolator?) {
        if (animId in 0 until ANIM_TYPES_COUNT) {
            interpolators[animId] = interpolator
        }
    }

    open fun hasAnimationFlag(flag: Int): Boolean = (animFlags and flag) != 0

    companion object {
        const val SKIP_ALL_ANIMATIONS = 1 shl 0
        const val SKIP_OVERVIEW = 1 shl 1
        const val SKIP_DEPTH_CONTROLLER = 1 shl 2
        const val SKIP_SCRIM = 1 shl 3

        const val USER_CONTROLLED = 1 shl 0
        const val HANDLE_STATE_APPLY = 1 shl 1

        const val ANIM_VERTICAL_PROGRESS = 0
        const val ANIM_WORKSPACE_SCALE = 1
        const val ANIM_WORKSPACE_TRANSLATE = 2
        const val ANIM_WORKSPACE_FADE = 3
        const val ANIM_HOTSEAT_SCALE = 4
        const val ANIM_HOTSEAT_TRANSLATE = 5
        const val ANIM_OVERVIEW_SCALE = 6
        const val ANIM_OVERVIEW_TRANSLATE_X = 7
        const val ANIM_OVERVIEW_TRANSLATE_Y = 8
        const val ANIM_OVERVIEW_FADE = 9
        const val ANIM_ALL_APPS_FADE = 10
        const val ANIM_SCRIM_FADE = 11
        const val ANIM_OVERVIEW_MODAL = 12
        const val ANIM_DEPTH = 13
        const val ANIM_OVERVIEW_ACTIONS_FADE = 14
        const val ANIM_WORKSPACE_PAGE_TRANSLATE_X = 15
        const val ANIM_HOTSEAT_FADE = 16
        const val ANIM_OVERVIEW_SPLIT_SELECT_FLOATING_TASK_TRANSLATE_OFFSCREEN = 17
        const val ANIM_OVERVIEW_SPLIT_SELECT_INSTRUCTIONS_FADE = 18
        const val ANIM_ALL_APPS_KEYBOARD_FADE = 19

        const val ANIM_TYPES_COUNT = 21
    }
}
