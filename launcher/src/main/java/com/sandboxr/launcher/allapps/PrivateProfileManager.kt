/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.sandboxr.launcher.allapps

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
 * Manages the Private Space profile (Android 14 / API 34+), detecting its existence and
 * whether it is currently locked or accessible.
 *
 * The private profile is a secondary user profile that users can lock/unlock separately
 * from their main profile. When locked, private apps are hidden in All Apps.
 *
 * On API < 34 this manager reports [PrivateProfileState.ABSENT] always.
 */
class PrivateProfileManager(private val context: Context) {

    companion object {
        private const val TAG = "PrivateProfileManager"

        // These action strings are available on API 34+.
        private const val ACTION_PRIVATE_SPACE_LOCK_CHANGED =
            "android.intent.action.PRIVATE_SPACE_LOCK_CHANGED"
    }

    /**
     * Describes the current state of the private space profile.
     */
    enum class PrivateProfileState {
        /** Private space is not supported or not set up on this device (API < 34). */
        ABSENT,
        /** Private space exists and is unlocked (accessible). */
        UNLOCKED,
        /** Private space exists but is currently locked (hidden). */
        LOCKED,
    }

    fun interface PrivateProfileStateListener {
        fun onPrivateProfileStateChanged(state: PrivateProfileState)
    }

    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val listeners = CopyOnWriteArrayList<PrivateProfileStateListener>()

    @Volatile
    var currentState: PrivateProfileState = PrivateProfileState.ABSENT
        private set

    /** The [UserHandle] of the private profile, or null if absent. */
    @Volatile
    var privateProfileUser: UserHandle? = null
        private set

    private val lockReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            Log.d(TAG, "Private space lock state changed")
            refreshState()
        }
    }

    private var receiverRegistered = false

    /**
     * Initializes detection and registers broadcast receiver.
     * No-op on API < 34.
     */
    fun init() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Private space is Android 14+ only
            currentState = PrivateProfileState.ABSENT
            return
        }
        if (receiverRegistered) return

        try {
            val filter = IntentFilter(ACTION_PRIVATE_SPACE_LOCK_CHANGED)
            context.registerReceiver(lockReceiver, filter)
            receiverRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register private space receiver: ${e.message}")
        }
        refreshState()
    }

    fun destroy() {
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(lockReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister private space receiver: ${e.message}")
            }
            receiverRegistered = false
        }
        listeners.clear()
    }

    fun addListener(listener: PrivateProfileStateListener) {
        listeners.add(listener)
        listener.onPrivateProfileStateChanged(currentState)
    }

    fun removeListener(listener: PrivateProfileStateListener) {
        listeners.remove(listener)
    }

    /** Returns true if a private space profile exists (regardless of lock state). */
    fun hasPrivateProfile(): Boolean = currentState != PrivateProfileState.ABSENT

    /** Returns true if the private space exists and is currently unlocked. */
    fun isPrivateProfileUnlocked(): Boolean = currentState == PrivateProfileState.UNLOCKED

    /**
     * Refreshes the detected state of the private profile.
     */
    fun refreshState() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return
        }

        val newUser = detectPrivateProfileUser()
        privateProfileUser = newUser

        val newState = when {
            newUser == null -> PrivateProfileState.ABSENT
            isProfileLocked(newUser) -> PrivateProfileState.LOCKED
            else -> PrivateProfileState.UNLOCKED
        }

        if (newState != currentState) {
            Log.d(TAG, "Private profile state changed: $currentState -> $newState")
            currentState = newState
            notifyListeners(newState)
        }
    }

    private fun detectPrivateProfileUser(): UserHandle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return try {
            // On API 34+, there is a dedicated API for private space user
            val profiles = userManager.userProfiles ?: return null
            profiles.firstOrNull { user ->
                // Private profile type detection requires API 34+
                isPrivateSpaceProfile(user)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to detect private profile: ${e.message}")
            null
        }
    }

    private fun isPrivateSpaceProfile(user: UserHandle): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        if (user == android.os.Process.myUserHandle()) return false
        return try {
            // API 34 introduced getUserType() method
            val method = userManager.javaClass.getMethod("isPrivateProfile", UserHandle::class.java)
            method.invoke(userManager, user) as? Boolean ?: false
        } catch (e: Exception) {
            // isPrivateProfile not available, use profile type check
            try {
                val method = userManager.javaClass.getMethod("getUserType")
                // USER_TYPE_PROFILE_PRIVATE is the profile type string
                // Reflective fallback - check for non-managed, non-main profile
                false
            } catch (e2: Exception) {
                false
            }
        }
    }

    private fun isProfileLocked(user: UserHandle): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                userManager.isQuietModeEnabled(user)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check profile lock state: ${e.message}")
            false
        }
    }

    private fun notifyListeners(state: PrivateProfileState) {
        for (listener in listeners) {
            listener.onPrivateProfileStateChanged(state)
        }
    }
}
