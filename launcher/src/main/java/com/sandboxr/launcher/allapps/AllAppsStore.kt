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

package com.sandboxr.launcher.allapps

import android.os.UserHandle
import android.util.Log
import android.view.ViewGroup
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.util.ComponentKey
import com.sandboxr.launcher.util.PackageUserKey
import java.util.Arrays
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A reactive store maintaining the in-memory collection of all installed applications.
 * Notifies registered UI listeners whenever the model changes.
 */
class AllAppsStore {

    fun interface OnUpdateListener {
        fun onAppsUpdated()
    }

    private var apps: Array<AppInfo> = AppInfo.EMPTY_ARRAY
    private val updateListeners = CopyOnWriteArrayList<OnUpdateListener>()
    private val iconContainers = ArrayList<ViewGroup>()
    private var packageUserKeyToUidMap: Map<PackageUserKey, Int> = Collections.emptyMap()
    private var modelFlags: Int = 0
    private var deferUpdatesFlags: Int = 0
    private var updatePending: Boolean = false

    fun getApps(): Array<AppInfo> = apps

    /**
     * Sets the current set of applications and maps package+user key to UID.
     * Note: apps should be sorted by COMPONENT_KEY_COMPARATOR for binary search.
     */
    fun setApps(
        newApps: Array<AppInfo>?,
        flags: Int = 0,
        map: Map<PackageUserKey, Int> = emptyMap()
    ) {
        apps = newApps ?: AppInfo.EMPTY_ARRAY
        modelFlags = flags
        packageUserKeyToUidMap = map
        notifyUpdate()
    }

    fun lookUpForUid(packageName: String, user: UserHandle): Int {
        return packageUserKeyToUidMap[PackageUserKey(packageName, user)] ?: -1
    }

    fun hasModelFlag(mask: Int): Boolean {
        return (modelFlags and mask) != 0
    }

    /**
     * Returns [AppInfo] matching the provided [ComponentKey], or null if not found.
     */
    fun getApp(key: ComponentKey): AppInfo? {
        val tempInfo = AppInfo().apply {
            componentName = key.componentName
            user = key.user
        }
        val index = Arrays.binarySearch(apps, tempInfo, AppInfo.COMPONENT_KEY_COMPARATOR)
        return if (index >= 0) apps[index] else null
    }

    fun addUpdateListener(listener: OnUpdateListener) {
        updateListeners.add(listener)
    }

    fun removeUpdateListener(listener: OnUpdateListener) {
        updateListeners.remove(listener)
    }

    fun registerIconContainer(container: ViewGroup) {
        if (!iconContainers.contains(container)) {
            iconContainers.add(container)
        }
    }

    fun unregisterIconContainer(container: ViewGroup) {
        iconContainers.remove(container)
    }

    fun deferUpdates(flag: Int) {
        deferUpdatesFlags = deferUpdatesFlags or flag
    }

    fun disableDeferUpdates(flag: Int) {
        deferUpdatesFlags = deferUpdatesFlags and flag.inv()
        if (deferUpdatesFlags == 0 && updatePending) {
            notifyUpdate()
            updatePending = false
        }
    }

    fun notifyUpdate() {
        if (deferUpdatesFlags != 0) {
            updatePending = true
            return
        }
        for (listener in updateListeners) {
            listener.onAppsUpdated()
        }
    }

    /**
     * Updates notification dot badges across all registered icon containers.
     */
    fun updateNotificationDots(updatedKeyPredicate: (PackageUserKey) -> Boolean) {
        for (container in iconContainers) {
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is BubbleTextView && child.tag is com.sandboxr.launcher.model.data.ItemInfoWithIcon) {
                    val info = child.tag as com.sandboxr.launcher.model.data.ItemInfoWithIcon
                    val key = PackageUserKey(info.targetComponent?.packageName, info.user)
                    if (updatedKeyPredicate(key)) {
                        child.applyDotState(info, true)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "AllAppsStore"
        const val DEFER_UPDATES_NEXT_DRAW = 1 shl 0
        const val DEFER_UPDATES_TEST = 1 shl 1
    }
}
