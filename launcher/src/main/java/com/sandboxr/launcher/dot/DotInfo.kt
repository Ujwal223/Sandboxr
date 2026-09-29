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

package com.sandboxr.launcher.dot

import com.sandboxr.launcher.notification.NotificationKeyData
import kotlin.math.min

/**
 * Encapsulates notification state and counts for an icon's notification badge dot.
 */
open class DotInfo {

    companion object {
        const val MAX_COUNT = 999
    }

    private val mNotificationKeys = ArrayList<NotificationKeyData>()
    private var mTotalCount: Int = 0

    open fun addOrUpdateNotificationKey(notificationKey: NotificationKeyData): Boolean {
        val indexOfPrev = mNotificationKeys.indexOf(notificationKey)
        if (indexOfPrev != -1) {
            val prevKey = mNotificationKeys[indexOfPrev]
            if (prevKey.count == notificationKey.count) {
                return false
            }
            mTotalCount -= prevKey.count
            mTotalCount += notificationKey.count
            prevKey.count = notificationKey.count
            return true
        }
        val added = mNotificationKeys.add(notificationKey)
        if (added) {
            mTotalCount += notificationKey.count
        }
        return added
    }

    open fun removeNotificationKey(notificationKey: NotificationKeyData): Boolean {
        val index = mNotificationKeys.indexOf(notificationKey)
        if (index != -1) {
            val removed = mNotificationKeys.removeAt(index)
            mTotalCount -= removed.count
            return true
        }
        return false
    }

    open fun getNotificationKeys(): List<NotificationKeyData> = ArrayList(mNotificationKeys)

    open fun getNotificationCount(): Int = min(mTotalCount, MAX_COUNT)

    open fun hasDot(): Boolean = mNotificationKeys.isNotEmpty()
}
