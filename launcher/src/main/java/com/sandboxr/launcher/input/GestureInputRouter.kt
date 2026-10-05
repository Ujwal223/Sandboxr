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

package com.sandboxr.launcher.input

import android.content.Context
import android.view.MotionEvent
import com.sandboxr.launcher.inputconsumers.DefaultNavHandle
import com.sandboxr.launcher.inputconsumers.DeviceLockedInputConsumer
import com.sandboxr.launcher.inputconsumers.InputConsumer
import com.sandboxr.launcher.inputconsumers.NavHandle
import com.sandboxr.launcher.inputconsumers.NavHandleLongPressHandler
import com.sandboxr.launcher.inputconsumers.NavHandleLongPressInputConsumer
import com.sandboxr.launcher.inputconsumers.OtherActivityInputConsumer
import com.sandboxr.launcher.inputconsumers.OverviewInputConsumer
import com.sandboxr.launcher.inputconsumers.ScreenPinnedInputConsumer

/**
 * Routes touch and gesture streams to the appropriate [InputConsumer]
 * based on current device and activity states.
 */
class GestureInputRouter(
    val context: Context,
    val displayId: Int = 0
) {

    var isDeviceLocked: Boolean = false
    var isScreenPinned: Boolean = false
    var isInOverview: Boolean = false
    var navHandle: NavHandle = DefaultNavHandle()

    var activeConsumer: InputConsumer = InputConsumer.NO_OP
        private set

    var onSwipeToHome: (() -> Unit)? = null
    var onSwipeAndHoldToRecents: (() -> Unit)? = null
    var onNavHandleLongPress: ((NavHandle) -> Unit)? = null

    /**
     * Re-evaluates state and instantiates the correct consumer for the start of a gesture (ACTION_DOWN).
     */
    fun createInputConsumer(): InputConsumer {
        return when {
            isDeviceLocked -> {
                DeviceLockedInputConsumer(context, displayId)
            }
            isScreenPinned -> {
                ScreenPinnedInputConsumer(context, displayId)
            }
            isInOverview -> {
                OverviewInputConsumer(context, displayId, onSwipeDownToHome = onSwipeToHome)
            }
            else -> {
                val otherActivityConsumer = OtherActivityInputConsumer(
                    context = context,
                    displayId = displayId,
                    onSwipeToHome = onSwipeToHome,
                    onSwipeAndHoldToRecents = onSwipeAndHoldToRecents
                )
                val longPressHandler = NavHandleLongPressHandler(context, onNavHandleLongPress)
                NavHandleLongPressInputConsumer(
                    context = context,
                    delegate = otherActivityConsumer,
                    navHandle = navHandle,
                    handler = longPressHandler,
                    displayId = displayId
                )
            }
        }
    }

    /**
     * Feeds a motion event into the gesture router.
     */
    fun onMotionEvent(ev: MotionEvent) {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            activeConsumer = createInputConsumer()
        }
        activeConsumer.onMotionEvent(ev)
    }
}
