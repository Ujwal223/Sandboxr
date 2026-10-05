/*
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

package com.sandboxr.launcher.virtual

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.ui.AppItem
import com.sandboxr.virtual.client.stub.StubActivity
import com.sandboxr.virtual.server.am.VActivityManagerService

/**
 * Metadata record representing a guest application installed inside a SANDBOXR
 * container environment.
 *
 * Encapsulates the environment identity, package metadata, display label, badged icon,
 * and execution intent for seamless integration into the ported GrapheneOS Launcher3.
 */
data class VirtualAppInfo(
    val packageName: String,
    val mainActivity: String,
    val label: String,
    val envId: String,
    val envName: String,
    val envColor: Long,
    val iconBitmap: Bitmap? = null,
    val versionName: String = "1.0",
    val versionCode: Long = 1L,
    val isSystemClone: Boolean = false,
    val installTime: Long = System.currentTimeMillis()
) {

    /**
     * Unique key identifying this app instance across multi-environments.
     * E.g. "com.instagram.android@env-work-uuid"
     */
    val compositeKey: String
        get() = "$packageName@$envId"

    /**
     * Converts this virtual app into a canonical AOSP/GrapheneOS [AppInfo] model object
     * used by LauncherModel, AllAppsList, and CellLayout.
     */
    fun toLauncherAppInfo(context: Context): AppInfo {
        val component = ComponentName(packageName, mainActivity)
        val targetIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = component
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        }

        // The intent executed by the host launcher redirects to StubActivity container
        val launchIntent = Intent(context, StubActivity::class.java).apply {
            putExtra(VActivityManagerService.EXTRA_TARGET_INTENT, targetIntent)
            putExtra(VActivityManagerService.EXTRA_ENV_ID, envId)
            putExtra(VActivityManagerService.EXTRA_TARGET_PKG, packageName)
            putExtra(VActivityManagerService.EXTRA_TARGET_ACTIVITY, mainActivity)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        }

        val appInfo = AppInfo(context, component, null)
        appInfo.title = label
        appInfo.intent = launchIntent
        if (iconBitmap != null) {
            appInfo.bitmap = com.sandboxr.launcher.icons.BitmapInfo.fromBitmap(iconBitmap)
        }
        return appInfo
    }

    /**
     * Converts this virtual app into an [AppItem] model object used by the Jetpack Compose
     * Liquid Glass launcher workspace.
     */
    fun toAppItem(): AppItem {
        return AppItem(
            packageName = packageName,
            label = label,
            iconBitmap = iconBitmap?.asImageBitmap(),
            envId = envId,
            envColor = Color(envColor),
            isSystemApp = false,
            notificationBadgeCount = 0,
            userHandle = null
        )
    }
}
