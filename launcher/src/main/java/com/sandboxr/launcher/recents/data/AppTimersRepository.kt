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

package com.sandboxr.launcher.recents.data

import android.app.KeyguardManager
import android.os.UserHandle
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.inject.Inject
import javax.inject.Singleton

interface AppTimersRepository {
    fun getRemainingDuration(packageName: String, userHandle: UserHandle): Duration?
    fun invalidateCache()
}

@Singleton
class AppTimersRepositoryImpl @Inject constructor() : AppTimersRepository {
    private val mCache: ConcurrentMap<String, Duration> = ConcurrentHashMap()

    override fun getRemainingDuration(packageName: String, userHandle: UserHandle): Duration? {
        val key = "$packageName:${userHandle.hashCode()}"
        return mCache[key]
    }

    override fun invalidateCache() {
        mCache.clear()
    }

    fun setTimer(packageName: String, userHandle: UserHandle, duration: Duration) {
        val key = "$packageName:${userHandle.hashCode()}"
        mCache[key] = duration
    }
}

interface UserLockedStateRepository {
    fun invalidateCachedValues()
    fun getIsUserLocked(userId: Int): Boolean
}

@Singleton
class UserLockedRepository @Inject constructor(
    private val keyguardManager: KeyguardManager? = null
) : UserLockedStateRepository {
    private val cache: ConcurrentMap<Int, Boolean> = ConcurrentHashMap()

    override fun invalidateCachedValues() {
        cache.clear()
    }

    override fun getIsUserLocked(userId: Int): Boolean {
        return cache.getOrPut(userId) {
            keyguardManager?.isDeviceLocked ?: false
        }
    }

    fun setUserLocked(userId: Int, locked: Boolean) {
        cache[userId] = locked
    }
}
