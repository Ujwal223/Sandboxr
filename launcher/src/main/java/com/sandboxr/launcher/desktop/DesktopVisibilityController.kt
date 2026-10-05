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

package com.sandboxr.launcher.desktop

import android.content.Context
import android.util.Log
import android.util.SparseArray
import android.util.SparseBooleanArray
import androidx.annotation.AnyThread
import java.io.PrintWriter
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Controls the visibility of the workspace and desktop mode state on foldables, tablets,
 * and external displays.
 */
class DesktopVisibilityController(
    val context: Context
) {

    /**
     * Tracks the desks configuration on a display.
     */
    data class DisplayDeskConfig(
        val displayId: Int,
        var activeDeskId: Int = INACTIVE_DESK_ID,
        val deskIds: MutableSet<Int> = mutableSetOf()
    )

    private val displaysDesksConfigsMap = SparseArray<DisplayDeskConfig>()
    private val inOverviewStateMap = SparseBooleanArray()
    private val desktopVisibilityListeners = CopyOnWriteArraySet<DesktopVisibilityListener>()

    var canCreateDesks: Boolean = false
        private set

    var launcherAnimationRunning: Boolean = false

    /**
     * Listener for desktop mode visibility and desk lifecycle events.
     */
    interface DesktopVisibilityListener {
        fun onDeskAdded(displayId: Int, deskId: Int) {}
        fun onDeskRemoved(displayId: Int, deskId: Int) {}
        fun onActiveDeskChanged(displayId: Int, newActiveDesk: Int, oldActiveDesk: Int) {}
        fun onTaskAppearingInDeskWithOverviewShowing(taskId: Int, displayId: Int, deskId: Int) {}
        fun onTaskbarCornerRoundingUpdate(doesAnyTaskRequireTaskbarRounding: Boolean, displayId: Int) {}
        fun onDesktopModeChanged(displayId: Int, inDesktopMode: Boolean) {}
    }

    fun registerDesktopVisibilityListener(listener: DesktopVisibilityListener) {
        desktopVisibilityListeners.add(listener)
    }

    fun unregisterDesktopVisibilityListener(listener: DesktopVisibilityListener) {
        desktopVisibilityListeners.remove(listener)
    }

    @AnyThread
    fun getActiveDeskId(displayId: Int): Int {
        return displaysDesksConfigsMap[displayId]?.activeDeskId ?: INACTIVE_DESK_ID
    }

    @AnyThread
    fun isInDesktopMode(displayId: Int): Boolean {
        val activeDeskId = getActiveDeskId(displayId)
        return activeDeskId != INACTIVE_DESK_ID
    }

    fun isInDesktopModeAndNotInOverview(displayId: Int): Boolean {
        val inOverview = inOverviewStateMap.get(displayId, false)
        return isInDesktopMode(displayId) && !inOverview
    }

    fun setOverviewStateEnabled(displayId: Int, enabled: Boolean) {
        val prev = inOverviewStateMap.get(displayId, false)
        if (prev != enabled) {
            inOverviewStateMap.put(displayId, enabled)
            Log.d(TAG, "Overview state changed for display $displayId: enabled=$enabled")
        }
    }

    fun addDesk(displayId: Int, deskId: Int) {
        var config = displaysDesksConfigsMap[displayId]
        if (config == null) {
            config = DisplayDeskConfig(displayId, INACTIVE_DESK_ID, mutableSetOf(deskId))
            displaysDesksConfigsMap.put(displayId, config)
        } else {
            config.deskIds.add(deskId)
        }
        for (listener in desktopVisibilityListeners) {
            listener.onDeskAdded(displayId, deskId)
        }
    }

    fun removeDesk(displayId: Int, deskId: Int) {
        val config = displaysDesksConfigsMap[displayId] ?: return
        config.deskIds.remove(deskId)
        val wasActive = config.activeDeskId == deskId
        if (wasActive) {
            config.activeDeskId = INACTIVE_DESK_ID
            for (listener in desktopVisibilityListeners) {
                listener.onActiveDeskChanged(displayId, INACTIVE_DESK_ID, deskId)
                listener.onDesktopModeChanged(displayId, false)
            }
        }
        for (listener in desktopVisibilityListeners) {
            listener.onDeskRemoved(displayId, deskId)
        }
    }

    fun setActiveDesk(displayId: Int, newDeskId: Int) {
        var config = displaysDesksConfigsMap[displayId]
        if (config == null) {
            config = DisplayDeskConfig(displayId, newDeskId, mutableSetOf(newDeskId))
            displaysDesksConfigsMap.put(displayId, config)
        }
        val oldDeskId = config.activeDeskId
        if (oldDeskId != newDeskId) {
            config.activeDeskId = newDeskId
            val enteredDesktop = newDeskId != INACTIVE_DESK_ID
            for (listener in desktopVisibilityListeners) {
                listener.onActiveDeskChanged(displayId, newDeskId, oldDeskId)
                listener.onDesktopModeChanged(displayId, enteredDesktop)
            }
        }
    }

    fun notifyTaskbarCornerRounding(displayId: Int, doesAnyTaskRequireRounding: Boolean) {
        for (listener in desktopVisibilityListeners) {
            listener.onTaskbarCornerRoundingUpdate(doesAnyTaskRequireRounding, displayId)
        }
    }

    fun setCanCreateDesks(canCreate: Boolean) {
        this.canCreateDesks = canCreate
    }

    fun dumpLogs(prefix: String, pw: PrintWriter) {
        pw.println("$prefix DesktopVisibilityController:")
        pw.println("$prefix   listenersCount=${desktopVisibilityListeners.size}")
        pw.println("$prefix   displaysCount=${displaysDesksConfigsMap.size()}")
        for (i in 0 until displaysDesksConfigsMap.size()) {
            val key = displaysDesksConfigsMap.keyAt(i)
            val config = displaysDesksConfigsMap.valueAt(i)
            pw.println("$prefix   display[$key]: activeDesk=${config.activeDeskId}, desks=${config.deskIds}")
        }
    }

    companion object {
        private const val TAG = "DesktopVisController"
        const val INACTIVE_DESK_ID = -1
    }
}
