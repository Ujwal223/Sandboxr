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

package com.sandboxr.launcher.pm

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.android.users.UserType
import com.sandboxr.launcher.compat.LauncherAppsCompat
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import javax.inject.Inject

/**
 * High-level Package Manager wrapper coordinating multi-profile app loading.
 * Strictly separates Personal apps, Work Profile apps (into dedicated work tab),
 * and Private Space apps (isolated and hidden when locked).
 */
@LauncherAppSingleton
class LauncherAppsWrapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userCache: UserCache,
    private val compat: LauncherAppsCompat,
) {
    private val userManager = context.getSystemService(UserManager::class.java)

    fun getPersonalApps(): List<LauncherActivityInfo> {
        val myUser = Process.myUserHandle()
        return compat.getActivityList(null, myUser)
    }

    fun getWorkProfileApps(): List<LauncherActivityInfo> {
        val workUser = getWorkProfileUser() ?: return emptyList()
        if (isWorkProfileQuiet()) return emptyList()
        return compat.getActivityList(null, workUser)
    }

    fun getPrivateSpaceApps(): List<LauncherActivityInfo> {
        val privateUser = getPrivateSpaceUser() ?: return emptyList()
        if (isPrivateSpaceLocked()) return emptyList()
        return compat.getActivityList(null, privateUser)
    }

    fun getWorkProfileUser(): UserHandle? {
        val profiles = userCache.userProfiles
        return profiles.firstOrNull {
            it != Process.myUserHandle() && userCache.getUserInfo(it).type == UserType.WORK
        }
    }

    fun getPrivateSpaceUser(): UserHandle? {
        val profiles = userCache.userProfiles
        return profiles.firstOrNull {
            it != Process.myUserHandle() && userCache.getUserInfo(it).type == UserType.PRIVATE
        }
    }

    fun hasWorkProfile(): Boolean = getWorkProfileUser() != null

    fun hasPrivateSpace(): Boolean = getPrivateSpaceUser() != null

    fun isWorkProfileQuiet(): Boolean {
        val workUser = getWorkProfileUser() ?: return false
        return compat.isProfileQuiet(workUser)
    }

    fun isPrivateSpaceLocked(): Boolean {
        val privateUser = getPrivateSpaceUser() ?: return false
        return compat.isProfileLocked(privateUser)
    }

    fun setWorkProfileQuietMode(quiet: Boolean): Boolean {
        val workUser = getWorkProfileUser() ?: return false
        return try {
            userManager?.requestQuietModeEnabled(quiet, workUser) ?: false
        } catch (e: Exception) {
            false
        }
    }
}
