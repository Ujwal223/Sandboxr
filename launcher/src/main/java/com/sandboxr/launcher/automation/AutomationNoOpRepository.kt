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
import com.sandboxr.launcher.util.MutableDiffAwareRef
import com.sandboxr.launcher.util.PackageUserKey
import javax.inject.Inject

/**
 * No-op implementation of [AutomationRepository].
 */
@LauncherAppSingleton
class AutomationNoOpRepository @Inject constructor() : AutomationRepository {

    override val automatedPackages: ListenableDiffAwareRef<Set<PackageUserKey>, AutomationChange> =
        MutableDiffAwareRef(emptySet<PackageUserKey>())

    override fun isPackageAutomated(user: UserHandle, packageName: String): Boolean = false

    override fun isPackageAutomated(userId: Int, packageName: String): Boolean = false
}
