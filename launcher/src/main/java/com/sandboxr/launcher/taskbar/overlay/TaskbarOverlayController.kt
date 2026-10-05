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

package com.sandboxr.launcher.taskbar.overlay

import android.content.Context
import android.content.ContextWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import com.sandboxr.launcher.DeviceProfile
import com.sandboxr.launcher.dagger.ActivityContextComponent
import com.sandboxr.launcher.views.ActivityContext

/**
 * Overlay context used when floating windows or all-apps sheets are open above Taskbar.
 */
class TaskbarOverlayContext(
    base: Context,
    private val deviceProfile: DeviceProfile,
) : ContextWrapper(base), ActivityContext, LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun getActivityComponent(): ActivityContextComponent? = null
    override fun getDeviceProfile(): DeviceProfile = deviceProfile
    override fun getRootView(): View? = null
    override fun getLayoutInflater(): LayoutInflater? = LayoutInflater.from(this)
}

/** Controller managing Taskbar overlay windows. */
class TaskbarOverlayController(
    val context: Context,
) {
    var isOverlayOpen: Boolean = false
        private set

    fun openOverlay() {
        isOverlayOpen = true
    }

    fun closeOverlay() {
        isOverlayOpen = false
    }
}
