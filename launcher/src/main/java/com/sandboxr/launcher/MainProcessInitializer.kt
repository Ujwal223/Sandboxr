/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.sandboxr.launcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.launcher3.BuildConfig
import com.android.launcher3.Flags
import com.sandboxr.launcher.config.FeatureFlags
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import com.sandboxr.launcher.graphics.BitmapCreationCheck
import com.sandboxr.launcher.logging.FileLog
import javax.inject.Inject

/**
 * Utility class to handle one-time initializations of the Launcher main process.
 */
@LauncherAppSingleton
open class MainProcessInitializer @Inject constructor() {

    companion object {
        private const val TAG = "MainProcessInitializer"
        private const val DEBUG_STRICT_MODE = false
        const val NOTIFICATION_CHANNEL_ID = "com.sandboxr.launcher.Debug"
        const val NOTIFICATION_CHANNEL_NAME = "Debug"
        const val NOTIFICATION_TAG = "Debug"
        const val NOTIFICATION_ID = 0
    }

    @Volatile
    private var isInitialized = false

    @VisibleForTesting
    fun isInitialized(): Boolean = isInitialized

    open fun init(context: Context) {
        if (isInitialized) {
            return
        }
        synchronized(this) {
            if (isInitialized) return
            isInitialized = true
        }

        val appContext = context.applicationContext ?: context

        // 1. Initialize file logger directory
        try {
            val filesDir = appContext.filesDir
            if (filesDir != null) {
                FileLog.setDir(filesDir)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to initialize FileLog directory: ${t.message}")
        }

        // 2. Track bitmap creations during draw pass if enabled
        if (BitmapCreationCheck.ENABLED) {
            BitmapCreationCheck.startTracking(appContext)
        }

        // 3. Configure StrictMode in debug / studio builds if enabled
        if (DEBUG_STRICT_MODE || (BuildConfig.IS_STUDIO_BUILD && Flags.enableStrictMode())) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )
        }

        // 4. In debug builds on debug devices, notify on uncaught crash if feature flag is set
        if (BuildConfig.IS_DEBUG_DEVICE && FeatureFlags.NOTIFY_CRASHES.get()) {
            setupCrashNotification(appContext)
        }
    }

    private fun setupCrashNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    val stackTrace = Log.getStackTraceString(throwable)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, stackTrace)
                    }
                    val chooser = Intent.createChooser(shareIntent, null).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        chooser,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
                    } else {
                        @Suppress("DEPRECATION")
                        Notification.Builder(context)
                    }

                    val notification = builder
                        .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
                        .setContentTitle("Launcher crash detected!")
                        .setStyle(Notification.BigTextStyle().bigText(stackTrace))
                        .addAction(android.R.drawable.ic_menu_share, "Share", pendingIntent)
                        .setAutoCancel(true)
                        .build()

                    notificationManager?.notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to post crash notification: ${e.message}", e)
                } finally {
                    defaultHandler?.uncaughtException(thread, throwable)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to setup crash notification handler: ${e.message}")
        }
    }
}
