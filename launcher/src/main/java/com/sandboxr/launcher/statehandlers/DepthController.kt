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

package com.sandboxr.launcher.statehandlers

import android.util.FloatProperty
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.anim.Interpolators
import com.sandboxr.launcher.anim.PendingAnimation
import com.sandboxr.launcher.statemanager.StateManager
import com.sandboxr.launcher.states.SpringLoadedState
import com.sandboxr.launcher.states.StateAnimationConfig
import com.sandboxr.launcher.util.MultiPropertyFactory

/**
 * StateHandler implementation that manages window blur and depth levels across Launcher states.
 */
open class DepthController(private val launcher: Launcher) : BaseDepthController, StateManager.StateHandler<LauncherState> {

    var currentDepth: Float = 0f
        protected set

    private val depthProperty = object : FloatProperty<DepthController>("depth") {
        override fun setValue(obj: DepthController, value: Float) {
            obj.currentDepth = value
        }

        override fun get(obj: DepthController): Float = obj.currentDepth
    }

    private val multiPropertyFactory = MultiPropertyFactory(
        this,
        depthProperty,
        3,
        { a, b -> maxOf(a, b) }
    )

    override val stateDepth: MultiPropertyFactory<*>.MultiProperty = multiPropertyFactory[0]
    override val widgetDepth: MultiPropertyFactory<*>.MultiProperty = multiPropertyFactory[1]
    override val folderZoom: MultiPropertyFactory<*>.MultiProperty = multiPropertyFactory[2]

    override fun setState(state: LauncherState) {
        val target = if (state is SpringLoadedState) SpringLoadedState.DEPTH_15_PERCENT else 0f
        stateDepth.setValue(target)
    }

    override fun setStateWithAnimation(
        toState: LauncherState,
        config: StateAnimationConfig,
        animation: PendingAnimation
    ) {
        if (config.hasAnimationFlag(StateAnimationConfig.SKIP_DEPTH_CONTROLLER)) return
        val target = if (toState is SpringLoadedState) SpringLoadedState.DEPTH_15_PERCENT else 0f
        val interpolator = config.getInterpolator(StateAnimationConfig.ANIM_DEPTH, Interpolators.LINEAR)
        animation.setFloat(this, depthProperty, target, interpolator)
    }

    override fun onBackProgressed(toState: LauncherState, backProgress: Float) {
        val target = if (toState is SpringLoadedState) SpringLoadedState.DEPTH_15_PERCENT else 0f
        stateDepth.setValue(target * backProgress)
    }

    override fun onBackCancelled(toState: LauncherState) {
        setState(launcher.getStateManager().state)
    }
}
