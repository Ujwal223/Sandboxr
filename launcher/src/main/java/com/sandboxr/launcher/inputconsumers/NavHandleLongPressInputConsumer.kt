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
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.sandboxr.launcher.inputconsumers.InputConsumer.Companion.TYPE_NAV_HANDLE_LONG_PRESS

/**
 * Input consumer that intercepts touch events near the navigation handle and initiates
 * long-press detection.
 */
class NavHandleLongPressInputConsumer(
    val context: Context,
    delegate: InputConsumer,
    val navHandle: NavHandle,
    val handler: NavHandleLongPressHandler = NavHandleLongPressHandler(context),
    override val displayId: Int = 0
) : DelegateInputConsumer(displayId, delegate) {

    override val type: Int = TYPE_NAV_HANDLE_LONG_PRESS

    private val mainHandler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val touchSlopSquared = (touchSlop * touchSlop).toFloat()

    private var downX: Float = 0f
    private var downY: Float = 0f
    private var isInsideNavHandle: Boolean = false
    private var longPressRunnable: Runnable? = null
    var isLongPressTriggered: Boolean = false
        private set

    override fun onMotionEvent(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                isLongPressTriggered = false

                val handleBounds = navHandle.getBoundsOnScreen()
                // Check if touch down intersects with the nav handle area (with padding)
                isInsideNavHandle = handleBounds.isEmpty ||
                        (ev.rawX >= handleBounds.left - touchSlop &&
                         ev.rawX <= handleBounds.right + touchSlop &&
                         ev.rawY >= handleBounds.top - touchSlop &&
                         ev.rawY <= handleBounds.bottom + touchSlop)

                if (isInsideNavHandle) {
                    handler.onTouchStarted(navHandle)
                    val r = handler.getLongPressRunnable(navHandle, displayId)
                    if (r != null) {
                        longPressRunnable = Runnable {
                            isLongPressTriggered = true
                            r.run()
                        }
                        mainHandler.postDelayed(longPressRunnable!!, handler.getLongPressTimeout())
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isInsideNavHandle && longPressRunnable != null) {
                    val dx = ev.rawX - downX
                    val dy = ev.rawY - downY
                    val distSquared = dx * dx + dy * dy
                    if (distSquared > touchSlopSquared) {
                        // User moved too much, cancel long press
                        cancelLongPress("touch slop passed")
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                cancelLongPress("touch action up")
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelLongPress("touch action cancel")
            }
        }

        // Forward event to delegate if long press has not consumed/pilfered it
        if (!isLongPressTriggered) {
            super.onMotionEvent(ev)
        }
    }

    private fun cancelLongPress(reason: String) {
        longPressRunnable?.let {
            mainHandler.removeCallbacks(it)
            longPressRunnable = null
            handler.onTouchFinished(navHandle, reason)
        }
        isInsideNavHandle = false
    }

    /**
     * Directly triggers long press for testing.
     */
    fun triggerLongPressForTesting() {
        longPressRunnable?.let {
            mainHandler.removeCallbacks(it)
            it.run()
        }
    }
}
