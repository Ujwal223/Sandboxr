/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.sandboxr.launcher.workspacefunctions

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import com.sandboxr.launcher.appfunctions.workspace.provider.InstalledItemsProvider
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.model.repository.AppsListRepository
import javax.inject.Inject

/**
 * Returns installed apps on the device using [AppsListRepository] and [LauncherApps].
 */
class LauncherInstalledAppsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appsListRepository: AppsListRepository
) : InstalledItemsProvider<LauncherActivityInfo> {

    private val launcherApps: LauncherApps? = context.getSystemService(LauncherApps::class.java)

    override suspend fun getInstalledItems(orderByUsageStats: Boolean): List<LauncherActivityInfo> {
        val apps = appsListRepository.appsListStateRef.value.apps
        val service = launcherApps ?: return emptyList()
        return apps.mapNotNull { app ->
            service.resolveActivity(app.intent, app.user)
        }
    }
}
