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

package com.sandboxr.launcher.pm

import android.content.pm.PackageInstaller.SessionInfo
import android.os.UserHandle

data class PackageInstallInfo(
    @JvmField val packageName: String,
    @JvmField val state: Int,
    @JvmField val progress: Int,
    @JvmField val user: UserHandle,
) {
    companion object {
        const val STATUS_INSTALLED: Int = 0
        const val STATUS_INSTALLING: Int = 1
        const val STATUS_INSTALLED_DOWNLOADING: Int = 2
        const val STATUS_FAILED: Int = 3

        @JvmStatic
        fun fromInstallingState(info: SessionInfo): PackageInstallInfo {
            val pkg = info.appPackageName ?: ""
            val progress = (info.progress * 100f).toInt()
            val user = try {
                info.user ?: android.os.Process.myUserHandle()
            } catch (e: Exception) {
                android.os.Process.myUserHandle()
            }
            return PackageInstallInfo(
                packageName = pkg,
                state = STATUS_INSTALLING,
                progress = progress,
                user = user
            )
        }

        @JvmStatic
        fun fromState(state: Int, packageName: String, user: UserHandle): PackageInstallInfo {
            return PackageInstallInfo(
                packageName = packageName,
                state = state,
                progress = 0,
                user = user
            )
        }
    }
}
