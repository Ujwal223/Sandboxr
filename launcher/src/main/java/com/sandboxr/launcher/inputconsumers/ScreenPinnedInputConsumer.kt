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
import android.view.MotionEvent
import com.sandboxr.launcher.inputconsumers.InputConsumer.Companion.TYPE_SCREEN_PINNED

/**
 * Input consumer active when an app is pinned (kiosk / task pinning mode).
 * Blocks normal home/recents gestures and shows toast/notification on swipe attempt.
 */
class ScreenPinnedInputConsumer(
    val context: Context,
    override val displayId: Int = 0,
    var onPinnedGestureAttempted: (() -> Unit)? = null
) : InputConsumer {

    override val type: Int = TYPE_SCREEN_PINNED

    private var downY: Float = 0f

    override fun onMotionEvent(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = ev.rawY
            }
            MotionEvent.ACTION_UP -> {
                if (downY - ev.rawY > 100f) {
                    onPinnedGestureAttempted?.invoke()
                }
            }
        }
    }

    override fun allowInterceptByParent(): Boolean = false
}
