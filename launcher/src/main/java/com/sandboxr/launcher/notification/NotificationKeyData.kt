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
import android.service.notification.StatusBarNotification

/**
 * Key data representing a system notification mapped to an icon dot.
 */
data class NotificationKeyData(
    val notificationKey: String,
    val shortcutId: String? = null,
    var count: Int = 1,
    val personKeys: Array<String> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NotificationKeyData) return false
        return notificationKey == other.notificationKey
    }

    override fun hashCode(): Int = notificationKey.hashCode()

    companion object {
        @JvmStatic
        fun fromNotification(sbn: StatusBarNotification): NotificationKeyData {
            val notif = sbn.notification
            val count = if (notif.number > 0) notif.number else 1

            val personKeysList = mutableListOf<String>()
            try {
                val peopleList = notif.extras?.getParcelableArrayList<android.app.Person>(Notification.EXTRA_PEOPLE_LIST)
                if (!peopleList.isNullOrEmpty()) {
                    for (person in peopleList) {
                        person.key?.let { personKeysList.add(it) }
                    }
                }
            } catch (_: Throwable) {
                // Ignore any reflection or deserialization issues
            }

            try {
                val peopleArray = notif.extras?.getStringArray(Notification.EXTRA_PEOPLE)
                if (peopleArray != null) {
                    for (p in peopleArray) {
                        personKeysList.add(p)
                    }
                }
            } catch (_: Throwable) {
                // Ignore
            }

            val sortedPersonKeys = personKeysList.distinct().sorted().toTypedArray()

            return NotificationKeyData(
                notificationKey = sbn.key,
                shortcutId = notif.shortcutId,
                count = count,
                personKeys = sortedPersonKeys
            )
        }
    }
}
