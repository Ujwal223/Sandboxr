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

package com.sandboxr.launcher.dot

/**
 * Aggregates notification dots across multiple folder items.
 */
class FolderDotInfo : DotInfo() {
    private var mNumItemsWithNotes: Int = 0

    fun addDotInfo(dotInfo: DotInfo?) {
        if (dotInfo != null && dotInfo.hasDot()) {
            mNumItemsWithNotes++
            for (key in dotInfo.getNotificationKeys()) {
                addOrUpdateNotificationKey(key)
            }
        }
    }

    fun subtractDotInfo(dotInfo: DotInfo?) {
        if (dotInfo != null && dotInfo.hasDot()) {
            mNumItemsWithNotes = maxOf(0, mNumItemsWithNotes - 1)
            for (key in dotInfo.getNotificationKeys()) {
                removeNotificationKey(key)
            }
        }
    }

    override fun hasDot(): Boolean = mNumItemsWithNotes > 0
}
