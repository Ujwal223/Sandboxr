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

package com.sandboxr.launcher.sysuiconnection

import android.content.Context
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * State representing System UI and gesture navigation connection status.
 */
data class SysUIConnectionState(
    val isConnected: Boolean = false,
    val windowToken: IBinder? = null,
    val systemUiFlags: Long = 0L,
    val activeDisplayId: Int = 0
)

/**
 * Tracks and manages the IPC connection between Android SystemUI and Sandboxr QuickStep.
 */
class SysUIConnectionTracker private constructor() {

    companion object {
        @Volatile
        private var instance: SysUIConnectionTracker? = null

        fun get(): SysUIConnectionTracker {
            return instance ?: synchronized(this) {
                instance ?: SysUIConnectionTracker().also { instance = it }
            }
        }
    }

    private val _connectionState = MutableStateFlow(SysUIConnectionState())
    val connectionState: StateFlow<SysUIConnectionState> = _connectionState.asStateFlow()

    private val listeners = CopyOnWriteArrayList<(SysUIConnectionState) -> Unit>()

    fun addListener(listener: (SysUIConnectionState) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (SysUIConnectionState) -> Unit) {
        listeners.remove(listener)
    }

    fun onConnected(token: IBinder?, flags: Long, displayId: Int = 0) {
        val newState = SysUIConnectionState(
            isConnected = true,
            windowToken = token,
            systemUiFlags = flags,
            activeDisplayId = displayId
        )
        _connectionState.value = newState
        listeners.forEach { it.invoke(newState) }
    }

    fun onDisconnected() {
        val newState = SysUIConnectionState(isConnected = false)
        _connectionState.value = newState
        listeners.forEach { it.invoke(newState) }
    }
}
