/*
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

package com.sandboxr.launcher.inputconsumers

import android.content.Context
import android.os.Vibrator
import android.os.VibrationEffect
import android.util.Log
import android.view.ViewConfiguration

/**
 * Handler for the navigation handle long-press gesture.
 * Manages haptic feedback, animation triggers, and long press action dispatch.
 */
class NavHandleLongPressHandler(
    val context: Context,
    var onLongPressAction: ((NavHandle) -> Unit)? = null
) {

    private val vibrator: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    var isPendingInvocation: Boolean = false
        private set

    /**
     * Returns the long-press duration in milliseconds.
     */
    fun getLongPressTimeout(): Long = ViewConfiguration.getLongPressTimeout().toLong()

    /**
     * Generates a Runnable executed when long press timeout triggers.
     */
    fun getLongPressRunnable(navHandle: NavHandle, displayId: Int): Runnable? {
        isPendingInvocation = true
        return Runnable {
            Log.d(TAG, "Nav handle long press triggered on display $displayId")
            performHaptic()
            isPendingInvocation = false
            onLongPressAction?.invoke(navHandle)
        }
    }

    fun onTouchStarted(navHandle: NavHandle) {
        isPendingInvocation = false
        navHandle.animateNavBarLongPress(isTouchDown = true, shrink = false, durationMs = getLongPressTimeout())
    }

    fun onTouchFinished(navHandle: NavHandle, reason: String) {
        Log.d(TAG, "Nav handle touch finished: $reason")
        isPendingInvocation = false
        navHandle.animateNavBarLongPress(isTouchDown = false, shrink = false, durationMs = 160)
    }

    private fun performHaptic() {
        try {
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to vibrate for nav handle long press", e)
        }
    }

    companion object {
        private const val TAG = "NavHandleLongPress"
    }
}
