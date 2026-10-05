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

import android.view.MotionEvent
import com.sandboxr.launcher.inputconsumers.InputConsumer.Companion.TYPE_RESET_GESTURE

/**
 * Consumer used to safely reset gesture pipelines and consume lingering touch events.
 */
class ResetGestureInputConsumer(
    override val displayId: Int = 0,
    var onReset: (() -> Unit)? = null
) : InputConsumer {

    override val type: Int = TYPE_RESET_GESTURE

    override fun onMotionEvent(ev: MotionEvent) {
        if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) {
            onReset?.invoke()
        }
    }
}
