/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.animation.AnimatorSet
import android.appwidget.AppWidgetProviderInfo
import android.graphics.Point
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Pair
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.sandboxr.launcher.AbstractFloatingView
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.CellLayout
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherSettings
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.PendingAddItemInfo
import com.sandboxr.launcher.R
import com.sandboxr.launcher.ShortcutAndWidgetContainer
import com.sandboxr.launcher.Workspace
import com.sandboxr.launcher.anim.AnimatorListeners
import com.sandboxr.launcher.apppairs.AppPairIcon
import com.sandboxr.launcher.automation.AutomationRepository
import com.sandboxr.launcher.dragndrop.DragOptions
import com.sandboxr.launcher.dragndrop.DragView
import com.sandboxr.launcher.folder.Folder
import com.sandboxr.launcher.folder.FolderIcon
import com.sandboxr.launcher.keyboard.KeyboardDragAndDropView
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.AppPairInfo
import com.sandboxr.launcher.model.data.CollectionInfo
import com.sandboxr.launcher.model.data.FolderInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.ItemInfoWithIcon
import com.sandboxr.launcher.model.data.LauncherAppWidgetInfo
import com.sandboxr.launcher.model.data.WorkspaceItemFactory
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.popup.PopupContainer
import com.sandboxr.launcher.popup.PopupController
import com.sandboxr.launcher.popup.PopupData
import com.sandboxr.launcher.shortcuts.DeepShortcutView
import com.sandboxr.launcher.touch.ItemLongClickListener
import com.sandboxr.launcher.util.IntArray
import com.sandboxr.launcher.util.ShortcutUtil
import com.sandboxr.launcher.views.BubbleTextHolder
import com.sandboxr.launcher.widget.AppWidgetResizeFrame
import com.sandboxr.launcher.widget.LauncherAppWidgetHostView
import com.sandboxr.launcher.widget.NavigableAppWidgetHostView
import com.sandboxr.launcher.widget.PendingAddWidgetInfo
import java.util.Collections
import java.util.function.Consumer

open class LauncherAccessibilityDelegate(launcher: Launcher) : BaseAccessibilityDelegate<Launcher>(launcher) {

    init {
        mActions.put(REMOVE, LauncherAction(REMOVE, mContext.getString(R.string.remove_drop_target_label), KeyEvent.KEYCODE_X))
        mActions.put(UNINSTALL, LauncherAction(UNINSTALL, mContext.getString(R.string.uninstall_drop_target_label), KeyEvent.KEYCODE_U))
        mActions.put(DISMISS_PREDICTION, LauncherAction(DISMISS_PREDICTION, mContext.getString(R.string.dismiss_prediction_label), KeyEvent.KEYCODE_X))
        mActions.put(RECONFIGURE, LauncherAction(RECONFIGURE, mContext.getString(R.string.gadget_setup_text), KeyEvent.KEYCODE_E))
        mActions.put(ADD_TO_WORKSPACE, LauncherAction(ADD_TO_WORKSPACE, mContext.getString(R.string.action_add_to_workspace), KeyEvent.KEYCODE_P))
        mActions.put(MOVE, LauncherAction(MOVE, mContext.getString(R.string.action_move), KeyEvent.KEYCODE_M))
        mActions.put(MOVE_TO_WORKSPACE, LauncherAction(MOVE_TO_WORKSPACE, mContext.getString(R.string.action_move_to_workspace), KeyEvent.KEYCODE_P))
        mActions.put(RESIZE, LauncherAction(RESIZE, mContext.getString(R.string.action_resize), KeyEvent.KEYCODE_R))
        mActions.put(DEEP_SHORTCUTS, LauncherAction(DEEP_SHORTCUTS, mContext.getString(R.string.action_deep_shortcut), KeyEvent.KEYCODE_S))
        mActions.put(CLOSE, LauncherAction(CLOSE, mContext.getString(R.string.action_close), KeyEvent.KEYCODE_X))
    }

