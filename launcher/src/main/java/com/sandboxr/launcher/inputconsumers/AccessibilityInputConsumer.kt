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
import com.sandboxr.launcher.inputconsumers.InputConsumer.Companion.TYPE_ACCESSIBILITY

/**
 * Input consumer that intercepts multi-finger or swipe-up accessibility gestures from the bottom of the screen.
 */
class AccessibilityInputConsumer(
    val context: Context,
    delegate: InputConsumer,
    override val displayId: Int = 0,
    var onAccessibilityGesture: (() -> Unit)? = null
) : DelegateInputConsumer(displayId, delegate) {

    override val type: Int = TYPE_ACCESSIBILITY

    var isAccessibilityTriggered: Boolean = false
        private set

    override fun onMotionEvent(ev: MotionEvent) {
        if (ev.pointerCount >= 2 && ev.actionMasked == MotionEvent.ACTION_MOVE) {
            isAccessibilityTriggered = true
            onAccessibilityGesture?.invoke()
            return
        }
        super.onMotionEvent(ev)
    }
}
