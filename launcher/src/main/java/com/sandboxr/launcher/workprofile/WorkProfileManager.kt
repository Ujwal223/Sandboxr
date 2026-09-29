/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.sandboxr.launcher.workprofile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages the work profile state (existence and quiet mode / paused status)
 * and notifies observers when the profile's availability changes.
 *
 * Work profile detection:
 * - Queries [UserManager] for managed (work) profiles via [UserCache].
 * - Listens to [Intent.ACTION_MANAGED_PROFILE_AVAILABLE], [Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE],
 *   [Intent.ACTION_MANAGED_PROFILE_ADDED], and [Intent.ACTION_MANAGED_PROFILE_REMOVED].
 *
 * Usage:
 * ```kotlin
 * val manager = WorkProfileManager.INSTANCE.get(context)
 * manager.addListener(myListener)
 * ```
 */
class WorkProfileManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "WorkProfileManager"

        @Volatile
        private var instance: WorkProfileManager? = null

        fun get(context: Context): WorkProfileManager {
            return instance ?: synchronized(this) {
                instance ?: WorkProfileManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Describes the current state of the work profile.
     */
    enum class WorkProfileState {
        /** No managed work profile exists on this device for the current user. */
        ABSENT,
        /** Work profile exists and is enabled/available. */
        ENABLED,
        /** Work profile exists but is in quiet mode (paused). */
        DISABLED,
    }

    /** Listener for work profile state changes. */
    fun interface WorkProfileStateListener {
        fun onWorkProfileStateChanged(state: WorkProfileState)
    }

    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val listeners = CopyOnWriteArrayList<WorkProfileStateListener>()

    @Volatile
    var currentState: WorkProfileState = WorkProfileState.ABSENT
        private set

    /** The [UserHandle] of the managed work profile, or null if absent. */
    @Volatile
    var workProfileUser: UserHandle? = null
        private set

    private val profileReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Received profile broadcast: ${intent.action}")
            refreshState()
        }
    }

    private var receiverRegistered = false

    /**
     * Registers the broadcast receiver and performs an initial state refresh.
     * Should be called on the main thread during launcher initialization.
     */
    fun init() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
        }
        context.registerReceiver(profileReceiver, filter)
        receiverRegistered = true
        refreshState()
    }

    /**
     * Unregisters the broadcast receiver. Call when the owning component is destroyed.
     */
    fun destroy() {
        if (receiverRegistered) {
            context.unregisterReceiver(profileReceiver)
            receiverRegistered = false
        }
        listeners.clear()
    }

    fun addListener(listener: WorkProfileStateListener) {
        listeners.add(listener)
        // Immediately notify new listener of current state
        listener.onWorkProfileStateChanged(currentState)
    }

    fun removeListener(listener: WorkProfileStateListener) {
        listeners.remove(listener)
    }

    /** Returns true if a managed work profile exists and is currently enabled. */
    fun isWorkProfileEnabled(): Boolean = currentState == WorkProfileState.ENABLED

    /** Returns true if a managed work profile exists (enabled or disabled). */
    fun hasWorkProfile(): Boolean = currentState != WorkProfileState.ABSENT

    /**
     * Requests to enable or disable quiet mode on the work profile.
     *
     * @param enable true = turn on the work profile, false = pause it.
     * @return true if the request was made successfully, false if no work profile.
     */
    fun setWorkProfileEnabled(enable: Boolean): Boolean {
        val user = workProfileUser ?: return false
        return try {
            userManager.requestQuietModeEnabled(!enable, user)
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to toggle work profile quiet mode: ${e.message}")
            false
        }
    }

    /**
     * Refreshes [currentState] by querying [UserManager] and dispatches callbacks.
     * Safe to call from any thread.
     */
    fun refreshState() {
        val profiles = getUserProfiles()
        val workUser = profiles.firstOrNull { isWorkProfile(it) }
        workProfileUser = workUser

        val newState = when {
            workUser == null -> WorkProfileState.ABSENT
            isQuietModeEnabled(workUser) -> WorkProfileState.DISABLED
            else -> WorkProfileState.ENABLED
        }

        if (newState != currentState) {
            Log.d(TAG, "Work profile state changed: $currentState -> $newState")
            currentState = newState
            notifyListeners(newState)
        }
    }

    private fun getUserProfiles(): List<UserHandle> {
        return try {
            userManager.userProfiles ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get user profiles: ${e.message}")
            emptyList()
        }
    }

    private fun isWorkProfile(user: UserHandle): Boolean {
        if (user == android.os.Process.myUserHandle()) return false
        return try {
            // UserManager.isManagedProfile(int userId) is a @SystemApi, use reflection
            // UserHandle.getIdentifier() is public SDK since API 24
            val userId: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                user.hashCode() // hashCode() returns mHandle which is the userId
            } else {
                user.hashCode()
            }
            try {
                // Try the int-based version (API 30+ system API)
                val method = userManager.javaClass.getMethod("isManagedProfile", Int::class.java)
                method.invoke(userManager, userId) as? Boolean ?: false
            } catch (e: NoSuchMethodException) {
                // Fallback: try the no-arg version (API 23+, checks current user — imprecise)
                // We can still detect work profile if quiet mode is enabled on a secondary user
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    userManager.isQuietModeEnabled(user)
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check managed profile: ${e.message}")
            false
        }
    }

    private fun isQuietModeEnabled(user: UserHandle): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                userManager.isQuietModeEnabled(user)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check quiet mode: ${e.message}")
            false
        }
    }

    private fun notifyListeners(state: WorkProfileState) {
        for (listener in listeners) {
            listener.onWorkProfileStateChanged(state)
        }
    }
}
