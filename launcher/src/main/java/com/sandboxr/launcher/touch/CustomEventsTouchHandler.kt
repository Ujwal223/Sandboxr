/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.sandboxr.launcher.touch

import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.sandboxr.launcher.MotionEventsUtils.isTrackpadMotionEvent
import com.sandboxr.launcher.touch.CustomActionsListener.Companion.ACTION_LAUNCH
import com.sandboxr.launcher.touch.CustomActionsListener.Companion.ACTION_POPUP_MENU
import com.sandboxr.launcher.touch.CustomActionsListener.Companion.ACTION_START_DRAG
import com.sandboxr.launcher.util.TouchUtil
import kotlin.math.abs

/**
 * Handles touch events to trigger custom actions on a view, based on gestures like taps, long
 * presses, and mouse-specific interactions like right clicks and drags.
 */
class CustomEventsTouchHandler(
    private val view: View,
    private val defaultTouchHandler: (MotionEvent) -> Boolean = { false },
    private val shouldIgnoreTouchDown: (MotionEvent) -> Boolean = { false }
) : GestureDetector.SimpleOnGestureListener(), CustomTouchDelegate {

    private var downX = 0f
    private var downY = 0f

    private val gestureDetector: GestureDetector =
        GestureDetector(
            view.context,
            this,
            Handler(view.context.mainLooper ?: Looper.getMainLooper())
        )

    private var isRightClickActive = false

    override var customActionsListener: CustomActionsListener? = null

    var enableMouseLongPressForDrag = false
    var enableCursorDrivenWorkflows = true

    override fun onDelegateTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && shouldIgnoreTouchDown(event)) {
            return false
        }

        if (!enableCursorDrivenWorkflows || customActionsListener == null) {
            return defaultTouchHandler(event)
        }

        if (isRightClickActive) {
            val action = event.actionMasked
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                isRightClickActive = false
            }
            return true
        }

        if (shouldStartMouseDrag(event) && !enableMouseLongPressForDrag) {
            performActions(ACTION_START_DRAG)
            return true
        }

        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onDown(event: MotionEvent): Boolean {
        downX = event.x
        downY = event.y
        if (TouchUtil.isMouseRightClickDownOrMove(event)) {
            isRightClickActive = true
            val cancelEvent = MotionEvent.obtain(event).apply { this.action = MotionEvent.ACTION_CANCEL }
            gestureDetector.onTouchEvent(cancelEvent)
            cancelEvent.recycle()
            performActions(ACTION_POPUP_MENU)
        }
        return true
    }

    override fun onLongPress(event: MotionEvent) {
        if (!isMouseEvent(event)) {
            performActions(ACTION_POPUP_MENU or ACTION_START_DRAG)
        } else if (enableMouseLongPressForDrag) {
            performActions(ACTION_START_DRAG)
        }
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        performActions(ACTION_LAUNCH)
        return true
    }

    private fun performActions(actionMask: Int) {
        customActionsListener?.performActions(view, actionMask)
    }

    private fun shouldStartMouseDrag(event: MotionEvent): Boolean {
        if (!isMouseEvent(event) ||
            event.actionMasked != MotionEvent.ACTION_MOVE ||
            TouchUtil.isMouseRightClickDownOrMove(event) ||
            isTrackpadMotionEvent(event)
        ) {
            return false
        }
        val touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop
        return abs(event.x - downX) > touchSlop || abs(event.y - downY) > touchSlop
    }

    private fun isMouseEvent(event: MotionEvent): Boolean {
        return event.isFromSource(InputDevice.SOURCE_MOUSE)
    }
}
