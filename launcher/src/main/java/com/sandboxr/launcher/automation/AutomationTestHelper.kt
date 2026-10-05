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

package com.sandboxr.launcher.automation

import android.os.Process
import android.os.UserHandle
import com.sandboxr.launcher.util.ListenableDiffAwareRef
import com.sandboxr.launcher.util.MutableDiffAwareRef
import com.sandboxr.launcher.util.PackageUserKey
import java.util.concurrent.ConcurrentHashMap

/**
 * UI automation helper for test frameworks (TAPL, Robolectric, UI Automator).
 * Allows test suites to simulate and verify automated app interactions.
 */
class AutomationTestHelper : AutomationRepository {

    private val mAutomatedPackages = ConcurrentHashMap.newKeySet<PackageUserKey>()
    private val mDiffAwareRef = MutableDiffAwareRef<Set<PackageUserKey>, AutomationChange>(mAutomatedPackages)

    override val automatedPackages: ListenableDiffAwareRef<Set<PackageUserKey>, AutomationChange>
        get() = mDiffAwareRef

    /** Registers a package as automated for testing purposes. */
    fun setPackageAutomated(user: UserHandle, packageName: String, automated: Boolean) {
        val key = PackageUserKey(packageName, user)
        val changed = if (automated) {
            mAutomatedPackages.add(key)
        } else {
            mAutomatedPackages.remove(key)
        }

        if (changed) {
            val change = AutomationChange(
                userHandle = user,
                addedPackages = if (automated) setOf(packageName) else emptySet(),
                removedPackages = if (!automated) setOf(packageName) else emptySet()
            )
            mDiffAwareRef.dispatchValue(HashSet(mAutomatedPackages), change)
        }
    }

    override fun isPackageAutomated(user: UserHandle, packageName: String): Boolean {
        return mAutomatedPackages.contains(PackageUserKey(packageName, user))
    }

    override fun isPackageAutomated(userId: Int, packageName: String): Boolean {
        return mAutomatedPackages.any {
            it.mPackageName == packageName && it.mUser.hashCode() == userId
        }
    }

    /** Clears all automated packages. */
    fun reset() {
        val previous = HashSet(mAutomatedPackages)
        mAutomatedPackages.clear()
        if (previous.isNotEmpty()) {
            val change = AutomationChange(
                userHandle = Process.myUserHandle(),
                addedPackages = emptySet(),
                removedPackages = previous.map { it.mPackageName }.toSet()
            )
            mDiffAwareRef.dispatchValue(emptySet<PackageUserKey>(), change)
        }
    }
}
