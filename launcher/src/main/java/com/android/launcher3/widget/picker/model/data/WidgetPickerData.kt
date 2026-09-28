/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.launcher3.widget.picker.model.data

import com.android.launcher3.model.WidgetItem
import com.android.launcher3.util.PackageUserKey
import com.android.launcher3.widget.model.WidgetsListBaseEntry

/** Widget data for display in the widget picker. */
data class WidgetPickerData(val allWidgets: List<WidgetsListBaseEntry> = listOf()) {

    fun findAllWidgetsForPackageUser(packageUserKey: PackageUserKey): List<WidgetItem> =
        allWidgets.firstOrNull {
            PackageUserKey.fromPackageItemInfo(it.mPkgItem) == packageUserKey
        }?.mWidgets ?: emptyList()
}
