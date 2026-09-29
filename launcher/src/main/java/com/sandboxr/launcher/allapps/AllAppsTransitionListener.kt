/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.sandboxr.launcher.allapps

/**
 * Listener interface for observing All Apps drawer open, close, and progress transitions.
 */
interface AllAppsTransitionListener {
    /**
     * Called when the transition starts.
     * @param toAllApps true if transitioning towards All Apps, false if closing towards Home workspace.
     */
    fun onAllAppsTransitionStart(toAllApps: Boolean)

    /**
     * Called when the transition completes.
     * @param toAllApps true if the final state is All Apps, false if Home workspace.
     */
    fun onAllAppsTransitionEnd(toAllApps: Boolean)
}
