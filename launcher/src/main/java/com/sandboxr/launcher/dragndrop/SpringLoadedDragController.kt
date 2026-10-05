/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.sandboxr.launcher.dragndrop

import com.android.launcher3.Alarm
import com.android.launcher3.OnAlarmListener
import com.sandboxr.launcher.CellLayout
import com.sandboxr.launcher.Launcher

/**
 * Coordinates automatic page snapping when an item is held hovering over a workspace edge or mini-page.
 */
class SpringLoadedDragController(private val launcher: Launcher) : OnAlarmListener {
    internal val alarm = Alarm().also { it.setOnAlarmListener(this) }

    private var screen: CellLayout? = null

    fun cancel() = alarm.cancelAlarm()

    fun setAlarm(cl: CellLayout?) {
        cancel()
        alarm.setAlarm(
            if (cl == null) ENTER_SPRING_LOAD_CANCEL_HOVER_TIME else ENTER_SPRING_LOAD_HOVER_TIME
        )
        screen = cl
    }

    override fun onAlarm(alarm: Alarm) {
        val targetScreen = screen
        if (targetScreen != null) {
            val workspace = launcher.getWorkspace()
            if (workspace != null) {
                val pageIndex = workspace.indexOfChild(targetScreen)
                if (pageIndex >= 0 && pageIndex != workspace.getCurrentPage()) {
                    workspace.snapToPage(pageIndex)
                }
            }
        } else {
            launcher.getDragController()?.cancelDrag()
        }
    }

    companion object {
        private const val ENTER_SPRING_LOAD_HOVER_TIME: Long = 500L
        private const val ENTER_SPRING_LOAD_CANCEL_HOVER_TIME: Long = 950L
    }
}
