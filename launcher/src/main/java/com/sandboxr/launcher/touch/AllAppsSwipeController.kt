/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.sandboxr.launcher.touch

import android.view.MotionEvent
import android.view.animation.Interpolator
import com.sandboxr.launcher.AbstractFloatingView
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.anim.Interpolators
import com.sandboxr.launcher.states.StateAnimationConfig

/** Returns a reversed version of the given [Interpolator]. */
private fun reverseInterpolator(interpolator: Interpolator): Interpolator =
    Interpolator { input -> 1f - interpolator.getInterpolation(1f - input) }

/**
 * TouchController to switch between NORMAL and ALL_APPS state via vertical swipe gestures.
 */
class AllAppsSwipeController(launcher: Launcher) :
    AbstractStateChangeTouchController(launcher, SingleAxisSwipeDetector.VERTICAL) {

    override fun canInterceptTouch(ev: MotionEvent): Boolean {
        if (mCurrentAnimation != null) {
            return true
        }
        if (AbstractFloatingView.getTopOpenView(mLauncher) != null) {
            return false
        }
        if (!mLauncher.isInState(LauncherState.NORMAL) && !mLauncher.isInState(LauncherState.ALL_APPS)) {
            return false
        }
        return true
    }

    override fun getTargetState(
        fromState: LauncherState,
        isDragTowardPositive: Boolean
    ): LauncherState {
        return if (fromState == LauncherState.NORMAL && shouldOpenAllApps(isDragTowardPositive)) {
            LauncherState.ALL_APPS
        } else if (fromState == LauncherState.ALL_APPS && !isDragTowardPositive) {
            LauncherState.NORMAL
        } else {
            fromState
        }
    }

    override fun initCurrentAnimation(): Float {
        val range = getShiftRange()
        val toState = mToState ?: LauncherState.ALL_APPS
        val fromState = mFromState ?: LauncherState.NORMAL
        val config = getConfigForStates(fromState, toState)
        config.duration = (2 * range).toLong()

        mCurrentAnimation = mLauncher.getStateManager().createAnimationToNewWorkspace(toState, config)
        val startVerticalShift = fromState.getVerticalProgress(mLauncher) * range
        val endVerticalShift = toState.getVerticalProgress(mLauncher) * range
        val totalShift = endVerticalShift - startVerticalShift
        return if (totalShift != 0f) 1f / totalShift else 1f
    }

    override fun getConfigForStates(
        fromState: LauncherState,
        toState: LauncherState
    ): StateAnimationConfig {
        val config = super.getConfigForStates(fromState, toState)
        config.animProps = config.animProps or StateAnimationConfig.USER_CONTROLLED
        if (fromState == LauncherState.NORMAL && toState == LauncherState.ALL_APPS) {
            applyNormalToAllAppsAnimConfig(config)
        } else if (fromState == LauncherState.ALL_APPS && toState == LauncherState.NORMAL) {
            applyAllAppsToNormalConfig(config)
        }
        return config
    }

    companion object {
        private const val ALL_APPS_SCRIM_VISIBLE_THRESHOLD = 0.1f
        private const val ALL_APPS_STAGGERED_FADE_THRESHOLD = 0.5f

        private val ALL_APPS_SCRIM_RESPONDER: Interpolator =
            Interpolator { input ->
                Interpolators.clampToProgress(
                    Interpolators.LINEAR,
                    ALL_APPS_SCRIM_VISIBLE_THRESHOLD,
                    ALL_APPS_STAGGERED_FADE_THRESHOLD
                ).getInterpolation(input)
            }

        private val ALL_APPS_SHEET_DEPTH: Interpolator = Interpolators.DECELERATED_EASE

        @JvmStatic
        fun applyAllAppsToNormalConfig(config: StateAnimationConfig) {
            config.setInterpolator(
                StateAnimationConfig.ANIM_SCRIM_FADE,
                reverseInterpolator(ALL_APPS_SCRIM_RESPONDER)
            )
            config.setInterpolator(StateAnimationConfig.ANIM_ALL_APPS_FADE, Interpolators.FINAL_FRAME)
            if (!config.isUserControlled()) {
                config.duration = 200
                config.setInterpolator(
                    StateAnimationConfig.ANIM_VERTICAL_PROGRESS,
                    Interpolators.ACCELERATE
                )
            }
            config.setInterpolator(
                StateAnimationConfig.ANIM_WORKSPACE_SCALE,
                reverseInterpolator(ALL_APPS_SHEET_DEPTH)
            )
            config.setInterpolator(
                StateAnimationConfig.ANIM_HOTSEAT_SCALE,
                reverseInterpolator(ALL_APPS_SHEET_DEPTH)
            )
            config.setInterpolator(
                StateAnimationConfig.ANIM_DEPTH,
                reverseInterpolator(ALL_APPS_SHEET_DEPTH)
            )
        }

        @JvmStatic
        fun applyNormalToAllAppsAnimConfig(config: StateAnimationConfig) {
            config.setInterpolator(StateAnimationConfig.ANIM_ALL_APPS_FADE, Interpolators.INSTANT)
            config.setInterpolator(StateAnimationConfig.ANIM_SCRIM_FADE, ALL_APPS_SCRIM_RESPONDER)
            if (!config.isUserControlled()) {
                config.setInterpolator(
                    StateAnimationConfig.ANIM_VERTICAL_PROGRESS,
                    Interpolators.EMPHASIZED
                )
            }
            config.setInterpolator(StateAnimationConfig.ANIM_WORKSPACE_SCALE, ALL_APPS_SHEET_DEPTH)
            config.setInterpolator(StateAnimationConfig.ANIM_HOTSEAT_SCALE, ALL_APPS_SHEET_DEPTH)
            config.setInterpolator(StateAnimationConfig.ANIM_DEPTH, ALL_APPS_SHEET_DEPTH)
        }
    }
}
