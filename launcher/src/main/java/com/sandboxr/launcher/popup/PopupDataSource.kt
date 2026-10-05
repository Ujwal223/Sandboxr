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

import android.content.Intent
import android.os.Process
import android.util.Log
import android.view.View
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.AbstractFloatingViewHelper
import com.sandboxr.launcher.DropTargetHandler
import com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
import com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_APP_GROUP
import com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET
import com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_FOLDER
import com.sandboxr.launcher.R
import com.sandboxr.launcher.SecondaryDropTarget
import com.android.launcher3.Utilities
import com.android.launcher3.logging.StatsLogManager.LauncherEvent
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.LauncherAppWidgetInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.popup.SystemShortcut.BubbleActivityStarter
import com.sandboxr.launcher.popup.SystemShortcut.TaskbarBubbleActivityStarter
import com.sandboxr.launcher.util.ActivityOptionsWrapper
import com.sandboxr.launcher.util.PackageManagerHelper
import com.sandboxr.launcher.views.ActivityContext
import com.android.wm.shell.shared.bubbles.logging.EntryPoint

object PopupDataSource {

    val removePopupData =
        PopupData(
            iconResId = R.drawable.ic_uninstall_no_shadow,
            labelResId = R.string.remove_system_shortcut_label,
            category = PopupCategory.SYSTEM_SHORTCUT_FIXED,
        ) { activityContext: ActivityContext, itemInfo: ItemInfo, view: View ->
            AbstractFloatingView.closeAllOpenViews(activityContext)
            val dropTargetHandler: DropTargetHandler? = activityContext.getDropTargetHandler()
            if (dropTargetHandler != null) {
                dropTargetHandler.prepareToUndoDelete(itemInfo)
                dropTargetHandler.onDeleteComplete(itemInfo, view)
            }
        }
}

object FolderSystemShortcuts : PopupDataMapper {

    override fun getPopupDataByItemInfo(itemInfo: ItemInfo): List<PopupData>? =
        if (itemInfo.itemType == ITEM_TYPE_FOLDER) listOf(PopupDataSource.removePopupData) else null
}

object AppPairSystemShortcuts : PopupDataMapper {

    override fun getPopupDataByItemInfo(itemInfo: ItemInfo): List<PopupData>? =
        if (itemInfo.itemType == ITEM_TYPE_APP_GROUP) listOf(PopupDataSource.removePopupData)
        else null
}

object AppWidgetSystemShortcuts : PopupDataMapper {
    private const val TAG = "AppWidgetSystemShortcuts"

    private val widgetSettingsPopupData =
        PopupData(
            iconResId = R.drawable.ic_setting,
            labelResId = R.string.widget_settings,
            category = PopupCategory.SYSTEM_SHORTCUT_FIXED,
        ) { activityContext: ActivityContext, itemInfo: ItemInfo, view: View ->
            Log.d(TAG, "widgetSettings clicked for $itemInfo")
        }

    override fun getPopupDataByItemInfo(itemInfo: ItemInfo): List<PopupData>? {
        return if (itemInfo.itemType != ITEM_TYPE_APPWIDGET) null
        else if (itemInfo is LauncherAppWidgetInfo && itemInfo.isReconfigurable) {
            listOf(PopupDataSource.removePopupData, widgetSettingsPopupData)
        } else {
            listOf(PopupDataSource.removePopupData)
        }
    }
}

object CustomWidgetSystemShortcuts : PopupDataMapper {

    override fun getPopupDataByItemInfo(itemInfo: ItemInfo): List<PopupData>? =
        if (itemInfo.itemType == ITEM_TYPE_CUSTOM_APPWIDGET) listOf(PopupDataSource.removePopupData)
        else null
}

object UnusedShortcuts {

    private val handleAddToHomeScreenFromAllApps =
        { activityContext: ActivityContext, itemInfo: ItemInfo, view: View ->
            AbstractFloatingView.closeAllOpenViews(activityContext)
            activityContext.getStatsLogManager()
                .logger()
                .withItemInfo(itemInfo)
                .log(LauncherEvent.LAUNCHER_TAP_TO_ADD_TO_HOME_SCREEN_FROM_ALL_APPS)
        }

