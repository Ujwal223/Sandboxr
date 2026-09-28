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

package com.sandboxr.launcher.icons

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.UserHandle
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import com.sandboxr.launcher.util.PackageUserKey
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

/**
 * Tracks dynamic date, time, and timezone changes to notify icon updates for Clock and Calendar apps.
 */
@LauncherAppSingleton
class IconChangeTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val listeners = CopyOnWriteArrayList<(PackageUserKey) -> Unit>()
    private var isRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val user = android.os.Process.myUserHandle()
            when (intent?.action) {
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED -> {
                    notifyIconChanged("com.google.android.calendar", user)
                    notifyIconChanged("com.google.android.deskclock", user)
                    notifyIconChanged("com.android.calendar", user)
                    notifyIconChanged("com.android.deskclock", user)
                }
            }
        }
    }

    init {
        register()
    }

    fun register() {
        if (!isRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            context.registerReceiver(receiver, filter)
            isRegistered = true
        }
    }

    fun unregister() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (ignored: Exception) {}
            isRegistered = false
        }
    }

    fun addChangeListener(listener: (PackageUserKey) -> Unit) {
        listeners.add(listener)
    }

    fun removeChangeListener(listener: (PackageUserKey) -> Unit) {
        listeners.remove(listener)
    }

    fun notifyIconChanged(packageName: String, user: UserHandle) {
        val key = PackageUserKey(packageName, user)
        listeners.forEach { it(key) }
    }
}
