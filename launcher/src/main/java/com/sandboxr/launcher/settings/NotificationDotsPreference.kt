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

package com.sandboxr.launcher.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.AttributeSet
import androidx.core.app.NotificationManagerCompat
import androidx.preference.Preference
import com.sandboxr.launcher.R
import com.sandboxr.launcher.notification.NotificationListener

/**
 * Preference for toggling and checking Notification Dots listener permission.
 */
class NotificationDotsPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    init {
        title = context.getString(R.string.notification_dots_title)
        updateSummary()
    }

    override fun onAttached() {
        super.onAttached()
        updateSummary()
    }

    fun isNotificationListenerGranted(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    fun isBadgingEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "notification_badging", 1) == 1
        } catch (_: Throwable) {
            true
        }
    }

    fun updateSummary() {
        summary = if (!isNotificationListenerGranted()) {
            context.getString(R.string.notification_dots_desc_permission_needed)
        } else if (isBadgingEnabled()) {
            context.getString(R.string.notification_dots_desc_on)
        } else {
            context.getString(R.string.notification_dots_desc_off)
        }
    }

    fun handlePreferenceClick() {
        val intent = if (!isNotificationListenerGranted()) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, NotificationListener::class.java).flattenToString()
                )
            }
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Throwable) {
            }
        }
    }

    override fun onClick() {
        super.onClick()
        handlePreferenceClick()
    }
}
