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

package com.sandboxr.launcher.model.repository

import android.app.admin.DevicePolicyManager.ACTION_DEVICE_POLICY_RESOURCE_UPDATED
import android.content.Context
import androidx.annotation.WorkerThread
import com.android.launcher3.concurrent.annotations.LightweightBackground
import com.android.launcher3.concurrent.annotations.LightweightBackgroundPriority
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import com.sandboxr.launcher.model.StringCache
import com.sandboxr.launcher.pm.UserCache
import com.sandboxr.launcher.util.DaggerSingletonTracker
import com.sandboxr.launcher.util.LooperExecutor
import com.sandboxr.launcher.util.MutableListenableRef
import com.sandboxr.launcher.util.SimpleBroadcastReceiver
import com.sandboxr.launcher.util.SimpleBroadcastReceiver.Companion.actionsFilter
import javax.inject.Inject

/** Repository for [StringCache] */
@LauncherAppSingleton
class StringCacheRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    userCache: UserCache,
    @LightweightBackground(LightweightBackgroundPriority.UI) executor: LooperExecutor,
    lifeCycle: DaggerSingletonTracker,
) {

    private val _stringCache = MutableListenableRef(StringCache.EMPTY)
    /** Cache for strings used in launcher */
    val stringCache = _stringCache.asListenable()

    init {
        // User is added or removed
        lifeCycle.addCloseable(
            userCache.userChanges.forEach(executor) {
                if (it.newUser == null || it.oldUser == null) reloadCache()
            }
        )

        // Device profile policy changes
        val dpUpdateReceiver = SimpleBroadcastReceiver(context, executor) { reloadCache() }
        dpUpdateReceiver.register(actionsFilter(ACTION_DEVICE_POLICY_RESOURCE_UPDATED)) {
            // Cache will reload after the intent is registered
            reloadCache()
        }
        lifeCycle.addCloseable(dpUpdateReceiver)
    }

    @WorkerThread
    private fun reloadCache() = _stringCache.dispatchValue(StringCache.fromContext(context))

    companion object {
        @JvmStatic
        fun getStringCache(ctx: Context) = StringCache.fromContext(ctx)
    }
}
