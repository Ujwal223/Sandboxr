/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.sandboxr.launcher.taskbar

import android.view.KeyEvent

/**
 * Controller handling navigation button tap and long-press actions in 3-button mode.
 */
class TaskbarNavButtonController(
    val activity: TaskbarActivityContext,
) {
    companion object {
        const val BUTTON_BACK = 1
        const val BUTTON_HOME = 2
        const val BUTTON_RECENTS = 3
        const val BUTTON_IME_SWITCH = 4
        const val BUTTON_A11Y = 5
    }

    private lateinit var controllers: TaskbarControllers

    var onBackClickListener: (() -> Unit)? = null
    var onHomeClickListener: (() -> Unit)? = null
    var onRecentsClickListener: (() -> Unit)? = null
    var onImeSwitchClickListener: (() -> Unit)? = null
    var onA11yClickListener: (() -> Unit)? = null

    var onBackLongClickListener: (() -> Boolean)? = null
    var onHomeLongClickListener: (() -> Boolean)? = null
    var onRecentsLongClickListener: (() -> Boolean)? = null

    fun init(controllers: TaskbarControllers) {
        this.controllers = controllers
    }

    fun onButtonClick(buttonType: Int) {
        when (buttonType) {
            BUTTON_BACK -> onBackClickListener?.invoke()
            BUTTON_HOME -> onHomeClickListener?.invoke()
            BUTTON_RECENTS -> onRecentsClickListener?.invoke()
            BUTTON_IME_SWITCH -> onImeSwitchClickListener?.invoke()
            BUTTON_A11Y -> onA11yClickListener?.invoke()
        }
    }

    fun onButtonLongClick(buttonType: Int): Boolean {
        return when (buttonType) {
            BUTTON_BACK -> onBackLongClickListener?.invoke() ?: false
            BUTTON_HOME -> onHomeLongClickListener?.invoke() ?: false
            BUTTON_RECENTS -> onRecentsLongClickListener?.invoke() ?: false
            else -> false
        }
    }
}
