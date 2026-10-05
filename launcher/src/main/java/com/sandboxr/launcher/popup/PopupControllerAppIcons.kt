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

package com.sandboxr.launcher.popup

import android.content.Context
import android.os.Trace
import android.view.View
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.Flags
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.popup.ui.PopupItem
import com.sandboxr.launcher.util.PackageUserKey
import com.sandboxr.launcher.util.ShortcutUtil
import com.sandboxr.launcher.views.ActivityContext
import java.util.stream.Collectors

/**
 * Controller for app icons. It handles actions for the popups such as showing and dismissing
 * popups. This is used for icons and shortcuts in the workspace, hotseat, and all apps.
 */
class PopupControllerForAppIcon<T> : PopupController<T> where T : Context, T : ActivityContext {

    @Suppress("UNCHECKED_CAST")
    override fun show(view: View): Popup? {
        val container: PopupContainer<T>
        val icon = view as BubbleTextView
        val activityContext: T = ActivityContext.lookupContext(icon.context) as T
        val item = icon.tag as ItemInfo
        try {
            Trace.beginSection("showPopupMenu")
            if (PopupContainer.getOpen(activityContext) != null) {
                // There is already an items container open, so don't open this one.
                icon.clearFocus()
                return null
            }
            if (!ShortcutUtil.supportsShortcuts(item)) {
                return null
            }
            val popupDataProvider =
                activityContext.getPopupDataProvider()
                    ?: activityContext.activityComponent?.popupDataProvider
                    ?: PopupDataProvider(activityContext)
            val deepShortcutCount = popupDataProvider.getShortcutCountForItem(item)
            val systemShortcuts =
                activityContext
                    .getSupportedShortcuts(item)
                    .map { s ->
                        @Suppress("UNCHECKED_CAST")
                        (s as SystemShortcut.Factory<in T>).getShortcut(activityContext, item, icon) as SystemShortcut<T>?
                    }
                    .filter { it != null }
                    .map { it!! }
                    .collect(Collectors.toList())

            container =
                PopupContainerWithArrow.create(
                    context = activityContext,
                    originalView = icon,
                    itemInfo = item,
                )
            container.deepShortcutDragHandler =
                LauncherDeepShortcutDragHandler(activityContext as Launcher, container)
            container.configureForLauncher(activityContext, item)
            if (Flags.expandableLongPressMenu()) {
                val systemShortcutRedesign =
                    systemShortcuts.map { shortcut ->
                        PopupItem(
                            iconResId = shortcut.iconResId,
                            labelResId = shortcut.labelResId,
                            popupAction = { shortcut.onClick(view) },
                            category =
                                if (shortcut.mIsCollapsible) PopupCategory.SYSTEM_SHORTCUT
                                else PopupCategory.SYSTEM_SHORTCUT_FIXED,
                        )
                    }

                container.showComposePopup(
                    if (view.showingMinimalPopup) emptyList() else systemShortcutRedesign,
                    deepShortcutCount,
                )
            } else {
                container.populateAndShowRows(
                    deepShortcutCount,
                    if (view.showingMinimalPopup) emptyList() else systemShortcuts,
                )
            }
            activityContext.refreshAndBindWidgetsForPackageUser(PackageUserKey.fromItemInfo(item))
            container.requestFocus()
        } finally {
            logEvent(activityContext.statsLogManager, item.itemType, PopupEvent.OPEN)
            Trace.endSection()
        }
        return container
    }

    override fun dismiss() {}
}
