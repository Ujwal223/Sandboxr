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

import android.content.pm.ActivityInfo
import android.os.Handler
import android.os.Looper
import com.sandboxr.launcher.BaseActivity

/**
 * Utility class to manage activity orientation and screen rotation requests.
 */
open class RotationHelper(private val activity: BaseActivity) {

    private val handler = Handler(Looper.getMainLooper())
    private var stateHandlerRequest = REQUEST_NONE
    private var currentTransitionRequest = REQUEST_NONE
    private var currentStateRequest = REQUEST_NONE

    private var destroyed = false
    private var lastActivityFlags = -999

    open fun setStateHandlerRequest(request: Int) {
        if (stateHandlerRequest != request) {
            stateHandlerRequest = request
            updateOrientation()
        }
    }

    open fun setCurrentTransitionRequest(request: Int) {
        if (currentTransitionRequest != request) {
            currentTransitionRequest = request
            updateOrientation()
        }
    }

    open fun setCurrentStateRequest(request: Int) {
        if (currentStateRequest != request) {
            currentStateRequest = request
            updateOrientation()
        }
    }

    private fun updateOrientation() {
        if (destroyed) return
        val request = when {
            stateHandlerRequest != REQUEST_NONE -> stateHandlerRequest
            currentTransitionRequest != REQUEST_NONE -> currentTransitionRequest
            currentStateRequest != REQUEST_NONE -> currentStateRequest
            else -> REQUEST_NONE
        }

        val orientation = when (request) {
            REQUEST_ROTATE -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            REQUEST_LOCK -> ActivityInfo.SCREEN_ORIENTATION_LOCKED
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        if (orientation != lastActivityFlags) {
            lastActivityFlags = orientation
            handler.post {
                if (!destroyed) {
                    try {
                        activity.requestedOrientation = orientation
                    } catch (e: Exception) {
                        // Ignored if window token is detached or non-resizable
                    }
                }
            }
        }
    }

    open fun destroy() {
        destroyed = true
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        const val REQUEST_NONE = 0
        const val REQUEST_ROTATE = 1
        const val REQUEST_LOCK = 2
    }
}
