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

import android.animation.AnimatorSet
import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.os.Trace
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.Px
import androidx.compose.ui.platform.ComposeView
import com.sandboxr.launcher.AbstractFloatingView
import com.sandboxr.launcher.DragSource
import com.sandboxr.launcher.DropTarget.DragObject
import com.sandboxr.launcher.Flags
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherPrefs
import com.sandboxr.launcher.LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_VIEW
import com.sandboxr.launcher.R
import com.sandboxr.launcher.accessibility.LauncherAccessibilityDelegate
import com.sandboxr.launcher.dragndrop.DragController
import com.sandboxr.launcher.dragndrop.DragOptions
import com.sandboxr.launcher.folder.Folder
import com.android.launcher3.logging.StatsLogManager
import com.android.launcher3.logging.StatsLogManager.LauncherEvent
import com.android.launcher3.logging.StatsLogManager.LauncherEvent.IGNORE
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.ItemInfoWithIcon
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.popup.ui.ComposePopup
import com.sandboxr.launcher.popup.ui.DeepShortcutClickEvent
import com.sandboxr.launcher.popup.ui.PopupItem
import com.sandboxr.launcher.popup.ui.PopupViewModel
import com.sandboxr.launcher.popup.ui.SystemShortcutClickEvent
import com.sandboxr.launcher.shortcuts.DeepShortcutView
import com.sandboxr.launcher.util.ShortcutUtil
import com.sandboxr.launcher.views.ActivityContext
import com.sandboxr.launcher.views.BaseDragLayer

/**
 * Base popup container for shortcuts associated with the item {@code originalView}
 *
 * @param <T> The activity on which the popup shows
 * @param context The context in which the popup is created.
 * @param originalView The view from which this popup was opened.
 */
