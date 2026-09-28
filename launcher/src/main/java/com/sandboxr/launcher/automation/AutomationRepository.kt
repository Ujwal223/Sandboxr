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

package com.sandboxr.launcher.automation

import android.os.UserHandle
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import com.sandboxr.launcher.util.ListenableDiffAwareRef
import com.sandboxr.launcher.util.PackageUserKey
import javax.inject.Inject

/** Repository to provide information related to automated apps */
interface AutomationRepository {

    /** Unified ref for the set of all automated packages and the changes occurring to them. */
    val automatedPackages: ListenableDiffAwareRef<Set<PackageUserKey>, AutomationChange>?
        get() = null

    /** Returns if the provided package is being automated for the provided user */
    fun isPackageAutomated(user: UserHandle, packageName: String): Boolean = false

    /** Returns if the provided package is being automated for the provided user ID */
    fun isPackageAutomated(userId: Int, packageName: String): Boolean = false

    companion object {
        @JvmField
        val INSTANCE = com.sandboxr.launcher.util.DaggerSingletonObject {
            DefaultAutomationRepository()
        }
    }
}

/** Contains the delta of automated packages from an automation change for a given user. */
data class AutomationChange(
    val userHandle: UserHandle,
    val addedPackages: Set<String>,
    val removedPackages: Set<String>,
)

@LauncherAppSingleton
class DefaultAutomationRepository @Inject constructor() : AutomationRepository
