/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.sandboxr.launcher.secondarydisplay

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.GridView
import com.sandboxr.launcher.AbstractFloatingView
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.DeviceProfile
import com.sandboxr.launcher.DropTarget
import com.sandboxr.launcher.R
import com.sandboxr.launcher.allapps.ActivityAllAppsContainerView
import com.sandboxr.launcher.config.FeatureFlags
import com.sandboxr.launcher.dragndrop.DragOptions
import com.sandboxr.launcher.dragndrop.DragView
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.popup.PopupContainer
import com.sandboxr.launcher.popup.PopupContainerWithArrow
import com.sandboxr.launcher.popup.PopupDataProvider
import com.sandboxr.launcher.popup.SystemShortcut
import com.sandboxr.launcher.touch.SingleAxisSwipeDetector
import com.sandboxr.launcher.util.ApiWrapper
import com.sandboxr.launcher.util.ShortcutUtil
import com.sandboxr.launcher.util.TouchController
import com.sandboxr.launcher.views.BaseDragLayer

/**
 * DragLayer for Secondary launcher.
 */
class SecondaryDragLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseDragLayer<SecondaryDisplayLauncher>(context, attrs) {

    private val mContainer: SecondaryDisplayLauncher
        get() = mActivity

    private var mAllAppsButton: View? = null
    private var mAppsView: ActivityAllAppsContainerView<SecondaryDisplayLauncher>? = null
    private var mWorkspace: GridView? = null
    private var mPinnedAppsAdapter: PinnedAppsAdapter? = null

    init {
        recreateControllers()
    }

