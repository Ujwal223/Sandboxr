/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.sandboxr.launcher.notification

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationManagerCompat
import com.sandboxr.launcher.dot.DotInfo
import com.sandboxr.launcher.util.PackageUserKey
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Service that listens to system notifications and translates them into icon badge dots.
 */
open class NotificationListener : NotificationListenerService() {

    interface NotificationsChangedListener {
        fun onNotificationPosted(packageUserKey: PackageUserKey, notificationKey: NotificationKeyData) {}
        fun onNotificationRemoved(packageUserKey: PackageUserKey, notificationKey: NotificationKeyData) {}
        fun onNotificationFullRefresh(packageUserToDotInfos: Map<PackageUserKey, DotInfo>) {}
    }

    private val mWorkerHandler = Handler(Looper.getMainLooper())

    override fun onListenerConnected() {
        super.onListenerConnected()
        sNotificationListener = this
        sIsConnected = true
        onNotificationFullRefreshInternal()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        sIsConnected = false
        if (sNotificationListener === this) {
            sNotificationListener = null
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || shouldBeFiltered(sbn)) {
            return
        }
        val keyData = NotificationKeyData.fromNotification(sbn)
        val packageUserKey = PackageUserKey(sbn.packageName, sbn.user)

        mWorkerHandler.post {
            val repo = NotificationRepository.getInstance()
            val currentMap = HashMap(repo.packageUserToDotInfos)
            val dotInfo = currentMap.getOrPut(packageUserKey) { DotInfo() }
            val changed = dotInfo.addOrUpdateNotificationKey(keyData)
            if (changed) {
                repo.dispatchUpdate(currentMap) { it == packageUserKey }
                for (listener in sListeners) {
                    listener.onNotificationPosted(packageUserKey, keyData)
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val keyData = NotificationKeyData.fromNotification(sbn)
        val packageUserKey = PackageUserKey(sbn.packageName, sbn.user)

        mWorkerHandler.post {
            val repo = NotificationRepository.getInstance()
            val currentMap = HashMap(repo.packageUserToDotInfos)
            val dotInfo = currentMap[packageUserKey]
            if (dotInfo != null) {
                val changed = dotInfo.removeNotificationKey(keyData)
                if (changed) {
                    if (!dotInfo.hasDot()) {
                        currentMap.remove(packageUserKey)
                    }
                    repo.dispatchUpdate(currentMap) { it == packageUserKey }
                    for (listener in sListeners) {
                        listener.onNotificationRemoved(packageUserKey, keyData)
                    }
                }
            }
        }
    }

    open fun onNotificationFullRefreshInternal() {
        val activeNotifs = try {
            activeNotifications ?: emptyArray()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to get active notifications", e)
            emptyArray()
        }

        mWorkerHandler.post {
            val tempMap = HashMap<PackageUserKey, DotInfo>()
            for (sbn in activeNotifs) {
                if (shouldBeFiltered(sbn)) continue
                val keyData = NotificationKeyData.fromNotification(sbn)
                val packageUserKey = PackageUserKey(sbn.packageName, sbn.user)
                val dotInfo = tempMap.getOrPut(packageUserKey) { DotInfo() }
                dotInfo.addOrUpdateNotificationKey(keyData)
            }
            val repo = NotificationRepository.getInstance()
            repo.dispatchUpdate(tempMap) { true }
            for (listener in sListeners) {
                listener.onNotificationFullRefresh(tempMap)
            }
        }
    }

    companion object {
        private const val TAG = "NotificationListener"
        private val sListeners = CopyOnWriteArrayList<NotificationsChangedListener>()

        @Volatile
        private var sNotificationListener: NotificationListener? = null

        @Volatile
        private var sIsConnected: Boolean = false

        @JvmStatic
        fun isConnected(): Boolean = sIsConnected && sNotificationListener != null

        @JvmStatic
        fun getInstanceIfConnected(): NotificationListener? =
            if (sIsConnected) sNotificationListener else null

        @JvmStatic
        fun addNotificationsChangedListener(listener: NotificationsChangedListener) {
            sListeners.add(listener)
            val repo = NotificationRepository.getInstance()
            if (repo.packageUserToDotInfos.isNotEmpty()) {
                listener.onNotificationFullRefresh(repo.packageUserToDotInfos)
            }
        }

        @JvmStatic
        fun removeNotificationsChangedListener(listener: NotificationsChangedListener) {
            sListeners.remove(listener)
        }

        fun requestRebind(componentName: ComponentName) {
            try {
                NotificationListenerService.requestRebind(componentName)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to requestRebind", e)
            }
        }

        @JvmStatic
        fun cancelNotificationIfConnected(key: String) {
            sNotificationListener?.let {
                try {
                    it.cancelNotification(key)
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to cancel notification: $key", e)
                }
            }
        }

        @JvmStatic
        fun shouldBeFiltered(sbn: StatusBarNotification): Boolean {
            val notif = sbn.notification ?: return true
            // Ongoing notifications shouldn't show dots on launcher icons
            if ((notif.flags and Notification.FLAG_ONGOING_EVENT) != 0) {
                return true
            }
            return false
        }

        @JvmStatic
        fun isNotificationListenerAccessGranted(context: Context): Boolean {
            return NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        }

        @VisibleForTesting
        @JvmStatic
        fun setInstanceForTesting(listener: NotificationListener?, connected: Boolean = true) {
            sNotificationListener = listener
            sIsConnected = connected
        }

        @VisibleForTesting
        @JvmStatic
        fun clearListenersForTesting() {
            sListeners.clear()
        }
    }
}
