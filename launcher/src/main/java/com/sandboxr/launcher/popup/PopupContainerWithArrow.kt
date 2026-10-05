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

package com.sandboxr.launcher.popup

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.graphics.Typeface
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.DragSource
import com.sandboxr.launcher.Flags
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.R
import com.sandboxr.launcher.Utilities
import com.sandboxr.launcher.dragndrop.DragController
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.ItemInfoWithIcon
import com.sandboxr.launcher.popup.ui.PopupItem
import com.sandboxr.launcher.shortcuts.DeepShortcutTextView
import com.sandboxr.launcher.shortcuts.DeepShortcutView
import com.sandboxr.launcher.util.Executors
import com.sandboxr.launcher.util.ShortcutUtil
import com.sandboxr.launcher.views.ActivityContext
import java.util.Optional
import java.util.stream.Collectors
import kotlin.math.max

/**
 * A container for shortcuts to deep links associated with an app.
 *
 * @param <T> The activity on with the popup shows
 */
@SuppressLint("ViewConstructor")
class PopupContainerWithArrow<T : ActivityContext>
private constructor(
    context: Context,
    val originalIcon: View,
    itemInfo: ItemInfo,
    updateIconUi: Boolean,
) :
    PopupContainer<T>(context, originalIcon, itemInfo, updateIconUi) {

    private val deepShortcuts: MutableList<DeepShortcutView> = ArrayList()
    private val interceptTouchDown = PointF()
    private val shortcutHeight: Float =
        resources.getDimension(R.dimen.system_shortcut_header_height)

    val itemClickListener: OnClickListener
        get() = OnClickListener { view: View? ->
            mActivityContext?.itemOnClickListener?.onClick(view)
        }

    private var containerWidth: Int = resources.getDimensionPixelSize(R.dimen.bg_popup_item_width)
    private var deepShortcutContainer: ViewGroup? = null
    private var currentHeight = 0f

    var itemDragHandler: PopupItemDragHandler? = null
        private set

    var widgetContainer: ViewGroup? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            interceptTouchDown[ev.x] = ev.y
        }
        // Stop sending touch events to deep shortcut views if user moved beyond touch slop.
        return (Utilities.squaredHypot(interceptTouchDown.x - ev.x, interceptTouchDown.y - ev.y) >
            Utilities.squaredTouchSlop(context))
    }

    fun setPopupItemDragHandler(popupItemDragHandler: PopupItemDragHandler?) {
        itemDragHandler = popupItemDragHandler
    }

    fun configureForLauncher(launcher: Launcher, itemInfo: ItemInfo) {
        addOnAttachStateChangeListener(
            LauncherPopupLiveUpdateHandler(launcher, this as PopupContainerWithArrow<Launcher>)
        )
        if (
            (itemInfo !is ItemInfoWithIcon) ||
                (itemInfo.runtimeStatusFlags and ItemInfoWithIcon.FLAG_NOT_PINNABLE) == 0
        ) {
            itemDragHandler = LauncherPopupItemDragHandler(launcher, this)
        }
        launcher.dragController?.addDragListener(this)
    }

    @CallSuper
    override fun showComposePopup(systemShortcuts: List<PopupItem>, deepShortcutCount: Int) {
        super.showComposePopup(systemShortcuts, deepShortcutCount)
        if (ShortcutUtil.supportsDeepShortcuts(itemInfo)) {
            loadAppShortcuts(itemInfo)
        }
    }

    override fun handleClose(animate: Boolean) {
        super.handleClose(animate)
        if (hasFocus() && originalView.isAttachedToWindow) {
            originalView.requestFocus()
        }
    }

    override fun requestFocusOnOpened(): Boolean {
        return true
    }

    /**
     * Populates and shows the popup container with only the provided system shortcuts.
     *
     * @param systemShortcuts List of system shortcuts to be displayed in the popup.
     */
    fun showSystemShortcuts(systemShortcuts: List<SystemShortcut<*>>) {
        if (systemShortcuts.isEmpty()) {
            return
        }
        containerWidth = resources.getDimensionPixelSize(R.dimen.bg_popup_item_width)
        addSystemShortcuts(
            systemShortcuts,
            R.layout.system_shortcut_rows_container,
            R.layout.system_shortcut,
        )
        show()
    }

    /**
     * Populate and show shortcuts for the Launcher U app shortcut design. Will inflate the
     * container and shortcut View instances for the popup container.
     *
     * @param deepShortcutCount Number of DeepShortcutView instances to add to container
     * @param systemShortcuts List of SystemShortcuts to add to container
     */
    fun populateAndShowRows(deepShortcutCount: Int, systemShortcuts: List<SystemShortcut<*>>) {
        populateAndShowRows(itemInfo, deepShortcutCount, systemShortcuts)
    }

    /**
     * Populate and show shortcuts for the Launcher U app shortcut design. Will inflate the
     * container and shortcut View instances for the popup container.
     *
     * @param itemInfo The info that is used to load app shortcuts
     * @param deepShortcutCount Number of DeepShortcutView instances to add to container
     * @param systemShortcuts List of SystemShortcuts to add to container
     */
    private fun populateAndShowRows(
        itemInfo: ItemInfo,
        deepShortcutCount: Int,
        systemShortcuts: List<SystemShortcut<*>>,
    ) {
        containerWidth = resources.getDimensionPixelSize(R.dimen.bg_popup_item_width)

        if (deepShortcutCount > 0) {
            addAllShortcuts(deepShortcutCount, systemShortcuts)
        } else if (systemShortcuts.isNotEmpty()) {
            addSystemShortcuts(
                systemShortcuts,
                R.layout.system_shortcut_rows_container,
                R.layout.system_shortcut,
            )
        }
        show()
        loadAppShortcuts(itemInfo)
    }

    /** Animates and loads shortcuts on background thread for this popup container */
    private fun loadAppShortcuts(originalItemInfo: ItemInfo) {
        accessibilityPaneTitle = context.getString(R.string.action_deep_shortcut)
        layoutTransition = LayoutTransition()

        if (Flags.expandableLongPressMenu()) {
            Executors.MODEL_EXECUTOR.handler.postAtFrontOfQueue(
                PopupPopulator.createUpdateRunnable(
                    context,
                    originalItemInfo,
                    Handler(context.mainLooper),
                    viewModel::onDeepShortcutsLoaded,
                )
            )
        } else {
            Executors.MODEL_EXECUTOR.handler.postAtFrontOfQueue(
                PopupPopulator.createUpdateRunnable(
                    mActivityContext,
                    originalItemInfo,
                    Handler(context.mainLooper),
                    this,
                    deepShortcuts,
                )
            )
        }
    }

    private fun addAllShortcuts(deepShortcutCount: Int, systemShortcuts: List<SystemShortcut<*>>) {
        if (deepShortcutCount + systemShortcuts.size <= SHORTCUT_COLLAPSE_THRESHOLD) {
            addSystemShortcuts(
                systemShortcuts,
                R.layout.system_shortcut_rows_container,
                R.layout.system_shortcut,
            )
            val startingHeight = ((shortcutHeight * systemShortcuts.size) + mChildContainerMargin)
            addDeepShortcuts(deepShortcutCount, startingHeight)
            return
        }

        currentHeight = shortcutHeight + mChildContainerMargin

        collapseEligibleSystemShortcutsIfOverThreshold(systemShortcuts)
        addDeepShortcuts(deepShortcutCount, currentHeight)
    }

    private fun collapseEligibleSystemShortcutsIfOverThreshold(
        systemShortcuts: List<SystemShortcut<*>>
    ) {
        val collapsibleSystemShortcuts = getCollapsibleSystemShortcuts(systemShortcuts)
        addSystemShortcutsIconsOnly(collapsibleSystemShortcuts)
        containerWidth =
            max(
                containerWidth.toDouble(),
                (collapsibleSystemShortcuts.size *
                        resources.getDimensionPixelSize(
                            R.dimen.system_shortcut_header_icon_touch_size
                        ))
                    .toDouble(),
            )
            .toInt()
        val nonCollapsibleSystemShortcuts =
            systemShortcuts
                .stream()
                .filter { shortcut: SystemShortcut<*> -> !shortcut.mIsCollapsible }
                .toList()
        if (nonCollapsibleSystemShortcuts.isNotEmpty()) {
            addSystemShortcuts(
                nonCollapsibleSystemShortcuts,
                R.layout.system_shortcut_rows_container,
                R.layout.system_shortcut,
            )
            currentHeight +=
                ((shortcutHeight * nonCollapsibleSystemShortcuts.size) + mChildContainerMargin)
        }
    }

    private fun addSystemShortcuts(
        systemShortcuts: List<SystemShortcut<*>>,
        @LayoutRes systemShortcutContainerLayout: Int,
        @LayoutRes systemShortcutLayout: Int,
    ) {
        if (systemShortcuts.isEmpty()) {
            return
        }
        systemShortcutContainer = inflateAndAdd(systemShortcutContainerLayout, this)
        widgetContainer = systemShortcutContainer
        for (i in systemShortcuts.indices) {
            initializeSystemShortcut(
                systemShortcutLayout,
                systemShortcutContainer,
                systemShortcuts[i],
                i < systemShortcuts.size - 1,
            )
        }
    }

    private fun addSystemShortcutsIconsOnly(systemShortcuts: List<SystemShortcut<*>>) {
        if (systemShortcuts.isEmpty()) {
            return
        }

        systemShortcutContainer = inflateAndAdd(R.layout.system_shortcut_icons_container, this)

        for (i in systemShortcuts.indices) {
            @LayoutRes var shortcutIconLayout = R.layout.system_shortcut_icon_only
            var shouldAppendSpacer = true

            if (i == 0) {
                shortcutIconLayout = R.layout.system_shortcut_icon_only_start
            } else if (i == systemShortcuts.size - 1) {
                shortcutIconLayout = R.layout.system_shortcut_icon_only_end
                shouldAppendSpacer = false
            }
            initializeSystemShortcut(
                shortcutIconLayout,
                systemShortcutContainer,
                systemShortcuts[i],
                shouldAppendSpacer,
            )
        }
    }

    private fun addDeepShortcuts(deepShortcutCount: Int, startingHeight: Float) {
        var height = startingHeight
        deepShortcutContainer = inflateAndAdd(R.layout.deep_shortcut_container, this)
        for (i in deepShortcutCount downTo 1) {
            height += shortcutHeight
            if (
                height >=
                    (mActivityContext?.deviceProfile?.deviceProperties?.availableHeightPx ?: 0)
            )
                break
            val v = inflateAndAdd<DeepShortcutView>(R.layout.deep_shortcut, deepShortcutContainer)
            v.layoutParams.width = containerWidth
            deepShortcuts.add(v)
        }
        updateHiddenShortcuts()
    }

    private fun updateHiddenShortcuts() {
        val total = deepShortcuts.size
        for (i in 0..<total) {
            val view = deepShortcuts[i]
            view.visibility = if (i >= PopupPopulator.MAX_SHORTCUTS) GONE else VISIBLE
        }
    }

    fun initializeWidgetShortcut(container: ViewGroup?, info: SystemShortcut<*>) {
        val view = initializeSystemShortcut(R.layout.system_shortcut, container, info, false)
        view.layoutParams.width = containerWidth
    }

    private fun initializeSystemShortcut(
        resId: Int,
        container: ViewGroup?,
        info: SystemShortcut<*>,
        shouldAppendSpacer: Boolean,
    ): View {
        val view = inflateAndAdd<View>(resId, container)
        if (view is DeepShortcutView) {
            val shortcutView = view
            if (com.android.wm.shell.Flags.enableGsf()) {
                shortcutView.bubbleText.typeface =
                    Typeface.create(
                        DeepShortcutTextView.GOOGLE_SANS_FLEX_LABEL_LARGE,
                        Typeface.NORMAL,
                    )
            }
            info.setIconAndLabelFor(shortcutView.iconView, shortcutView.bubbleText)
        } else if (view is ImageView) {
            info.setIconAndContentDescriptionFor(view)
            if (shouldAppendSpacer) inflateAndAdd<View>(R.layout.system_shortcut_spacer, container)
            view.setTooltipText(view.contentDescription)
        }
        view.tag = info
        view.setOnClickListener(info)
        return view
    }

    companion object {
        private const val SHORTCUT_COLLAPSE_THRESHOLD = 6

        @Deprecated("Left here since some dependent projects are using this method")
        fun canShow(icon: View?, item: ItemInfo?): Boolean {
            return icon is BubbleTextView && ShortcutUtil.supportsShortcuts(item)
        }

        @JvmStatic
        fun showForPrivateSpaceApp(icon: BubbleTextView) {
            val activityContext: ActivityContext = ActivityContext.lookupContext(icon.context)
            if (getOpen(ActivityContext.lookupContext(icon.context)) != null) {
                icon.clearFocus()
                return
            }
            val item = icon.tag as ItemInfo
            val deepShortcutCount =
                activityContext.getPopupDataProvider()?.getShortcutCountForItem(item) ?: 0

            val container =
                create<Launcher>(context = icon.context, originalView = icon, itemInfo = item)
            if (Flags.expandableLongPressMenu()) {
                container.showComposePopup(emptyList(), deepShortcutCount)
            } else {
                container.populateAndShowRows(deepShortcutCount, emptyList())
            }
            container.requestFocus()
        }

        private fun getWidgetShortcut(
            systemShortcuts: List<SystemShortcut<*>>
        ): Optional<SystemShortcut.Widgets<*>> {
            return systemShortcuts
                .stream()
                .filter { shortcut: SystemShortcut<*>? -> shortcut is SystemShortcut.Widgets<*> }
                .map { it as SystemShortcut.Widgets }
                .findFirst()
        }

        private fun getCollapsibleSystemShortcuts(
            systemShortcuts: List<SystemShortcut<*>>
        ): List<SystemShortcut<*>> {
            return systemShortcuts
                .stream()
                .filter { shortcut: SystemShortcut<*> -> shortcut.mIsCollapsible }
                .collect(Collectors.toList())
        }

        private fun getNonWidgetSystemShortcuts(
            systemShortcuts: List<SystemShortcut<*>>
        ): List<SystemShortcut<*>> {
            return systemShortcuts
                .stream()
                .filter { shortcut: SystemShortcut<*>? -> shortcut !is SystemShortcut.Widgets<*> }
                .collect(Collectors.toList())
        }

        @JvmStatic
        fun <T : ActivityContext> create(
            context: Context,
            originalView: View,
            itemInfo: ItemInfo,
            updateIconUi: Boolean = true,
        ): PopupContainerWithArrow<T> {
            val container =
                PopupContainerWithArrow<T>(context, originalView, itemInfo, updateIconUi)
            container.id = R.id.popup_container
            container.isFocusable = true
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
    }
}
