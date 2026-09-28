/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.sandboxr.launcher.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.UserHandle

/**
 * Base provider responsible for loading drawables for installed applications,
 * activities, and shortcuts across users and profiles.
 */
open class IconProvider(protected val context: Context? = null) {

    open fun getIcon(packageName: String): Drawable? {
        if (context == null) return null
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    open fun getIcon(packageName: String, iconDpi: Int): Drawable? {
        if (context == null) return null
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getResourcesForApplication(appInfo).getDrawableForDensity(appInfo.icon, iconDpi, null)
        } catch (e: Exception) {
            getIcon(packageName)
        }
    }

    open fun getIcon(activityInfo: LauncherActivityInfo, iconDpi: Int): Drawable? {
        return try {
            activityInfo.getIcon(iconDpi)
        } catch (e: Exception) {
            null
        }
    }

    open fun getIcon(componentName: ComponentName, user: UserHandle): Drawable? {
        if (context == null) return null
        return try {
            val pm = context.packageManager
            val activityInfo = pm.getActivityInfo(componentName, 0)
            pm.getResourcesForApplication(activityInfo.applicationInfo)
                .getDrawable(activityInfo.icon, null)
        } catch (e: Exception) {
            getIcon(componentName.packageName)
        }
    }

    open fun getSystemStateForPackage(systemState: String, packageName: String): String {
        return systemState
    }
}
