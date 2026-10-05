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

import android.view.KeyEvent
import android.view.MotionEvent

/**
 * Base input consumer that delegates operations to an inner delegate [InputConsumer].
 */
abstract class DelegateInputConsumer(
    override val displayId: Int,
    protected val delegate: InputConsumer
) : InputConsumer {

    override fun allowInterceptByParent(): Boolean = delegate.allowInterceptByParent()

    override fun isConsumerDetachedFromGesture(): Boolean = delegate.isConsumerDetachedFromGesture()

    override fun getActiveConsumerInHierarchy(): InputConsumer = delegate.getActiveConsumerInHierarchy()

    override fun onConsumerAboutToBeSwitched() {
        delegate.onConsumerAboutToBeSwitched()
    }

    override fun onMotionEvent(ev: MotionEvent) {
        delegate.onMotionEvent(ev)
    }

    override fun onHoverEvent(ev: MotionEvent) {
        delegate.onHoverEvent(ev)
    }

    override fun onKeyEvent(ev: KeyEvent) {
        delegate.onKeyEvent(ev)
    }
}
