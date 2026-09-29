/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.sandboxr.launcher.allapps

import android.content.Context
import android.os.Process
import android.os.UserHandle
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.pm.UserCache
import com.sandboxr.launcher.util.LabelComparator
import java.util.Comparator

/**
 * Comparator to arrange applications alphabetically by label and user profiles.
 */
class AppInfoComparator(context: Context) : Comparator<AppInfo> {

    private val userManager: UserCache = UserCache.getInstance(context)
    private val myUser: UserHandle = Process.myUserHandle()
    private val labelComparator: LabelComparator = LabelComparator()

    override fun compare(a: AppInfo?, b: AppInfo?): Int {
        if (a === b) return 0
        if (a == null) return -1
        if (b == null) return 1

        // Order by the title in the current locale
        val result = labelComparator.compare(getSortingTitle(a), getSortingTitle(b))
        if (result != 0) {
            return result
        }

        // If labels are same, compare component names
        val compA = a.componentName
        val compB = b.componentName
        if (compA != null && compB != null) {
            val compResult = compA.compareTo(compB)
            if (compResult != 0) {
                return compResult
            }
        }

        // Check user handle priority: primary user first, then sort by serial number
        if (myUser == a.user && myUser != b.user) {
            return -1
        } else if (myUser != a.user && myUser == b.user) {
            return 1
        } else if (a.user != null && b.user != null) {
            val aUserSerial = userManager.getSerialNumberForUser(a.user)
            val bUserSerial = userManager.getSerialNumberForUser(b.user)
            return aUserSerial.compareTo(bUserSerial)
        }

        return 0
    }

    private fun getSortingTitle(info: AppInfo): String {
        val appTitle = info.appTitle
        if (!appTitle.isNullOrEmpty()) {
            return appTitle.toString()
        }
        val title = info.title
        if (!title.isNullOrEmpty()) {
            return title.toString()
        }
        return ""
    }
}
