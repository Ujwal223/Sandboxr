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

package com.sandboxr.launcher.views

import android.content.Context
import android.content.ContextWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.savedstate.SavedStateRegistryOwner
import com.sandboxr.launcher.DeviceProfile
import com.sandboxr.launcher.DeviceProfile.OnDeviceProfileChangeListener
import com.sandboxr.launcher.dagger.ActivityContextComponent
import com.sandboxr.launcher.util.Executors
import com.sandboxr.launcher.util.LooperExecutor
import com.sandboxr.launcher.util.SafeCloseable
import com.sandboxr.launcher.util.ViewCache

/**
 * An interface used along with a Context for various launcher activities and containers.
 */
interface ActivityContext : SavedStateRegistryOwner {

    fun getActivityComponent(): ActivityContextComponent?

    fun getUiExecutor(): LooperExecutor {
        return Executors.MAIN_EXECUTOR
    }

    val cellPosMapper: com.android.launcher3.celllayout.CellPosMapper
        get() = com.android.launcher3.celllayout.CellPosMapper.DEFAULT

    fun getUndoDeleteController(): com.android.launcher3.UndoDeleteController? = null

    fun getDropTargetHandler(): com.sandboxr.launcher.DropTargetHandler? = null

    fun getDeviceProfile(): DeviceProfile

    fun getOnDeviceProfileChangeListeners(): List<OnDeviceProfileChangeListener> {
        return emptyList()
    }

    fun getViewCache(): ViewCache? {
        return null
    }

    fun getRootView(): View?

    fun getDragLayer(): BaseDragLayer<*>? {
        return null
    }

    fun getDragController(): com.sandboxr.launcher.dragndrop.DragController? {
        return null
    }

    fun finishAutoCancelActionMode(): Boolean {
        return false
    }

    fun isBubbleBarEnabled(): Boolean = false

    fun hasBubbles(): Boolean = false

    fun asContext(): Context {
        return this as Context
    }

    fun getLayoutInflater(): LayoutInflater?

    fun getItemOnClickListener(): View.OnClickListener? = null

    fun getAllAppsItemLongClickListener(): View.OnLongClickListener? = null

    fun getAllAppsItemCustomActionsListener(): Any? = null

    fun getAppsStore(): com.sandboxr.launcher.allapps.AllAppsStore? = null

    fun getAppsView(): com.sandboxr.launcher.allapps.ActivityAllAppsContainerView<*>? = null

    fun hideKeyboard() {}

    fun startActivitySafely(v: View?, intent: android.content.Intent?, item: com.sandboxr.launcher.model.data.ItemInfo?): Boolean = false

    fun closeOnDestroy(closeable: SafeCloseable) {}

    fun canUseMultipleShadesForPopup(): Boolean = false

    fun getStatsLogManager(): com.sandboxr.launcher.logging.StatsLogManager {
        return com.sandboxr.launcher.logging.StatsLogManager.newInstance(asContext())
    }

    fun getAccessibilityDelegate(): View.AccessibilityDelegate? = null

    fun getActivityLaunchOptions(v: View, item: com.sandboxr.launcher.model.data.ItemInfo): com.sandboxr.launcher.util.ActivityOptionsWrapper {
        return com.sandboxr.launcher.util.ActivityOptionsWrapper(null, com.sandboxr.launcher.util.RunnableList())
    }

    fun sendPendingIntentWithAnimation(view: View, pendingIntent: android.app.PendingIntent, itemInfo: com.sandboxr.launcher.model.data.ItemInfo): com.sandboxr.launcher.util.RunnableList? = null

    fun getCellLayout(container: Int, screenId: Int): com.sandboxr.launcher.CellLayout? = null

    fun getSupportedShortcuts(itemInfo: com.sandboxr.launcher.model.data.ItemInfo): java.util.stream.Stream<com.sandboxr.launcher.popup.SystemShortcut.Factory<*>> {
        return java.util.stream.Stream.empty()
    }

    fun refreshAndBindWidgetsForPackageUser(packageUser: com.sandboxr.launcher.util.PackageUserKey?) {}

    fun getPopupDataProvider(): com.sandboxr.launcher.popup.PopupDataProvider? = null

    fun getPopupControllerForAppIcons(): com.sandboxr.launcher.popup.PopupController<*>? = null

    fun getContent(): com.sandboxr.launcher.util.LauncherBindableItemsContainer? = null

    companion object {
        const val TAG = "ActivityContext"

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T : ActivityContext> lookupContext(context: Context): T {
            var current: Context? = context
            while (current != null) {
                if (current is ActivityContext) {
                    return current as T
                }
                current = if (current is ContextWrapper) current.baseContext else null
            }
            throw IllegalArgumentException("Cannot find ActivityContext in parent tree of $context")
        }
    }
}
