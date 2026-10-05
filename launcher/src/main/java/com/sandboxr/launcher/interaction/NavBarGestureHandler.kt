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

package com.sandboxr.launcher.interaction

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import com.sandboxr.launcher.inputconsumers.DefaultNavHandle
import com.sandboxr.launcher.inputconsumers.NavHandle
import com.sandboxr.launcher.inputconsumers.NavHandleLongPressHandler

/**
 * High-level handler managing touch gestures dispatched specifically over the navigation bar.
 */
class NavBarGestureHandler(
    val context: Context,
    val navHandle: NavHandle = DefaultNavHandle()
) {

    private val longPressHandler = NavHandleLongPressHandler(context)

    var onSwipeUp: (() -> Unit)? = null
    var onLongPress: ((NavHandle) -> Unit)? = null
        set(value) {
            field = value
            longPressHandler.onLongPressAction = value
        }

    private var downX = 0f
    private var downY = 0f

    fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                longPressHandler.onTouchStarted(navHandle)
                return true
            }

            MotionEvent.ACTION_UP -> {
                val deltaY = downY - ev.rawY
                if (deltaY > 80f) {
                    longPressHandler.onTouchFinished(navHandle, "swipe up")
                    onSwipeUp?.invoke()
                    return true
                }
                longPressHandler.onTouchFinished(navHandle, "action up")
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                longPressHandler.onTouchFinished(navHandle, "action cancel")
                return true
            }
        }
        return false
    }
}