    override fun recreateControllers() {
        super.recreateControllers()
        val statusBarController = ApiWrapper.INSTANCE.get(context)
            .createStatusBarTouchController(mContainer) { true }

        mControllers.add(SecondaryDisplayAllAppsTouchController())
        val dragCtrl = mContainer.getDragController()
        if (dragCtrl != null) {
            mControllers.add(dragCtrl)
        }
        if (statusBarController != null) {
            mControllers.add(statusBarController)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mAllAppsButton = findViewById(R.id.all_apps_button)
        mAppsView = findViewById(R.id.apps_view)
        mWorkspace = findViewById(R.id.workspace_grid)

        val appsView = mAppsView
        val workspace = mWorkspace
        if (appsView != null && workspace != null) {
            val adapter = PinnedAppsAdapter(
                mContainer,
                appsView.getAppsStore(),
                View.OnLongClickListener { onIconLongClicked(it) }
            )
            mPinnedAppsAdapter = adapter
            workspace.adapter = adapter
            workspace.numColumns = mContainer.getDeviceProfile().numColumns
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mPinnedAppsAdapter?.init()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mPinnedAppsAdapter?.destroy()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)

        val grid = mContainer.getDeviceProfile()
        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child === mAppsView) {
                val horizontalPadding = (2 * grid.workspacePaddingLeftPx) +
                        grid.workspacePaddingLeftPx +
                        grid.workspacePaddingRightPx
                val verticalPadding = grid.workspacePaddingTopPx +
                        grid.workspacePaddingBottomPx

                val allAppsProfile = grid.getAllAppsProfile()
                val maxWidth = grid.minCellWidthPx * allAppsProfile.numShownAllAppsColumns + horizontalPadding
                val appsWidth = Math.min(width - paddingLeft - paddingRight, maxWidth)

                val maxHeight = allAppsProfile.cellHeightPx * allAppsProfile.numShownAllAppsColumns + verticalPadding
                val appsHeight = Math.min(height - paddingTop - paddingBottom, maxHeight)

                mAppsView?.measure(
                    MeasureSpec.makeMeasureSpec(appsWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(appsHeight, MeasureSpec.EXACTLY)
                )
            } else if (child === mAllAppsButton) {
                val appsButtonSpec = MeasureSpec.makeMeasureSpec(
                    grid.iconSizePx, MeasureSpec.EXACTLY
                )
                mAllAppsButton?.measure(appsButtonSpec, appsButtonSpec)
            } else if (child === mWorkspace) {
                val edgeMargin = resources.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin)
                measureChildWithMargins(
                    mWorkspace, widthMeasureSpec, 0, heightMeasureSpec,
                    grid.iconSizePx + edgeMargin
                )
            } else {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            }
        }
    }

    val pinnedAppsAdapter: PinnedAppsAdapter?
        get() = mPinnedAppsAdapter

    fun onIconLongClicked(v: View): Boolean {
        if (v !is BubbleTextView) {
            return false
        }
        if (PopupContainer.getOpen(mContainer) != null) {
            v.clearFocus()
            return false
        }
        val item = v.tag as? ItemInfo ?: return false
        if (!ShortcutUtil.supportsShortcuts(item)) {
            return false
        }
        val popupDataProvider = mContainer.getPopupDataProvider()

        val systemShortcuts = mutableListOf<SystemShortcut<*>>()
        SystemShortcut.APP_INFO.getShortcut(mContainer, item, v)?.let { systemShortcuts.add(it) }

        val adapter = mPinnedAppsAdapter
        if (adapter != null && (!FeatureFlags.SECONDARY_DRAG_N_DROP_TO_PIN.get() || !mContainer.isAppDrawerShown())) {
            systemShortcuts.add(adapter.getSystemShortcut(item, v))
        }

        val deepShortcutCount = popupDataProvider?.getShortcutCountForItem(item) ?: 0
        val container = PopupContainerWithArrow.create<SecondaryDisplayLauncher>(
            mContainer,
            v,
            item,
            false
        )
        container.populateAndShowRows(deepShortcutCount, systemShortcuts)
        container.requestFocus()

        if (!FeatureFlags.SECONDARY_DRAG_N_DROP_TO_PIN.get() || !mContainer.isAppDrawerShown()) {
            return true
        }

        val options = DragOptions().apply {
            val allAppsProfile = mContainer.getDeviceProfile().getAllAppsProfile()
            val iconSize = mContainer.getDeviceProfile().iconSizePx.coerceAtLeast(1)
            intrinsicIconScaleFactor = allAppsProfile.cellHeightPx.toFloat() / iconSize
            preDragCondition = container.createPreDragCondition()
        }

        val appsView = mAppsView ?: return true
        mContainer.beginDragShared(v, appsView, options)
        return true
    }

    private inner class SecondaryDisplayAllAppsTouchController : TouchController {
        private val mSwipeDetector = SingleAxisSwipeDetector(
            context,
            object : SingleAxisSwipeDetector.Listener {
                override fun onDragStart(start: Boolean, startDisplacement: Float) {
                    val display = mAppsView?.display
                    if (display != null) {
                        mContainer.secondaryDisplayDelegate.openAllAppsForDisplay(display.displayId)
                    }
                }

                override fun onDrag(displacement: Float): Boolean = false

                override fun onDragEnd(velocity: Float) {}
            },
            SingleAxisSwipeDetector.VERTICAL
        ).apply {
            setDetectableScrollConditions(SingleAxisSwipeDetector.DIRECTION_POSITIVE, false)
        }

        override fun onControllerTouchEvent(ev: MotionEvent): Boolean {
            if (!usingTwoFingerSwipeOnConnectedDisplay(ev)) {
                return false
            }
            return mSwipeDetector.onTouchEvent(ev)
        }

        override fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean {
            if (usingTwoFingerSwipeOnConnectedDisplay(ev)) {
                return true
            }

            if (!mContainer.isAppDrawerShown()) {
                return false
            }

            if (AbstractFloatingView.getTopOpenView(mContainer) != null) {
                return false
            }

            val appsView = mAppsView
            if (ev.action == MotionEvent.ACTION_DOWN && appsView != null && !isEventOverView(appsView, ev)) {
                mContainer.showAppDrawer(false)
                return true
            }
            return false
        }

        private fun usingTwoFingerSwipeOnConnectedDisplay(ev: MotionEvent): Boolean {
            val isTwoFingerSwipe = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ev.classification == MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
            } else {
                false
            }
            return isTwoFingerSwipe && mContainer.secondaryDisplayDelegate.enableTaskbarConnectedDisplays()
        }
    }
}
