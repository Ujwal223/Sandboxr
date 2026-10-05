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

package com.sandboxr.launcher.display

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.util.Log
import com.sandboxr.launcher.util.SafeCloseable
import com.sandboxr.launcher.util.window.WindowManagerProxy
import java.io.PrintWriter
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

/**
 * Singleton that tracks live changes to the [LauncherDisplayInfo] and notifies registered
 * [DisplayInfoChangeListener]s when significant display-configuration changes occur.
 *
 * Changes tracked include:
 * - Density / font-scale changes ([LauncherDisplayInfo.CHANGE_DENSITY]).
 * - Navigation mode changes ([LauncherDisplayInfo.CHANGE_NAVIGATION_MODE]).
 * - Supported window bounds changes ([LauncherDisplayInfo.CHANGE_SUPPORTED_BOUNDS]).
 * - Night-mode changes ([LauncherDisplayInfo.CHANGE_NIGHT_MODE]).
 * - Rotation changes ([LauncherDisplayInfo.CHANGE_ROTATION]).
 *
 * @param context An application-level context.
 * @param wmProxy The [WindowManagerProxy] used to query display state.
 */
class DisplayController @Inject constructor(
    private val context: Context,
    private val wmProxy: WindowManagerProxy = WindowManagerProxy.INSTANCE,
) {

    // -----------------------------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------------------------

    @Volatile
    private var _info: LauncherDisplayInfo = LauncherDisplayInfo(context, wmProxy)

    /** The most recent snapshot of the display information. */
    val info: LauncherDisplayInfo get() = _info

    private val listeners = CopyOnWriteArrayList<DisplayInfoChangeListener>()

    // -----------------------------------------------------------------------------------------
    // System receivers
    // -----------------------------------------------------------------------------------------

    private val configReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            handleInfoChange()
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = handleInfoChange()
        override fun onDisplayRemoved(displayId: Int) = handleInfoChange()
        override fun onDisplayChanged(displayId: Int) = handleInfoChange()
    }

    /** Registers broadcast / display listeners. Call once, typically from [LauncherApplication]. */
    fun init() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_CONFIGURATION_CHANGED)
        }
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(configReceiver, filter)

        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        dm.registerDisplayListener(displayListener, null)
    }

    /** Unregisters all listeners. Call from [SafeCloseable] returned to the component. */
    fun destroy() {
        context.unregisterReceiver(configReceiver)
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        dm.unregisterDisplayListener(displayListener)
    }

    // -----------------------------------------------------------------------------------------
    // Change handling
    // -----------------------------------------------------------------------------------------

    private fun handleInfoChange() {
        val newInfo = LauncherDisplayInfo(context, wmProxy)
        val change = newInfo.diff(_info)
        if (change == 0) return

        _info = newInfo
        Log.d(TAG, "Display change: ${LauncherDisplayInfo.getChangeFlagsString(change)}")

        for (listener in listeners) {
            listener.onDisplayInfoChanged(newInfo, change)
        }
    }

    /**
     * Forces an immediate re-query of display state and dispatches to listeners if anything
     * changed. Useful after a configuration change that the system did not broadcast.
     */
    fun dispatchDisplayChanges() = handleInfoChange()

    // -----------------------------------------------------------------------------------------
    // Listener registration
    // -----------------------------------------------------------------------------------------

    /**
     * Registers a [DisplayInfoChangeListener] and immediately calls it with the current info.
     *
     * @return A [SafeCloseable] that removes the listener when closed.
     */
    fun addChangeListener(listener: DisplayInfoChangeListener): SafeCloseable {
        listeners.add(listener)
        listener.onDisplayInfoChanged(_info, LauncherDisplayInfo.CHANGE_ALL)
        return SafeCloseable { listeners.remove(listener) }
    }

    /** Removes a previously registered [DisplayInfoChangeListener]. */
    fun removeChangeListener(listener: DisplayInfoChangeListener) {
        listeners.remove(listener)
    }

    // -----------------------------------------------------------------------------------------
    // Debug
    // -----------------------------------------------------------------------------------------

    fun dump(pw: PrintWriter) {
        pw.println("DisplayController:")
        _info.dump(pw)
        pw.println("  listeners=${listeners.size}")
    }

    // -----------------------------------------------------------------------------------------
    // Interfaces
    // -----------------------------------------------------------------------------------------

    /** Callback interface for display-info changes. */
    fun interface DisplayInfoChangeListener {
        /**
         * Called when the active [LauncherDisplayInfo] changes.
         *
         * @param info   The new display info snapshot.
         * @param flags  Bitmask of [LauncherDisplayInfo.CHANGE_*] constants indicating what changed.
         */
        fun onDisplayInfoChanged(info: LauncherDisplayInfo, flags: Int)
    }


    companion object {
        private const val TAG = "DisplayController"

        @Volatile
        private var instance: DisplayController? = null

        @JvmStatic
        fun get(context: Context): DisplayController =
            instance ?: synchronized(this) {
                instance ?: DisplayController(context.applicationContext).also { instance = it }
            }

        @JvmStatic
        fun getInstance(context: Context): DisplayController = get(context)

        @JvmField
        val INSTANCE = object {
            operator fun get(context: Context): DisplayController = DisplayController.get(context)
        }
    }
}

/**
 * Returns the change-flags as a human-readable pipe-separated string for logging.
 */
fun LauncherDisplayInfo.Companion.getChangeFlagsString(change: Int): String {
    val flags = buildList {
        if (change and LauncherDisplayInfo.CHANGE_ACTIVE_SCREEN != 0) add("CHANGE_ACTIVE_SCREEN")
        if (change and LauncherDisplayInfo.CHANGE_ROTATION != 0) add("CHANGE_ROTATION")
        if (change and LauncherDisplayInfo.CHANGE_DENSITY != 0) add("CHANGE_DENSITY")
        if (change and LauncherDisplayInfo.CHANGE_SUPPORTED_BOUNDS != 0) add("CHANGE_SUPPORTED_BOUNDS")
        if (change and LauncherDisplayInfo.CHANGE_NAVIGATION_MODE != 0) add("CHANGE_NAVIGATION_MODE")
        if (change and LauncherDisplayInfo.CHANGE_SHOW_DESKTOP_FIRST_TASKBAR != 0) add("CHANGE_SHOW_DESKTOP_FIRST_TASKBAR")
        if (change and LauncherDisplayInfo.CHANGE_NIGHT_MODE != 0) add("CHANGE_NIGHT_MODE")
    }
    return flags.joinToString("|")
}
