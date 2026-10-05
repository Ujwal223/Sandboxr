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

package com.sandboxr.launcher.popup

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.sandboxr.launcher.LauncherSettings.Favorites
import com.sandboxr.launcher.R
import com.android.launcher3.logging.StatsLogManager.LauncherEvent.IGNORE
import com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_ALL_APPS_TAP_OR_LONGPRESS
import com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_SETTINGS_BUTTON_TAP_OR_LONGPRESS
import com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_WIDGETSTRAY_BUTTON_TAP_OR_LONGPRESS
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.popup.PopupCategory.SYSTEM_SHORTCUT
import com.sandboxr.launcher.views.ActivityContext
import com.sandboxr.launcher.views.OptionsPopupView.OptionItem

/** Class to create default set of long-press options. */
object WorkspaceLongPressOptions {

    @JvmStatic
    fun getAll(ctx: Context): List<PopupData> = buildList {
        add(
            PopupData(
                R.drawable.ic_palette,
                R.string.styles_wallpaper_button_text,
                SYSTEM_SHORTCUT,
                IGNORE,
            ) { ac, _, v ->
                startWallpaperPicker(ac, v)
            }
        )
        add(
            PopupData(
                R.drawable.widgets_24px,
                R.string.widget_button_text,
                SYSTEM_SHORTCUT,
                LAUNCHER_WIDGETSTRAY_BUTTON_TAP_OR_LONGPRESS,
            ) { ac, _, _ ->
                openWidgetPicker(ac.asContext())
            }
        )
        add(
            PopupData(
                R.drawable.ic_setting,
                R.string.settings_button_text,
                SYSTEM_SHORTCUT,
                LAUNCHER_SETTINGS_BUTTON_TAP_OR_LONGPRESS,
            ) { ac, _, _ ->
                ac.asContext()
                    .startActivity(
                        Intent(android.provider.Settings.ACTION_SETTINGS)
                            .addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            )
                    )
            }
        )
    }

    @JvmStatic
    fun getAllAsOptionItems(context: Context): List<OptionItem> =
        getAll(context).map {
            OptionItem(context, it.labelResId, it.iconResId, it.eventId) { v ->
                it.popupAction.invoke(ActivityContext.lookupContext(v.context), ItemInfo(), v)
                true
            }
        }

    private fun startWallpaperPicker(ac: ActivityContext, v: View) {
        val context = ac.asContext()
        val intent = Intent(Intent.ACTION_SET_WALLPAPER)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(EXTRA_WALLPAPER_OFFSET, 0.5f)
            .putExtra(EXTRA_WALLPAPER_LAUNCH_SOURCE, "app_launched_launcher")
            .putExtra(EXTRA_WALLPAPER_FLAVOR, "focus_wallpaper")
        val pickerPackage = context.getString(R.string.wallpaper_picker_package)
        if (!TextUtils.isEmpty(pickerPackage)) {
            intent.setPackage(pickerPackage)
        }
        ac.startActivitySafely(
            v,
            intent,
            WorkspaceItemInfo().also {
                it.intent = intent
                it.container = Favorites.CONTAINER_SETTINGS
            },
        )
    }

    @JvmStatic
    fun openWidgetPicker(ctx: Context): Boolean {
        if (ctx.packageManager.isSafeMode) {
            Toast.makeText(ctx, R.string.safemode_widget_error, Toast.LENGTH_SHORT).show()
            return false
        } else {
            val intent = Intent(Intent.ACTION_PICK)
            intent.setPackage(ctx.packageName)
            if (ctx !is android.app.Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                ctx.startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private const val EXTRA_WALLPAPER_OFFSET = "com.android.launcher3.WALLPAPER_OFFSET"
    private const val EXTRA_WALLPAPER_FLAVOR = "com.android.launcher3.WALLPAPER_FLAVOR"
    private const val EXTRA_WALLPAPER_LAUNCH_SOURCE = "com.android.wallpaper.LAUNCH_SOURCE"
}
