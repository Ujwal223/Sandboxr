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

import android.animation.ValueAnimator
import android.util.FloatProperty
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.sandboxr.launcher.anim.PropertySetter
import com.sandboxr.launcher.states.StateAnimationConfig

/**
 * Manages animations and state transitions for the [Workspace] and [Hotseat] across
 * [LauncherState] changes (NORMAL, SPRING_LOADED, ALL_APPS, OVERVIEW, EDIT_MODE).
 */
open class WorkspaceStateTransitionAnimation(
    protected val mLauncher: Launcher,
    protected val mWorkspace: Workspace<*>
) {

    open fun setState(state: LauncherState) {
        applyWorkspaceState(state, PropertySetter.NO_ANIM_PROPERTY_SETTER, StateAnimationConfig())
    }

    open fun setStateWithAnimation(
        toState: LauncherState,
        config: StateAnimationConfig,
        propertySetter: PropertySetter
    ) {
        applyWorkspaceState(toState, propertySetter, config)
    }

    protected open fun applyWorkspaceState(
        state: LauncherState,
        propertySetter: PropertySetter,
        config: StateAnimationConfig
    ) {
        mWorkspace.setStateWithAnimation(state, config, propertySetter)

        val hotseat = mLauncher.getHotseat()
        if (hotseat != null) {
            val hotseatAlpha = if (state == LauncherState.NORMAL || state == LauncherState.EDIT_MODE) 1f else 0f
            propertySetter.setViewAlpha(
                hotseat,
                hotseatAlpha,
                config.getInterpolator(StateAnimationConfig.ANIM_HOTSEAT_FADE, DecelerateInterpolator())
            )
        }

        val pageIndicator = mWorkspace.getPageIndicator()
        if (pageIndicator != null) {
            val indicatorAlpha = if (state == LauncherState.NORMAL) 1f else 0f
            propertySetter.setViewAlpha(
                pageIndicator,
                indicatorAlpha,
                config.getInterpolator(StateAnimationConfig.ANIM_WORKSPACE_FADE, DecelerateInterpolator())
            )
        }
    }

    companion object {
        @JvmStatic
        fun getWorkspaceSpringScaleAnimator(
            launcher: Launcher,
            workspace: Workspace<*>,
            scale: Float
        ): ValueAnimator {
            val animator = ValueAnimator.ofFloat(workspace.workspaceScale, scale)
            animator.duration = 250
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener { va ->
                workspace.workspaceScale = va.animatedValue as Float
            }
            return animator
        }

        @JvmStatic
        fun <T : View> getSpringScaleAnimator(
            launcher: Launcher,
            v: T,
            scale: Float,
            property: FloatProperty<T>
        ): ValueAnimator {
            val current = property.get(v)
            val animator = ValueAnimator.ofFloat(current, scale)
            animator.duration = 250
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener { va ->
                property.set(v, va.animatedValue as Float)
            }
            return animator
        }
    }
}
