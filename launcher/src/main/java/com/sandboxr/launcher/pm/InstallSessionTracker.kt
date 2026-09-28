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
import android.content.pm.PackageInstaller
import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Tracks PackageInstaller session events to provide real-time app installation progress
 * on the home screen and app drawer.
 */
class InstallSessionTracker(
    context: Context,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {
    private val packageInstaller: PackageInstaller? = try {
        context.packageManager.packageInstaller
    } catch (e: Exception) {
        null
    }

    private val listeners = CopyOnWriteArrayList<Callback>()
    private var isRegistered = false

    interface Callback {
        fun onPackageStateChanged(installInfo: PackageInstallInfo)
        fun onSessionFinished(sessionId: Int, success: Boolean)
    }

    private val sessionCallback = object : PackageInstaller.SessionCallback() {
        override fun onCreated(sessionId: Int) {
            notifyProgress(sessionId)
        }

        override fun onBadgingChanged(sessionId: Int) {
            notifyProgress(sessionId)
        }

        override fun onActiveChanged(sessionId: Int, active: Boolean) {}

        override fun onProgressChanged(sessionId: Int, progress: Float) {
            notifyProgress(sessionId)
        }

        override fun onFinished(sessionId: Int, success: Boolean) {
            listeners.forEach { it.onSessionFinished(sessionId, success) }
        }

        private fun notifyProgress(sessionId: Int) {
            val sessionInfo = packageInstaller?.getSessionInfo(sessionId) ?: return
            val installInfo = PackageInstallInfo.fromInstallingState(sessionInfo)
            listeners.forEach { it.onPackageStateChanged(installInfo) }
        }
    }

    fun register() {
        if (!isRegistered && packageInstaller != null) {
            try {
                packageInstaller.registerSessionCallback(sessionCallback, handler)
                isRegistered = true
            } catch (ignored: Exception) {}
        }
    }

    fun unregister() {
        if (isRegistered && packageInstaller != null) {
            try {
                packageInstaller.unregisterSessionCallback(sessionCallback)
            } catch (ignored: Exception) {}
            isRegistered = false
        }
    }

    fun addCallback(callback: Callback) {
        listeners.add(callback)
    }

    fun removeCallback(callback: Callback) {
        listeners.remove(callback)
    }
}
