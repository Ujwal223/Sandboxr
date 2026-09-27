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

import android.content.res.Configuration
import androidx.annotation.CallSuper
import com.sandboxr.launcher.views.ActivityContext

/**
 * Interface for a container that can be managed by a [StateManager].
 */
interface StatefulContainer<STATE_TYPE : BaseState<STATE_TYPE>> : ActivityContext {

    fun getStateManager(): StateManager<STATE_TYPE, *>

    fun collectStateHandlers(out: MutableList<StateManager.StateHandler<STATE_TYPE>>)

    fun createAtomicAnimationFactory(): StateManager.AtomicAnimationFactory<STATE_TYPE> {
        return StateManager.AtomicAnimationFactory(0)
    }

    fun onStateSetEnd(state: STATE_TYPE) {}

    fun onRepeatStateSetAborted(state: STATE_TYPE) {}

    @CallSuper
    fun onStateSetStart(state: STATE_TYPE) {}

    fun isInState(state: STATE_TYPE): Boolean {
        return getStateManager().state == state
    }

    fun shouldAnimateStateChange(): Boolean

    fun handleConfigurationChanged(newConfig: Configuration) {}

    fun reapplyUi() {}
}
