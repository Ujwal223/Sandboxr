/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.SparseArray
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.DropTarget
import com.sandboxr.launcher.LauncherSettings
import com.sandboxr.launcher.dragndrop.DragController
import com.sandboxr.launcher.dragndrop.DragOptions
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.views.ActivityContext
import com.sandboxr.launcher.views.BubbleTextHolder

abstract class BaseAccessibilityDelegate<T>(
    protected val mContext: T
) : View.AccessibilityDelegate(), DragController.DragListener
        where T : Context, T : ActivityContext {

    enum class DragType {
        ICON,
        FOLDER,
        APP_PAIR,
        FILESYSTEM_ICON,
        WIDGET
    }

    class DragInfo {
        @JvmField
        var dragType: DragType = DragType.ICON
        @JvmField
        var info: ItemInfo? = null
        @JvmField
        var item: View? = null
    }

    @JvmField
    protected val mActions = SparseArray<LauncherAction>()

    @JvmField
    protected var mDragInfo: DragInfo? = null

    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(host, info)
        val tag = host.tag
        if (tag is ItemInfo) {
            val actions = mutableListOf<LauncherAction>()
            getSupportedActions(host, tag, actions)
            actions.forEach { la -> info.addAction(la.accessibilityAction) }

            if (!itemSupportsLongClick(host)) {
                info.isLongClickable = false
                info.removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK)
            }
        }
    }

    /**
     * Adds all the accessibility actions that can be handled.
     */
    abstract fun getSupportedActions(host: View, item: ItemInfo, out: MutableList<LauncherAction>)

    private fun itemSupportsLongClick(host: View): Boolean {
        return when (host) {
            is BubbleTextView -> host.canShowLongPressPopup()
            is BubbleTextHolder -> host.bubbleText?.canShowLongPressPopup() == true
            else -> false
        }
    }

    protected open fun itemSupportsAccessibleDrag(item: ItemInfo): Boolean {
        if (item is WorkspaceItemInfo) {
            return item.screenId >= 0 &&
                    item.container != LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION
        }
        return item.id != ItemInfo.NO_ID
    }

    override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
        val tag = host.tag
        if (tag is ItemInfo && performAction(host, tag, action, false)) {
            return true
        }
        return super.performAccessibilityAction(host, action, args)
    }

    abstract fun performAction(
        host: View,
        item: ItemInfo,
        action: Int,
        fromKeyboard: Boolean
    ): Boolean

    protected open fun announceConfirmation(confirmation: String) {
        mContext.getDragLayer()?.announceForAccessibility(confirmation)
    }

    fun isInAccessibleDrag(): Boolean = mDragInfo != null

    fun getDragInfo(): DragInfo? = mDragInfo

    /**
     * @param clickedTarget the actual view that was clicked
     * @param dropLocation relative to [clickedTarget]. If provided, its center is used
     * as the actual drop location otherwise the view's center is used.
     */
    fun handleAccessibleDrop(clickedTarget: View, dropLocation: Rect?) {
        if (!isInAccessibleDrag()) return

        val loc = IntArray(2)
        if (dropLocation == null) {
            loc[0] = clickedTarget.width / 2
            loc[1] = clickedTarget.height / 2
        } else {
            loc[0] = dropLocation.centerX()
            loc[1] = dropLocation.centerY()
        }

        mContext.getDragLayer()?.getDescendantCoordRelativeToSelf(clickedTarget, loc)
        mContext.getDragController()?.completeAccessibleDrag(loc)
    }

    abstract fun beginAccessibleDrag(item: View, info: ItemInfo, fromKeyboard: Boolean): Boolean

    override fun onDragEnd() {
        mContext.getDragController()?.removeDragListener(this)
        mDragInfo = null
    }

    override fun onDragStart(dragObject: DropTarget.DragObject, options: DragOptions) {
        // No-op
    }

    open class LauncherAction(
        val keyCode: Int,
        val accessibilityAction: AccessibilityNodeInfo.AccessibilityAction,
        private val invoker: ((View?, ItemInfo, Int) -> Boolean)? = null
    ) {
        constructor(id: Int, label: CharSequence, keyCode: Int, invoker: ((View?, ItemInfo, Int) -> Boolean)? = null) : this(
            keyCode,
            AccessibilityNodeInfo.AccessibilityAction(id, label),
            invoker
        )

        /**
         * Invokes the action for the provided host
         */
        fun invokeFromKeyboard(host: View?): Boolean {
            val tag = host?.tag
            return if (tag is ItemInfo && invoker != null) {
                invoker.invoke(host, tag, accessibilityAction.id)
            } else {
                false
            }
        }
    }
}