open class PopupContainer<T : ActivityContext>(
    context: Context?,
    val originalView: View,
    val itemInfo: ItemInfo,
    val updateIconUi: Boolean,
) : ArrowPopup<T>(context), DragSource, DragController.DragListener, Popup {
    var deepShortcutDragHandler: DeepShortcutDragHandler? = null
    var systemShortcutContainer: ViewGroup? = null

    val startDragThreshold =
        originalView.context.resources.getDimensionPixelSize(
            R.dimen.deep_shortcuts_start_drag_threshold
        )

    private val iconViewController: IconViewController? = originalView as? IconViewController

    val viewModel = PopupViewModel()

    private val animationProxyView by lazy { View(context).apply { visibility = INVISIBLE } }

    private var fixedDragLayerPos: Rect? = null

    private fun onDeepShortcutClick(item: WorkspaceItemInfo, iconBounds: Rect) {
        if (animationProxyView.parent == null) {
            popupContainer.addView(animationProxyView)
        }

        val dlPos = IntArray(2).also { popupContainer.getLocationInWindow(it) }
        animationProxyView.apply {
            tag = item
            layoutParams =
                (layoutParams as? BaseDragLayer.LayoutParams ?: BaseDragLayer.LayoutParams(0, 0))
                    .apply {
                        width = iconBounds.width()
                        height = iconBounds.height()
                        x = iconBounds.left - dlPos[0]
                        y = iconBounds.top - dlPos[1]
                        customPosition = true
                        ignoreInsets = true
                    }
        }
        mActivityContext.itemOnClickListener?.onClick(animationProxyView)
    }

    private fun canAddShortcut(): Boolean {
        val isPinnable =
            (originalView.tag as? ItemInfoWithIcon)?.let {
                (it.runtimeStatusFlags and ItemInfoWithIcon.FLAG_NOT_PINNABLE) == 0
            } == true
        return mActivityContext is Launcher && isPinnable
    }

    fun showForSystemShortcuts(
        systemShortcuts: List<PopupData>,
        activityContext: ActivityContext,
        itemView: View,
    ) {
        if (Flags.expandableLongPressMenu()) {
            showComposePopup(
                systemShortcuts =
                    systemShortcuts.map { popupData ->
                        PopupItem(
                            iconResId = popupData.iconResId,
                            labelResId = popupData.labelResId,
                            popupAction = {
                                popupData.popupAction.invoke(activityContext, itemInfo, itemView)
                            },
                            category = popupData.category,
                        )
                    }
            )
        } else {
            systemShortcutContainer = inflateAndAdd(R.layout.system_shortcut_rows_container, this)
            systemShortcuts.forEach { systemShortcut ->
                val view: DeepShortcutView =
                    inflateAndAdd(R.layout.system_shortcut, systemShortcutContainer)

                view.iconView.setBackgroundResource(systemShortcut.iconResId)
                view.bubbleText.setText(systemShortcut.labelResId)

                view.tag = systemShortcut
                view.setOnClickListener {
                    if (systemShortcut.eventId != IGNORE) {
                        activityContext.statsLogManager
                            .logger()
                            .withItemInfo(itemInfo)
                            .log(systemShortcut.eventId)
                    }
                    systemShortcut.popupAction.invoke(activityContext, itemInfo, itemView)
                }
            }
            show()
        }
    }

    open fun showComposePopup(systemShortcuts: List<PopupItem>, deepShortcutCount: Int = 0) {
        mElevation = 0f
        mIsOpen = true
        popupContainer.addView(this)
        visibility = GONE
        val lp = layoutParams
        lp.width = LayoutParams.WRAP_CONTENT
        lp.height = LayoutParams.WRAP_CONTENT
        layoutParams = lp

        val deviceProfile = mActivityContext.deviceProfile
        val availableHeightDp =
            deviceProfile.pxToDp(deviceProfile.deviceProperties.availableHeightPx.toFloat())
        viewModel.init(
            systemShortcuts,
            deepShortcutCount,
            availableHeightDp,
            LauncherPrefs.get(context),
        )

        val composePopup =
            ComposeView(context).apply {
                setContent {
                    ComposePopup(
                        viewModel = viewModel,
                        onClickListener = { clickedItem ->
                            when (clickedItem) {
                                is SystemShortcutClickEvent -> {
                                    clickedItem.item.popupAction()
                                }
                                is DeepShortcutClickEvent -> {
                                    (clickedItem.item as? WorkspaceItemInfo)?.let {
                                        onDeepShortcutClick(it, clickedItem.iconBounds)
                                    }
                                }
                            }
                            close(true)
                        },
                        onAddIconClick =
                            if (canAddShortcut()) { clickedItem ->
                                val accessibilityDelegate =
                                    mActivityContext?.accessibilityDelegate
                                if (accessibilityDelegate is LauncherAccessibilityDelegate) {
                                    accessibilityDelegate.addToWorkspace(
                                        /* itemInfo */ clickedItem,
                                        /* accessibility= */ false,
                                    )
                                    mActivityContext.statsLogManager
                                        .logger()
                                        .withItemInfo(clickedItem)
                                        .log(
                                            LauncherEvent.LAUNCHER_TAP_TO_ADD_DEEP_SHORTCUT
                                        )
                                }

                                // If we have an open folder, don't animate the popup closing.
                                val folder = getOpenView<Folder>(mActivityContext, TYPE_FOLDER)
                                close(folder == null)
                                folder?.close(true)
                            } else null,
                        onDeepShortcutLongPress = { itemInfoWithIcon, offset ->
                            val touchPoint = PointF(offset.x, offset.y)
                            deepShortcutDragHandler?.onDeepShortcutLongPress(
                                itemInfoWithIcon,
                                touchPoint,
                            )
                        },
                        onMaxHeightMeasured = { maxHeightPx -> positionAndShow(maxHeightPx) },
                    )
                }
            }

        addView(composePopup)
    }

    private fun positionAndShow(@Px maxHeightPx: Int) {
        orientAboutObject(maxHeightPx)
        assignMarginsAndBackgrounds(this)
        if (shouldAddArrow()) {
            addArrow()
        }
        animateOpen()
    }

    @CallSuper
    override fun handleClose(animate: Boolean) {
        Trace.beginAsyncSection("dismissPopupMenu", hashCode())
        super.handleClose(animate)
    }

    @CallSuper
    override fun closeComplete() {
        super.closeComplete()
        popupContainer.removeView(animationProxyView)
        mActivityContext?.dragController?.removeDragListener(this)
        val openPopup = getOpen(mActivityContext)
        if (openPopup == null || openPopup.originalView !== iconViewController) {
            iconViewController?.getFloatingViewTextAlpha()?.setValue(1f)
            iconViewController?.setForceHideDot(false)
        }
        logEvent(mActivityContext.statsLogManager, itemInfo.itemType, PopupEvent.CLOSE)
        Trace.endAsyncSection("dismissPopupMenu", hashCode())
    }

    @CallSuper
    override fun onCreateCloseAnimation(anim: AnimatorSet) {
        anim.play(iconViewController?.getFloatingViewTextAlpha()?.animateToValue(1f))
        iconViewController?.setForceHideDot(false)
    }

    override fun getAccessibilityInitialFocusView(): View {
        return systemShortcutContainer?.getChildAt(0) ?: super.getAccessibilityInitialFocusView()
    }

    @CallSuper
    override fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val dl = popupContainer
            if (!dl.isEventOverView(this, ev)) {
                close(true)
                return !dl.isEventOverView(originalView, ev)
            }
        }
        return false
    }

    @CallSuper
    override fun getTargetObjectLocation(outPos: Rect) {
        fixedDragLayerPos?.let {
            outPos.set(it)
            return
        }
        popupContainer.getDescendantRectRelativeToSelf(originalView, outPos)
        outPos.top += originalView.paddingTop
        outPos.left += originalView.paddingLeft
        outPos.right -= originalView.paddingRight
        val iconHeight = iconViewController?.getIconHeight()
        outPos.bottom =
            outPos.top +
                if (iconHeight != null && iconHeight > 0) iconHeight else originalView.height
    }

    override fun isOfType(type: Int): Boolean {
        return (type and AbstractFloatingView.TYPE_ACTION_POPUP) != 0
    }

    @CallSuper
    override fun onDragStart(dragObject: DragObject, options: DragOptions) {
        mDeferContainerRemoval = true
        handleClose(true)
    }

    override fun onDropCompleted(target: View?, d: DragObject, success: Boolean) {}

    @CallSuper
    override fun onDragEnd() {
        if (!isOpen) {
            if (mOpenCloseAnimator != null) {
                mDeferContainerRemoval = false
            } else {
                if (mDeferContainerRemoval) {
                    closeComplete()
                }
            }
        }
    }

    override fun createPreDragCondition(): DragOptions.PreDragCondition {
        return object : DragOptions.PreDragCondition {
            override fun shouldStartDrag(distanceDragged: Double): Boolean {
                return distanceDragged > startDragThreshold
            }

            override fun onPreDragStart(dragObject: DragObject) {
                if (iconViewController == null || !updateIconUi) return
                iconViewController.setForceHideDot(true)
                if (mIsAboveIcon) {
                    iconViewController.setIconVisible(false)
                    originalView.visibility = VISIBLE
                } else {
                    originalView.visibility = INVISIBLE
                }
            }

            override fun onPreDragEnd(dragObject: DragObject, dragStarted: Boolean) {
                if (iconViewController == null || !updateIconUi) return
                iconViewController.setIconVisible(true)
                if (dragStarted) {
                    originalView.visibility = INVISIBLE
                } else {
                    if (!mIsAboveIcon) {
                        originalView.visibility = VISIBLE
                        iconViewController.getFloatingViewTextAlpha()?.setValue(0f)
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun getOpen(context: ActivityContext): PopupContainer<*>? =
            getOpenView(context, TYPE_ACTION_POPUP)

        @JvmStatic
        fun dismissInvalidPopup(activity: ActivityContext) {
            val popup = getOpen(activity)
            val view = popup?.originalView ?: return
            if (!view.isAttachedToWindow || !ShortcutUtil.supportsShortcuts(popup.itemInfo)) {
                popup.handleClose(true)
            }
        }

        fun <T : ActivityContext> create(
            context: Context,
            originalView: View,
            itemInfo: ItemInfo,
            updateIconUi: Boolean = true,
        ): PopupContainer<T> {
            val container = PopupContainer<T>(context, originalView, itemInfo, updateIconUi)
            container.id = R.id.popup_container
            container.clipChildren = false
            container.clipToPadding = false
            container.orientation = VERTICAL
            container.layoutParams =
                LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            return container
        }

        @JvmOverloads
        fun showForMenuItems(
            activity: ActivityContext,
            view: View,
            items: List<PopupData>,
            popupPos: Rect? = null,
        ): PopupContainer<ActivityContext>? {
            if (items.isEmpty()) return null
            return create<ActivityContext>(
                    activity.asContext(),
                    view,
                    view.tag as? ItemInfo ?: ItemInfo().apply { itemType = ITEM_TYPE_CUSTOM_VIEW },
                )
                .apply {
                    fixedDragLayerPos = popupPos
                    showForSystemShortcuts(items, activity, view)
                }
        }
    }
}
