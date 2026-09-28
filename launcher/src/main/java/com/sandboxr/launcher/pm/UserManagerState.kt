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

package com.sandboxr.launcher.pm

import android.os.Process
import android.os.UserHandle
import com.android.users.UserType
import com.sandboxr.launcher.util.UserIconInfo

/**
 * Snapshot of UserManager user profile state.
 */
class UserManagerState(private val userMap: Map<UserHandle, UserCache.CachedUserInfo>) {

    private val userSerialMap = userMap.mapKeys { it.value.iconInfo.userSerial }

    fun isUserQuiet(serialNo: Long): Boolean = userSerialMap[serialNo]?.isQuietModeEnabled ?: false

    fun isUserQuiet(user: UserHandle): Boolean = userMap[user]?.isQuietModeEnabled ?: false

    fun getUser(serialNo: Long): UserHandle =
        userSerialMap[serialNo]?.iconInfo?.user ?: Process.myUserHandle()

    fun isUserUnlocked(user: UserHandle): Boolean = userMap[user]?.isUnlocked ?: true

    val isAnyProfileQuietModeEnabled: Boolean
        get() = userMap.any { it.value.isQuietModeEnabled }

    fun getUserInfo(user: UserHandle): UserIconInfo = getCachedInfo(user).iconInfo

    val userProfiles: List<UserHandle>
        get() = userMap.keys.toList()

    fun getAllCachedInfos(): Collection<UserCache.CachedUserInfo> = userMap.values

    fun getPreInstallApps(user: UserHandle): Set<String> =
        userMap[user]?.preInstallApps ?: emptySet()

    fun getCachedInfo(user: UserHandle): UserCache.CachedUserInfo =
        userMap[user]
            ?: UserCache.CachedUserInfo(
                iconInfo = UserIconInfo(user, UserType.MAIN),
                isUnlocked = true,
                isQuietModeEnabled = false,
            )

    fun getCachedInfoOrNull(user: UserHandle): UserCache.CachedUserInfo? = userMap[user]
}
