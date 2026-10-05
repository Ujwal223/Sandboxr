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
import android.graphics.Point
import android.view.MotionEvent
import kotlin.math.abs

/**
 * Handles edge swipe gestures (from left or right edge inwards) to trigger back navigation.
 */
class EdgeBackGestureHandler(
    val context: Context,
    val displaySize: Point = Point(1080, 2400)
) {

    var edgeWidthPx: Float = 48f
    var swipeThresholdPx: Float = 80f

    var onBackInvoked: (() -> Unit)? = null

    private var downX: Float = 0f
    private var downY: Float = 0f
    private var isLeftEdge: Boolean = false
    private var isRightEdge: Boolean = false
    private var isGestureActive: Boolean = false

    fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                isLeftEdge = ev.rawX <= edgeWidthPx
                isRightEdge = ev.rawX >= displaySize.x - edgeWidthPx
                isGestureActive = isLeftEdge || isRightEdge
                return isGestureActive
            }

            MotionEvent.ACTION_MOVE -> {
                if (isGestureActive) {
                    val dx = if (isLeftEdge) ev.rawX - downX else downX - ev.rawX
                    val dy = abs(ev.rawY - downY)
                    // If vertical drag exceeds horizontal drag by 2x, cancel
                    if (dy > dx * 2f && dy > 40f) {
                        isGestureActive = false
                    }
                    return isGestureActive
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isGestureActive) {
                    val dx = if (isLeftEdge) ev.rawX - downX else downX - ev.rawX
                    if (dx >= swipeThresholdPx) {
                        onBackInvoked?.invoke()
                    }
                    isGestureActive = false
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                isGestureActive = false
            }
        }
        return false
    }
}
