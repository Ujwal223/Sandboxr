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

package com.sandboxr.launcher.compat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import com.sandboxr.launcher.pm.UserCache
import javax.inject.Inject

/**
 * Cross-version compatibility layer for [LauncherApps] providing robust multi-profile
 * discovery (Personal, Work Profile, Private Space, and Sandboxr environments).
 */
@LauncherAppSingleton
open class LauncherAppsCompat @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userCache: UserCache,
) {
    private val launcherApps: LauncherApps? = context.getSystemService(LauncherApps::class.java)
    private val userManager: UserManager? = context.getSystemService(UserManager::class.java)

    open fun getActivityList(packageName: String?, user: UserHandle): List<LauncherActivityInfo> {
        return try {
            launcherApps?.getActivityList(packageName, user) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    open fun resolveActivity(intent: Intent, user: UserHandle): LauncherActivityInfo? {
        return try {
            launcherApps?.resolveActivity(intent, user)
        } catch (e: Exception) {
            null
        }
    }

    open fun startMainActivity(
        component: ComponentName,
        user: UserHandle,
        sourceBounds: Rect? = null,
        opts: Bundle? = null
    ) {
        try {
            launcherApps?.startMainActivity(component, user, sourceBounds, opts)
        } catch (e: Exception) {
            // Fallback to standard context intent dispatch
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setComponent(component)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                sourceBounds?.let { this.sourceBounds = it }
            }
            context.startActivity(intent, opts)
        }
    }

    open fun isPackageEnabledForProfile(packageName: String, user: UserHandle): Boolean {
        return try {
            launcherApps?.isPackageEnabled(packageName, user) ?: false
        } catch (e: Exception) {
            false
        }
    }

    open fun isActivityEnabledForProfile(component: ComponentName, user: UserHandle): Boolean {
        return try {
            launcherApps?.isActivityEnabled(component, user) ?: false
        } catch (e: Exception) {
            false
        }
    }

    open fun isWorkProfile(user: UserHandle): Boolean {
        if (user == Process.myUserHandle()) return false
        val info = userCache.getUserInfo(user)
        return info.type == com.android.users.UserType.WORK
    }

    open fun isPrivateSpace(user: UserHandle): Boolean {
        if (user == Process.myUserHandle()) return false
        val info = userCache.getUserInfo(user)
        return info.type == com.android.users.UserType.PRIVATE
    }

    open fun isProfileQuiet(user: UserHandle): Boolean {
        return try {
            userManager?.isQuietModeEnabled(user) ?: false
        } catch (e: Exception) {
            false
        }
    }

    open fun isProfileLocked(user: UserHandle): Boolean {
        return try {
            !(userManager?.isUserUnlocked(user) ?: true)
        } catch (e: Exception) {
            false
        }
    }
}