    val addToHomeScreenFromAllAppsPopupData =
        PopupData(
            iconResId = R.drawable.ic_plus,
            labelResId = R.string.action_add_to_workspace,
            category = PopupCategory.SYSTEM_SHORTCUT_FIXED,
            popupAction = handleAddToHomeScreenFromAllApps,
        )

    private val handleAppInfo =
        { activityContext: ActivityContext, itemInfo: ItemInfo, view: View ->
            val sourceBounds = Utilities.getViewBounds(view)
            val options: ActivityOptionsWrapper =
                activityContext.getActivityLaunchOptions(view, itemInfo)

            options.onEndCallback.add { dismissTaskMenuView(activityContext) }
            PackageManagerHelper.startDetailsActivityForInfo(
                view.context,
                itemInfo,
                sourceBounds,
                options.toBundle(),
            )
        }

    val appInfoPopupData =
        PopupData(
            iconResId = R.drawable.info_24px,
            labelResId = R.string.app_info_drop_target_label,
            category = PopupCategory.SYSTEM_SHORTCUT,
            eventId = LauncherEvent.LAUNCHER_SYSTEM_SHORTCUT_APP_INFO_TAP,
            popupAction = handleAppInfo,
        )

    private val handleInstall =
        { activityContext: ActivityContext, itemInfo: ItemInfo, view: View ->
            val intent = Intent(Intent.ACTION_VIEW)
            if (itemInfo.targetComponent != null) {
                intent.data = android.net.Uri.parse("market://details?id=" + itemInfo.targetComponent!!.packageName)
            }
            activityContext.startActivitySafely(view, intent, itemInfo)
            AbstractFloatingView.closeAllOpenViews(activityContext)
        }

    val installPopupData =
        PopupData(
            iconResId = R.drawable.ic_install_no_shadow,
            labelResId = R.string.install_drop_target_label,
            category = PopupCategory.SYSTEM_SHORTCUT,
            popupAction = handleInstall,
        )

    private val handleDontSuggestApp =
        { activityContext: ActivityContext, itemInfo: ItemInfo, view: View ->
            dismissTaskMenuView(activityContext)
            android.widget.Toast.makeText(view.context, R.string.item_removed, android.widget.Toast.LENGTH_SHORT).show()
        }

    val dontSuggestAppPopupData =
        PopupData(
            iconResId = R.drawable.ic_block_no_shadow,
            labelResId = R.string.dismiss_prediction_label,
            category = PopupCategory.SYSTEM_SHORTCUT,
            eventId = LauncherEvent.LAUNCHER_SYSTEM_SHORTCUT_DONT_SUGGEST_APP_TAP,
            popupAction = handleDontSuggestApp,
        )

    private val handleUninstallApp =
        { activityContext: ActivityContext, itemInfo: ItemInfo, view: View ->
            dismissTaskMenuView(activityContext)
            val componentName = SecondaryDropTarget.getUninstallTarget(view.context, itemInfo)
            if (componentName != null) {
                SecondaryDropTarget.performUninstall(view.context, componentName, itemInfo)
            }
            Unit
        }

    val uninstallAppPopupData =
        PopupData(
            iconResId = R.drawable.ic_uninstall_no_shadow,
            labelResId = R.string.uninstall_private_system_shortcut_label,
            category = PopupCategory.SYSTEM_SHORTCUT,
            eventId = LauncherEvent.LAUNCHER_PRIVATE_SPACE_UNINSTALL_SYSTEM_SHORTCUT_TAP,
            popupAction = handleUninstallApp,
        )

    private fun dismissTaskMenuView(activityContext: ActivityContext) {
        AbstractFloatingView.closeOpenViews(
            activityContext,
            true,
            AbstractFloatingView.TYPE_ALL and AbstractFloatingView.TYPE_REBIND_SAFE.inv(),
        )
    }

    private const val TAG = "PopupDataSource"
}