    override fun getSupportedActions(host: View, item: ItemInfo, out: MutableList<LauncherAction>) {
        if (isNotInShortcutMenu(host) && ShortcutUtil.supportsShortcuts(item)) {
            mActions.get(DEEP_SHORTCUTS)?.let { out.add(it) }
        }

        val dropTargetBar = mContext.getDropTargetBar()
        if (dropTargetBar != null) {
            for (target in dropTargetBar.getDropTargets()) {
                val dropTargetAction = target.getSupportedAccessibilityAction(item, host)
                if (dropTargetAction != INVALID) {
                    mActions.get(dropTargetAction)?.let { out.add(it) }
                }
            }
        }

        if (itemSupportsAccessibleDrag(item)) {
            mActions.get(MOVE)?.let { out.add(it) }

            if (item.container >= 0) {
                mActions.get(MOVE_TO_WORKSPACE)?.let { out.add(it) }
            } else if (item is LauncherAppWidgetInfo) {
                if (getSupportedResizeActions(host, item).isNotEmpty()) {
                    mActions.get(RESIZE)?.let { out.add(it) }
                }
            }
        }

        if (host is AppWidgetResizeFrame) {
            mActions.get(CLOSE)?.let { out.add(it) }
        }

        if (supportAddToWorkSpace(item)) {
            mActions.get(ADD_TO_WORKSPACE)?.let { out.add(it) }
        }
    }

