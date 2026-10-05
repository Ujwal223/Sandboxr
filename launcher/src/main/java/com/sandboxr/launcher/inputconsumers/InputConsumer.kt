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

import android.view.Display
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent

/**
 * Interface representing a consumer of touch and input events during gesture navigation.
 */
interface InputConsumer {

    val type: Int
    val displayId: Int
        get() = Display.DEFAULT_DISPLAY

    fun allowInterceptByParent(): Boolean = true

    fun isConsumerDetachedFromGesture(): Boolean = false

    fun getActiveConsumerInHierarchy(): InputConsumer = this

    fun onConsumerAboutToBeSwitched() {}

    fun onMotionEvent(ev: MotionEvent) {}

    fun onHoverEvent(ev: MotionEvent) {}

    fun onKeyEvent(ev: KeyEvent) {}

    fun onInputEvent(ev: InputEvent) {
        when (ev) {
            is MotionEvent -> onMotionEvent(ev)
            is KeyEvent -> onKeyEvent(ev)
        }
    }

    val name: String
        get() {
            val sb = StringBuilder()
            for (i in NAMES.indices) {
                if ((type and (1 shl i)) != 0) {
                    if (sb.isNotEmpty()) sb.append(":")
                    sb.append(NAMES[i])
                }
            }
            return if (sb.isEmpty()) "TYPE_NO_OP" else sb.toString()
        }

    fun <T : InputConsumer> getInputConsumerOfClass(clazz: Class<T>): T? {
        return if (clazz.isInstance(this)) clazz.cast(this) else null
    }

    companion object {
        const val TYPE_NO_OP = 1 shl 0
        const val TYPE_LAUNCHER = 1 shl 1
        const val TYPE_OTHER_ACTIVITY = 1 shl 2
        const val TYPE_ASSISTANT = 1 shl 3
        const val TYPE_DEVICE_LOCKED = 1 shl 4
        const val TYPE_ACCESSIBILITY = 1 shl 5
        const val TYPE_SCREEN_PINNED = 1 shl 6
        const val TYPE_OVERVIEW = 1 shl 7
        const val TYPE_RESET_GESTURE = 1 shl 8
        const val TYPE_PROGRESS_DELEGATE = 1 shl 9
        const val TYPE_SYSUI_OVERLAY = 1 shl 10
        const val TYPE_ONE_HANDED = 1 shl 11
        const val TYPE_TASKBAR_STASH = 1 shl 12
        const val TYPE_STATUS_BAR = 1 shl 13
        const val TYPE_CURSOR_HOVER = 1 shl 14
        const val TYPE_NAV_HANDLE_LONG_PRESS = 1 shl 15
        const val TYPE_BUBBLE_BAR = 1 shl 16

        val NAMES = arrayOf(
            "TYPE_NO_OP",
            "TYPE_LAUNCHER",
            "TYPE_OTHER_ACTIVITY",
            "TYPE_ASSISTANT",
            "TYPE_DEVICE_LOCKED",
            "TYPE_ACCESSIBILITY",
            "TYPE_SCREEN_PINNED",
            "TYPE_OVERVIEW",
            "TYPE_RESET_GESTURE",
            "TYPE_PROGRESS_DELEGATE",
            "TYPE_SYSUI_OVERLAY",
            "TYPE_ONE_HANDED",
            "TYPE_TASKBAR_STASH",
            "TYPE_STATUS_BAR",
            "TYPE_CURSOR_HOVER",
            "TYPE_NAV_HANDLE_LONG_PRESS",
            "TYPE_BUBBLE_BAR"
        )

        val NO_OP: InputConsumer = createNoOpInputConsumer(Display.DEFAULT_DISPLAY)

        fun createNoOpInputConsumer(displayId: Int): InputConsumer {
            return object : InputConsumer {
                override val type: Int = TYPE_NO_OP
                override val displayId: Int = displayId
            }
        }
    }
}
