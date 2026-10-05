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

import android.app.AlertDialog
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.sandboxr.launcher.R
import com.sandboxr.launcher.notification.NotificationListener
import com.sandboxr.launcher.util.Executors.MAIN_EXECUTOR
import com.sandboxr.launcher.util.SafeCloseable
import com.sandboxr.launcher.util.SettingsCache
import com.sandboxr.launcher.util.SettingsCache.NOTIFICATION_BADGING_URI

/**
 * A [Preference] for displaying and managing the notification dots (badge) system setting.
 *
 * Observes the system `notification_badging` secure setting and the enabled notification
 * listeners list. Reflects the current combined state as a human-readable summary and,
 * when the listener permission is missing, shows a widget-frame warning icon alongside
 * a confirmation dialog guiding the user to grant access.
 */
class NotificationDotsPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    private var mWidgetFrameVisible = false
    private var mSettingCacheSafeCloseable: SafeCloseable? = null

    init {
        setTitle(R.string.notification_dots_title)
        updateSummary()
    }

    fun isNotificationListenerGranted(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            NOTIFICATION_ENABLED_LISTENERS
        ) ?: return false
        val myListener = ComponentName(context, NotificationListener::class.java)
        return enabledListeners.contains(myListener.flattenToString()) ||
            enabledListeners.contains(myListener.flattenToShortString())
    }

    fun isBadgingEnabled(): Boolean {
        return SettingsCache.getInstance(context).getValue(NOTIFICATION_BADGING_URI)
    }

    fun updateSummary() {
        onSettingsChanged(isBadgingEnabled())
    }

    fun handlePreferenceClick(): Boolean {
        return if (isNotificationListenerGranted()) {
            val targetIntent = intent
            if (targetIntent != null) {
                try {
                    context.startActivity(targetIntent)
                    true
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }
        } else {
            false
        }
    }

    /**
     * Observes changes to the enabled notification listeners list and re-evaluates
     * the combined dots state whenever the list changes.
     */
    private val mListenerListObserver = object : ContentObserver(MAIN_EXECUTOR.handler) {
        override fun onChange(selfChange: Boolean) {
            onSettingsChanged(
                SettingsCache.getInstance(context).getValue(NOTIFICATION_BADGING_URI)
            )
        }
    }

    override fun onAttached() {
        super.onAttached()
        // Start observing the badging URI; immediately fires with the current value.
        mSettingCacheSafeCloseable = SettingsCache.getInstance(context)
            .getListenableRef(NOTIFICATION_BADGING_URI)
            .forEach(MAIN_EXECUTOR, ::onSettingsChanged)

        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(NOTIFICATION_ENABLED_LISTENERS),
            false,
            mListenerListObserver
        )

        // Build the intent that opens the system notification settings with the
        // badging preference highlighted.
        val extras = Bundle().apply {
            putString(SettingsActivity.EXTRA_FRAGMENT_HIGHLIGHT_KEY, "notification_badging")
        }
        intent = Intent("android.settings.NOTIFICATION_SETTINGS")
            .putExtra(EXTRA_SHOW_FRAGMENT_ARGS, extras)
    }

    override fun onDetached() {
        super.onDetached()
        mSettingCacheSafeCloseable?.close()
        mSettingCacheSafeCloseable = null
        context.contentResolver.unregisterContentObserver(mListenerListObserver)
    }

    /**
     * Shows or hides the warning icon in the preference's widget frame.
     *
     * The widget frame is visible when badging is enabled but the notification
     * listener service has not been granted permission.
     */
    private fun setWidgetFrameVisible(isVisible: Boolean) {
        if (mWidgetFrameVisible != isVisible) {
            mWidgetFrameVisible = isVisible
            notifyChanged()
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val widgetFrame = holder.findViewById(android.R.id.widget_frame)
        widgetFrame?.visibility = if (mWidgetFrameVisible) View.VISIBLE else View.GONE
    }

    /**
     * Called whenever the system `notification_badging` setting changes.
     *
     * Updates the summary string and the warning-icon visibility based on the combined
     * state of the global badging toggle and the launcher's notification listener status.
     *
     * @param enabled `true` if the system notification dots toggle is on.
     */
    fun onSettingsChanged(enabled: Boolean) {
        var summaryResId = if (enabled) {
            R.string.notification_dots_desc_on
        } else {
            R.string.notification_dots_desc_off
        }

        var serviceEnabled = true
        if (enabled) {
            serviceEnabled = isNotificationListenerGranted()
            if (!serviceEnabled) {
                summaryResId = R.string.title_missing_notification_access
            }
        }

        setWidgetFrameVisible(!serviceEnabled)
        // Route clicks to the confirmation dialog only when the listener is missing.
        fragment = if (serviceEnabled) null else NotificationAccessConfirmation::class.java.name
        setSummary(summaryResId)
    }

    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        /** Hidden field `Settings.Secure.ENABLED_NOTIFICATION_LISTENERS`. */
        private const val NOTIFICATION_ENABLED_LISTENERS = "enabled_notification_listeners"
        private const val EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args"
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NotificationAccessConfirmation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dialog that informs the user that the launcher's notification listener permission
     * is missing and offers to navigate them to the system settings to grant it.
     */
    class NotificationAccessConfirmation :
        DialogFragment(), DialogInterface.OnClickListener {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val context: Context = requireActivity()
            val msg = context.getString(
                R.string.msg_missing_notification_access,
                context.getString(R.string.derived_app_name)
            )
            return AlertDialog.Builder(context)
                .setTitle(R.string.title_missing_notification_access)
                .setMessage(msg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.title_change_settings, this)
                .create()
        }

        override fun onClick(dialogInterface: DialogInterface, which: Int) {
            val cn = ComponentName(requireActivity(), NotificationListener::class.java)
            val showFragmentArgs = Bundle().apply {
                putString(SettingsActivity.EXTRA_FRAGMENT_HIGHLIGHT_KEY, cn.flattenToString())
            }
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(SettingsActivity.EXTRA_FRAGMENT_HIGHLIGHT_KEY, cn.flattenToString())
                .putExtra(":settings:show_fragment_args", showFragmentArgs)
            requireActivity().startActivity(intent)
        }
    }
}
