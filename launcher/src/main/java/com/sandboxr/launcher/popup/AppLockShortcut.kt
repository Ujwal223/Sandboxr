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

package com.sandboxr.launcher.popup

import android.app.PendingIntent
import android.util.Log
import android.view.View
import com.sandboxr.launcher.R
import com.android.launcher3.logging.StatsLogManager.LauncherEvent
import com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_SYSTEM_SHORTCUT_DISABLE_APP_LOCK_TAP
import com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_SYSTEM_SHORTCUT_ENABLE_APP_LOCK_TAP
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.util.Executors
import com.sandboxr.launcher.views.ActivityContext
import java.util.concurrent.CompletableFuture

/**
 * Represents the App Lock system shortcut for a given app.
 */
sealed class AppLockShortcut<T : ActivityContext>(
    iconId: Int,
    titleId: Int,
    target: T,
    itemInfo: ItemInfo,
    originalView: View,
    private val newAppLockEnabled: Boolean,
) : SystemShortcut<T>(iconId, titleId, target, itemInfo, originalView) {

    private val packageName: String? = mItemInfo.targetComponent?.packageName
    private val appLockPendingIntentFuture: CompletableFuture<PendingIntent?> =
        getAppLockPendingIntentFuture()

    private fun getAppLockPendingIntentFuture(): CompletableFuture<PendingIntent?> =
        if (packageName == null) {
            Log.w(TAG, "Package name is null")
            CompletableFuture.completedFuture(null)
        } else {
            CompletableFuture.supplyAsync(
                {
                    try {
                        val pm = mTarget.asContext().packageManager
                        val method = pm.javaClass.getMethod(
                            "getEnableAppLockIntentForPackage",
                            String::class.java,
                            Boolean::class.javaPrimitiveType
                        )
                        method.invoke(pm, packageName, newAppLockEnabled) as? PendingIntent
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to get App Lock intent for $packageName", e)
                        null
                    }
                },
                Executors.ORDERED_BG_EXECUTOR,
            )
        }

    override fun onClick(view: View) {
        mTarget.getStatsLogManager().logger().withItemInfo(mItemInfo).log(tapEvent)

        packageName ?: return
        appLockPendingIntentFuture
            .thenAcceptAsync(
                { pendingIntent ->
                    handleAppLockPendingIntentOnUi(pendingIntent, packageName, view)
                },
                mTarget.getUiExecutor(),
            )
            .exceptionally { ex ->
                Log.e(TAG, "Failed to get App Lock intent future for $packageName", ex)
                null
            }
    }

    private fun handleAppLockPendingIntentOnUi(
        pendingIntent: PendingIntent?,
        packageName: String,
        view: View,
    ) {
        if (pendingIntent == null) {
            Log.w(TAG, "Unable to get App Lock intent for $packageName")
            dismissTaskMenuView()
            return
        }

        val onEndCallback = mTarget.sendPendingIntentWithAnimation(view, pendingIntent, mItemInfo)
        if (onEndCallback == null) {
            dismissTaskMenuView()
        } else {
            onEndCallback.add(::dismissTaskMenuView)
        }
    }

    protected abstract val tapEvent: LauncherEvent

    private class EnableAppLockShortcut<T : ActivityContext>(
        target: T,
        itemInfo: ItemInfo,
        originalView: View,
    ) :
        AppLockShortcut<T>(
            R.drawable.ic_enable_app_lock_button,
            R.string.enable_app_lock,
            target,
            itemInfo,
            originalView,
            newAppLockEnabled = true,
        ) {
        override val tapEvent: LauncherEvent = LAUNCHER_SYSTEM_SHORTCUT_ENABLE_APP_LOCK_TAP
    }

    private class DisableAppLockShortcut<T : ActivityContext>(
        target: T,
        itemInfo: ItemInfo,
        originalView: View,
    ) :
        AppLockShortcut<T>(
            R.drawable.ic_disable_app_lock_button,
            R.string.disable_app_lock,
            target,
            itemInfo,
            originalView,
            newAppLockEnabled = false,
        ) {
        override val tapEvent: LauncherEvent = LAUNCHER_SYSTEM_SHORTCUT_DISABLE_APP_LOCK_TAP
    }

    companion object {
        private const val TAG = "AppLockShortcut"

        @JvmStatic
        fun <T : ActivityContext> newInstance(
            target: T,
            itemInfo: ItemInfo,
            originalView: View,
            isAppLockEnabled: Boolean,
        ): AppLockShortcut<T> {
            return if (isAppLockEnabled) {
                DisableAppLockShortcut(target, itemInfo, originalView)
            } else {
                EnableAppLockShortcut(target, itemInfo, originalView)
            }
        }
    }
}
