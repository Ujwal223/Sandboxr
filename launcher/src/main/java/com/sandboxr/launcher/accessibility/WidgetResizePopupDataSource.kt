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

package com.sandboxr.launcher.accessibility

import android.appwidget.AppWidgetHostView
import android.view.View
import com.sandboxr.launcher.CellLayout
import com.sandboxr.launcher.R
import com.sandboxr.launcher.celllayout.CellLayoutLayoutParams
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.popup.PopupCategory
import com.sandboxr.launcher.popup.PopupData
import com.sandboxr.launcher.views.ActivityContext

/** Various popup actions for widget resizing */
object WidgetResizePopupDataSource {

    @JvmStatic
    fun increaseWidthAction(): PopupData =
        resizeAction(R.drawable.ic_widget_width_increase, R.string.action_increase_width)

    @JvmStatic
    fun decreaseWidthAction(): PopupData =
        resizeAction(R.drawable.ic_widget_width_decrease, R.string.action_decrease_width)

    @JvmStatic
    fun increaseHeightAction(): PopupData =
        resizeAction(R.drawable.ic_widget_height_increase, R.string.action_increase_height)

    @JvmStatic
    fun decreaseHeightAction(): PopupData =
        resizeAction(R.drawable.ic_widget_height_decrease, R.string.action_decrease_height)

    private fun resizeAction(iconResId: Int, labelResId: Int): PopupData =
        PopupData(iconResId = iconResId, labelResId = labelResId, category = PopupCategory.SYSTEM_SHORTCUT) {
            activity, info, view ->
            performResizeAction(labelResId, activity, info, view)
        }

    private fun performResizeAction(
        action: Int,
        context: ActivityContext,
        info: ItemInfo,
        host: View,
    ) {
        val lp = host.layoutParams as? CellLayoutLayoutParams ?: return
        val layout = (host.parent as? View)?.parent as? CellLayout ?: return
        layout.markCellsAsUnoccupiedForView(host)

        if (action == R.string.action_increase_width) {
            if (
                ((host.layoutDirection == View.LAYOUT_DIRECTION_RTL) &&
                    layout.isRegionVacant(info.cellX - 1, info.cellY, 1, info.spanY)) ||
                !layout.isRegionVacant(info.cellX + info.spanX, info.cellY, 1, info.spanY)
            ) {
                lp.cellX -= 1
                info.cellX--
            }
            lp.cellHSpan++
            info.spanX++
        } else if (action == R.string.action_decrease_width) {
            lp.cellHSpan--
            info.spanX--
        } else if (action == R.string.action_increase_height) {
            if (!layout.isRegionVacant(info.cellX, info.cellY + info.spanY, info.spanX, 1)) {
                lp.cellY -= 1
                info.cellY--
            }
            lp.cellVSpan++
            info.spanY++
        } else if (action == R.string.action_decrease_height) {
            lp.cellVSpan--
            info.spanY--
        }

        layout.markCellsAsOccupiedForView(host)
        host.requestLayout()
        (context as? com.sandboxr.launcher.Launcher)?.getModelWriter()?.updateItemInDatabase(info)
    }
}
