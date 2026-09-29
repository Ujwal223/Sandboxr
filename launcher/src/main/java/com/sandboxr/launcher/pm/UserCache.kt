/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.sandboxr.launcher.pm

import android.content.Context
import android.content.Intent
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.annotation.WorkerThread
import com.android.users.UserType
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import com.sandboxr.launcher.util.DaggerSingletonTracker
import com.sandboxr.launcher.util.Executors
import com.sandboxr.launcher.util.LooperExecutor
import com.sandboxr.launcher.util.MutableListenableStream
import com.sandboxr.launcher.util.SimpleBroadcastReceiver
import com.sandboxr.launcher.util.SimpleBroadcastReceiver.Companion.actionsFilter
import com.sandboxr.launcher.util.UserIconInfo
import javax.inject.Inject

/**
 * Manages an in-process cache of user profiles and handles to minimize system binder IPC.
 */
@LauncherAppSingleton
class UserCache @Inject constructor(
    @ApplicationContext private val context: Context,
    tracker: DaggerSingletonTracker,
) {
    private val userManager = context.getSystemService(UserManager::class.java)
    private var closed = false
    private var _userInfoMap: UserManagerState? = null

    val userManagerState: UserManagerState
        get() = _userInfoMap ?: rebuildUserCache()

    private val _userChanges = MutableListenableStream<UserChangeEvent>()
    val userChanges = _userChanges.asListenable()

    init {
        val userChangeReceiver = SimpleBroadcastReceiver(
            context = context,
            executor = Executors.MODEL_EXECUTOR
        ) { onUsersChanged(it) }

        userChangeReceiver.register(
            actionsFilter(
                Intent.ACTION_MANAGED_PROFILE_ADDED,
                Intent.ACTION_MANAGED_PROFILE_AVAILABLE,
                Intent.ACTION_MANAGED_PROFILE_REMOVED,
                Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE,
                Intent.ACTION_MANAGED_PROFILE_UNLOCKED,
                Intent.ACTION_PROFILE_ACCESSIBLE,
                Intent.ACTION_PROFILE_ADDED,
                Intent.ACTION_PROFILE_AVAILABLE,
                Intent.ACTION_PROFILE_INACCESSIBLE,
                Intent.ACTION_PROFILE_REMOVED,
                Intent.ACTION_PROFILE_UNAVAILABLE,
            )
        ) {
            rebuildUserCache()
        }
        tracker.addCloseable { closed = true }
        tracker.addCloseable(userChangeReceiver)
    }

    @WorkerThread
    private fun onUsersChanged(intent: Intent) {
        if (closed) return
        val oldState = userManagerState
        rebuildUserCache()
        val user = intent.getParcelableExtra<UserHandle>(Intent.EXTRA_USER) ?: return
        val change = UserChangeEvent(
            oldState.getCachedInfoOrNull(user),
            userManagerState.getCachedInfoOrNull(user),
        )
        if (change.oldUser != change.newUser) {
            _userChanges.dispatchValue(change)
        }
    }

    @WorkerThread
    fun rebuildUserCache(): UserManagerState {
        val profiles = try {
            userManager?.userProfiles ?: listOf(Process.myUserHandle())
        } catch (e: Exception) {
            listOf(Process.myUserHandle())
        }

        val map = profiles.associateWith { buildCachedUserInfo(it) }
        val state = UserManagerState(map)
        _userInfoMap = state
        return state
    }

    private fun buildCachedUserInfo(user: UserHandle): CachedUserInfo {
        val isQuiet = try {
            userManager?.isQuietModeEnabled(user) ?: false
        } catch (e: Exception) {
            false
        }
        val isUnlocked = try {
            userManager?.isUserUnlocked(user) ?: true
        } catch (e: Exception) {
            true
        }
        val serial = try {
            userManager?.getSerialNumberForUser(user) ?: 0L
        } catch (e: Exception) {
            0L
        }

        val isMyUser = user == Process.myUserHandle()
        val type = if (isMyUser) UserType.MAIN else UserType.WORK

        return CachedUserInfo(
            iconInfo = UserIconInfo(user = user, type = type, userSerial = serial),
            isUnlocked = isUnlocked,
            isQuietModeEnabled = isQuiet,
        )
    }

    fun getSerialNumberForUser(user: UserHandle): Long = getUserInfo(user).userSerial

    fun getUserInfo(user: UserHandle): UserIconInfo = userManagerState.getUserInfo(user)

    fun isUserUnlocked(user: UserHandle): Boolean = userManagerState.isUserUnlocked(user)

    fun getUserForSerialNumber(serialNumber: Long): UserHandle =
        userManagerState.getUser(serialNumber)

    val userProfiles: List<UserHandle>
        get() = userManagerState.userProfiles

    fun getPreInstallApps(user: UserHandle): Set<String> = userManagerState.getPreInstallApps(user)

    data class CachedUserInfo(
        val iconInfo: UserIconInfo,
        val isUnlocked: Boolean,
        val isQuietModeEnabled: Boolean,
        val preInstallApps: Set<String> = emptySet(),
    )

    data class UserChangeEvent(val oldUser: CachedUserInfo?, val newUser: CachedUserInfo?)

    companion object {
        private const val TAG = "UserCache"

        @Volatile
        private var instance: UserCache? = null

        @JvmStatic
        fun getInstance(context: Context): UserCache {
            return instance ?: synchronized(this) {
                instance ?: UserCache(context.applicationContext, DaggerSingletonTracker()).also {
                    instance = it
                }
            }
        }

        @JvmStatic
        fun get(context: Context): UserCache = getInstance(context)

        @JvmField
        val INSTANCE = object {
            fun get(context: Context): UserCache = getInstance(context)
        }
    }
}