    private fun supportAddToWorkSpace(item: ItemInfo): Boolean {
        if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            return false
        }
        return ((item is AppInfo) && (item.runtimeStatusFlags and ItemInfoWithIcon.FLAG_NOT_PINNABLE == 0)) ||
                ((item is WorkspaceItemInfo) && (item.runtimeStatusFlags and ItemInfoWithIcon.FLAG_NOT_PINNABLE == 0)) ||
                (item is PendingAddItemInfo)
    }

    override fun performAction(
        host: View,
        item: ItemInfo,
        action: Int,
        fromKeyboard: Boolean
    ): Boolean {
        if (action == AccessibilityNodeInfo.ACTION_LONG_CLICK) {
            var popupController: PopupController<Launcher>? = null
            if (host is BubbleTextView || (host is BubbleTextHolder && host.bubbleText != null)) {
                popupController = if (ShortcutUtil.supportsShortcuts(item)) {
                    mContext.getPopupControllerForAppIcons()
                } else {
                    mContext.getPopupControllerForHomeScreenItems()
                }
            } else if (host is FolderIcon || host is AppPairIcon || host is NavigableAppWidgetHostView) {
                popupController = mContext.getPopupControllerForHomeScreenItems()
            }

            if (popupController == null) {
                return false
            }

            val popup = popupController.show(host)
            return popup != null && popup.createPreDragCondition() != null
        } else if (action == MOVE) {
            val itemView = if (host is AppWidgetResizeFrame) {
                host.getViewForAccessibility() ?: host
            } else {
                host
            }
            return beginAccessibleDrag(itemView, item, fromKeyboard)
        } else if (action == ADD_TO_WORKSPACE) {
            return addToWorkspace(item, accessibility = true, finishCallback = null)
        } else if (action == MOVE_TO_WORKSPACE) {
            return moveToWorkspace(item)
        } else if (action == RESIZE) {
            val itemView = if (host is AppWidgetResizeFrame) {
                host.getViewForAccessibility() ?: host
            } else {
                host
            }
            val info = item as? LauncherAppWidgetInfo ?: return false
            val actions = getSupportedResizeActions(itemView, info)
            val popup = PopupContainer.showForMenuItems(mContext, itemView, actions) ?: return false
            popup.requestFocus()
            popup.addOnCloseCallback {
                itemView.requestFocus()
                itemView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                itemView.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
                AbstractFloatingView.closeOpenViews(
                    mContext,
                    false,
                    AbstractFloatingView.TYPE_WIDGET_RESIZE_FRAME
                )
            }
            return true
        } else if (action == DEEP_SHORTCUTS) {
            val btv = if (host is BubbleTextView) {
                host
            } else {
                (host as? BubbleTextHolder)?.bubbleText
            }
            return btv != null && mContext.getPopupControllerForAppIcons().show(btv) != null
        } else if (action == CLOSE) {
            if (host is AppWidgetResizeFrame) {
                AbstractFloatingView.closeOpenViews(
                    mContext,
                    false,
                    AbstractFloatingView.TYPE_WIDGET_RESIZE_FRAME
                )
                return true
            }
        } else {
            val dropTargetBar = mContext.getDropTargetBar()
            if (dropTargetBar != null) {
                for (dropTarget in dropTargetBar.getDropTargets()) {
                    val dropTargetAction = dropTarget.getSupportedAccessibilityAction(item, host)
                    if (action == dropTargetAction) {
                        dropTarget.onAccessibilityDrop(host, item, action)
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun getSupportedResizeActions(host: View, info: LauncherAppWidgetInfo): List<PopupData> {
        val actions = mutableListOf<PopupData>()
        if (host is AppWidgetResizeFrame) {
            val childView = host.getViewForAccessibility()
            return if (childView != null) getSupportedResizeActions(childView, info) else actions
        }

        val widgetHostView = host as? LauncherAppWidgetHostView ?: return actions
        val providerInfo: AppWidgetProviderInfo = widgetHostView.appWidgetInfo ?: return actions

        val contentParent = (host.parent as? DragView)?.parent ?: host.parent

        val layout = if (contentParent is ShortcutAndWidgetContainer && contentParent.parent is CellLayout) {
            contentParent.parent as CellLayout
        } else {
            return actions
        }

        if ((providerInfo.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL) != 0) {
            if (layout.isRegionVacant(info.cellX + info.spanX, info.cellY, 1, info.spanY) ||
                layout.isRegionVacant(info.cellX - 1, info.cellY, 1, info.spanY)
            ) {
                actions.add(WidgetResizePopupDataSource.increaseWidthAction())
            }

            if (info.spanX > info.minSpanX && info.spanX > 1) {
                actions.add(WidgetResizePopupDataSource.decreaseWidthAction())
            }
        }

        if ((providerInfo.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL) != 0) {
            if (layout.isRegionVacant(info.cellX, info.cellY + info.spanY, info.spanX, 1) ||
                layout.isRegionVacant(info.cellX, info.cellY - 1, info.spanX, 1)
            ) {
                actions.add(WidgetResizePopupDataSource.increaseHeightAction())
            }

            if (info.spanY > info.minSpanY && info.spanY > 1) {
                actions.add(WidgetResizePopupDataSource.decreaseHeightAction())
            }
        }

        return actions
    }

    fun announceConfirmation(resId: Int) {
        announceConfirmation(mContext.getString(resId))
    }

    override fun beginAccessibleDrag(item: View, info: ItemInfo, fromKeyboard: Boolean): Boolean {
        if (!itemSupportsAccessibleDrag(info)) {
            return false
        }

        val dragInfo = DragInfo().apply {
            this.info = info
            this.item = item
            this.dragType = when {
                info is FolderInfo -> DragType.FOLDER
                info is AppPairInfo -> DragType.APP_PAIR
                info is LauncherAppWidgetInfo -> DragType.WIDGET
                info.itemType == LauncherSettings.Favorites.ITEM_TYPE_FILE_SYSTEM_FILE ||
                    info.itemType == LauncherSettings.Favorites.ITEM_TYPE_FILE_SYSTEM_FOLDER -> DragType.FILESYSTEM_ICON
                else -> DragType.ICON
            }
        }
        mDragInfo = dragInfo

        val pos = Rect()
        mContext.getDragLayer()?.getDescendantRectRelativeToSelf(item, pos)
        mContext.getDragController()?.addDragListener(this)

        val options = DragOptions().apply {
            isAccessibleDrag = true
            isKeyboardDrag = fromKeyboard
            simulatedDndStartPoint = Point(pos.centerX(), pos.centerY())
        }

        if (fromKeyboard) {
            val popup = mContext.layoutInflater.inflate(
                R.layout.keyboard_drag_and_drop,
                mContext.getDragLayer(),
                false
            ) as KeyboardDragAndDropView
            popup.showForIcon(item, info, options)
        } else {
            ItemLongClickListener.beginDrag(item, mContext, info, options)
        }
        return true
    }

    /**
     * Find empty space on the workspace and returns the screenId.
     */
    protected open fun findSpaceOnWorkspace(info: ItemInfo, outCoordinates: kotlin.IntArray): Int {
        val workspace = mContext.getWorkspace() ?: return -1
        val workspaceScreens: IntArray = workspace.getScreenOrder()
        var screenId: Int

        // First check if there is space on the current screen.
        var screenIndex = workspace.getCurrentPage()
        screenId = workspaceScreens.get(screenIndex)
        var layout = workspace.getPageAt(screenIndex) as? CellLayout

        var found = layout?.findCellForSpan(outCoordinates, info.spanX, info.spanY) ?: false
        screenIndex = 0
        while (!found && screenIndex < workspaceScreens.size()) {
            screenId = workspaceScreens.get(screenIndex)
            layout = workspace.getPageAt(screenIndex) as? CellLayout
            found = layout?.findCellForSpan(outCoordinates, info.spanX, info.spanY) ?: false
            screenIndex++
        }

        if (found) {
            return screenId
        }

        workspace.addExtraEmptyScreens()
        val emptyScreenIds = workspace.commitExtraEmptyScreens()
        if (emptyScreenIds.isEmpty) {
            return -1
        }

        screenId = emptyScreenIds.array.get(0)
        layout = workspace.getScreenWithId(screenId)
        found = layout?.findCellForSpan(outCoordinates, info.spanX, info.spanY) ?: false

        if (!found) {
            Log.wtf(TAG, "Not enough space on an empty screen")
        }
        return screenId
    }

    /**
     * Functionality to add the item [ItemInfo] to the workspace.
     */
    open fun addToWorkspace(
        item: ItemInfo,
        accessibility: Boolean = false,
        finishCallback: Consumer<Boolean>? = null
    ): Boolean {
        AbstractFloatingView.closeOpenViews(
            mContext,
            false,
            AbstractFloatingView.TYPE_WIDGET_RESIZE_FRAME
        )

        val coordinates = kotlin.IntArray(2)
        val screenId = findSpaceOnWorkspace(item, coordinates)
        val layout = mContext.getWorkspace()?.getScreenWithId(screenId)
        if (screenId == -1 || layout == null) {
            finishCallback?.accept(false)
            return false
        }
        layout.isDropPending = true
        val wrappedDropCallback = Consumer<Boolean> { success ->
            layout.isDropPending = false
            finishCallback?.accept(success)
        }

        val itemBindLogic = Runnable {
            when (item) {
                is WorkspaceItemFactory -> {
                    val info = item.makeWorkspaceItem(mContext)
                    info.checkAndApplyAutomationFlag(AutomationRepository.INSTANCE.get(mContext))
                    mContext.getModelWriter()?.addItemToDatabase(
                        info,
                        LauncherSettings.Favorites.CONTAINER_DESKTOP,
                        screenId,
                        coordinates[0],
                        coordinates[1]
                    )
                    bindItem(info, accessibility, wrappedDropCallback)
                }
                is PendingAddItemInfo -> {
                    if (item is PendingAddWidgetInfo && item.bindOptions == null) {
                        item.bindOptions = item.getDefaultSizeOptions(mContext)
                    }
                    mContext.addPendingItem(
                        item,
                        LauncherSettings.Favorites.CONTAINER_DESKTOP,
                        screenId,
                        coordinates,
                        item.spanX,
                        item.spanY
                    )
                    wrappedDropCallback.accept(true)
                }
                is WorkspaceItemInfo -> {
                    val info = item.clone()
                    info.checkAndApplyAutomationFlag(AutomationRepository.INSTANCE.get(mContext))
                    mContext.getModelWriter()?.addItemToDatabase(
                        info,
                        LauncherSettings.Favorites.CONTAINER_DESKTOP,
                        screenId,
                        coordinates[0],
                        coordinates[1]
                    )
                    bindItem(info, accessibility, wrappedDropCallback)
                }
                is CollectionInfo -> {
                    mContext.getModelWriter()?.addItemToDatabase(
                        item,
                        LauncherSettings.Favorites.CONTAINER_DESKTOP,
                        screenId,
                        coordinates[0],
                        coordinates[1]
                    )
                    val automationRepo = AutomationRepository.INSTANCE.get(mContext)
                    item.getContents().forEach { member ->
                        if (member is ItemInfoWithIcon) {
                            member.checkAndApplyAutomationFlag(automationRepo)
                        }
                        mContext.getModelWriter()?.addItemToDatabase(member, item.id, -1, -1, -1)
                    }
                    bindItem(item, accessibility, wrappedDropCallback)
                }
                else -> {}
            }
        }

        val workspace = mContext.getWorkspace() ?: return false
        mContext.getStateManager().goToState(
            LauncherState.NORMAL,
            true,
            AnimatorListeners.forSuccessCallback {
                val pageIndex = workspace.getPageIndexForScreenId(screenId)
                workspace.post {
                    if (workspace.getCurrentPage() == pageIndex) {
                        itemBindLogic.run()
                    } else {
                        workspace.snapToPage(pageIndex)
                        workspace.setOnPageTransitionEndCallback(itemBindLogic)
                    }
                }
            }
        )
        return true
    }

    private fun bindItem(
        item: ItemInfo,
        focusForAccessibility: Boolean,
        finishCallback: Consumer<Boolean>?
    ) {
        val anim = AnimatorSet()
        anim.addListener(AnimatorListeners.forEndCallback { success ->
            if (finishCallback != null) {
                finishCallback.accept(success)
            }
        })
        val view = View(mContext).apply { tag = item }
        if (focusForAccessibility) {
            view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
        mContext.bindInflatedItems(listOf(Pair.create(item, view)), anim)
    }

    /**
     * Functionality to move the item [ItemInfo] to the workspace.
     */
    open fun moveToWorkspace(item: ItemInfo): Boolean {
        val folder = Folder.getOpen(mContext)
        folder?.close(true)
        val info = item as? WorkspaceItemInfo ?: return false
        folder?.removeFolderContent(false, info)

        val coordinates = kotlin.IntArray(2)
        val screenId = findSpaceOnWorkspace(item, coordinates)
        if (screenId == -1) {
            return false
        }
        mContext.getModelWriter()?.moveItemInDatabase(
            info,
            LauncherSettings.Favorites.CONTAINER_DESKTOP,
            screenId,
            coordinates[0],
            coordinates[1]
        )

        Handler(Looper.getMainLooper()).post {
            mContext.inflateAndBindItemWithAnimation(item)
            announceConfirmation(R.string.item_moved)
        }
        return true
    }

    companion object {
        private const val TAG = "LauncherAccessibilityDelegate"

        val REMOVE: Int = R.id.action_remove
        val UNINSTALL: Int = R.id.action_uninstall
        val DISMISS_PREDICTION: Int = R.id.action_dismiss_prediction
        val PIN_PREDICTION: Int = R.id.action_pin_prediction
        val RECONFIGURE: Int = R.id.action_reconfigure
        const val INVALID: Int = -1
        val ADD_TO_WORKSPACE: Int = R.id.action_add_to_workspace
        val MOVE: Int = R.id.action_move
        val MOVE_TO_WORKSPACE: Int = R.id.action_move_to_workspace
        val RESIZE: Int = R.id.action_resize
        val DEEP_SHORTCUTS: Int = R.id.action_deep_shortcuts
        val CLOSE: Int = R.id.action_close

        private fun isNotInShortcutMenu(view: View?): Boolean {
            return view == null || view.parent !is DeepShortcutView
        }

        @JvmStatic
        fun getSupportedActions(launcher: Launcher, host: View?): List<LauncherAction> {
            if (host == null || host.tag !is ItemInfo) {
                return emptyList()
            }
            val container = PopupContainer.getOpen(launcher)
            val delegate = (container?.accessibilityDelegate as? LauncherAccessibilityDelegate)
                ?: launcher.getAccessibilityDelegate()
                ?: return emptyList()

            val result = mutableListOf<LauncherAction>()
            delegate.getSupportedActions(host, host.tag as ItemInfo, result)
            return result
        }
    }
}
