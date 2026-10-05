/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.sandboxr.launcher.touch

import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import com.sandboxr.launcher.CellLayout
import com.sandboxr.launcher.DeviceProfile
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.Workspace
import com.sandboxr.launcher.popup.WorkspaceLongPressOptions
import com.sandboxr.launcher.util.TouchUtil
import com.sandboxr.launcher.views.OptionsPopupView

/**
 * Helper class to handle touch on empty space in workspace and show options popup on long press.
 */
class WorkspaceTouchListener(
    private val mLauncher: Launcher,
    private val mWorkspace: Workspace<*>
) : GestureDetector.SimpleOnGestureListener(), OnTouchListener {

    private val mTempRect = Rect()
    private val mTouchDownPoint = PointF()
    private val mTouchSlop: Float = (2 * ViewConfiguration.get(mLauncher).scaledTouchSlop).toFloat()
    private val mGestureDetector: GestureDetector = GestureDetector(mWorkspace.context, this)

    private var mLongPressState = STATE_CANCELLED

    override fun onTouch(view: View, ev: MotionEvent): Boolean {
        mGestureDetector.onTouchEvent(ev)

        val action = ev.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            var handleLongPress = canHandleLongPress()

            if (handleLongPress) {
                val dp: DeviceProfile = mLauncher.getDeviceProfile()
                val dl = mLauncher.getDragLayer()
                val insets = dp.insets

                if (dl != null) {
                    mTempRect.set(insets.left, insets.top, dl.width - insets.right, dl.height - insets.bottom)
                    mTempRect.inset(dp.workspacePaddingLeftPx, dp.workspacePaddingTopPx)
                    handleLongPress = mTempRect.contains(ev.x.toInt(), ev.y.toInt())
                } else {
                    handleLongPress = false
                }
            }

            if (handleLongPress) {
                mLongPressState = STATE_REQUESTED
                mTouchDownPoint.set(ev.x, ev.y)
                if (TouchUtil.isMouseRightClickDownOrMove(ev)) {
                    maybeShowMenu()
                    return true
                }
            }

            mWorkspace.onTouchEvent(ev)
            return true
        }

        if (mLongPressState == STATE_PENDING_PARENT_INFORM) {
            ev.action = MotionEvent.ACTION_CANCEL
            mWorkspace.onTouchEvent(ev)
            ev.action = action
            mLongPressState = STATE_COMPLETED
        }

        val isInAllAppsBottomSheet = mLauncher.isInState(LauncherState.ALL_APPS)
        val result: Boolean

        if (mLongPressState == STATE_COMPLETED) {
            result = true
        } else if (mLongPressState == STATE_REQUESTED) {
            mWorkspace.onTouchEvent(ev)
            if (mWorkspace.isHandlingTouch()) {
                cancelLongPress()
            } else if (action == MotionEvent.ACTION_MOVE &&
                PointF.length(mTouchDownPoint.x - ev.x, mTouchDownPoint.y - ev.y) > mTouchSlop
            ) {
                cancelLongPress()
            }
            result = true
        } else {
            result = isInAllAppsBottomSheet && action != MotionEvent.ACTION_CANCEL && action != MotionEvent.ACTION_UP
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            if (!mWorkspace.isHandlingTouch()) {
                val currentPage = mWorkspace.getChildAt(mWorkspace.getCurrentPage()) as? CellLayout
                if (currentPage != null) {
                    mWorkspace.onWallpaperTap(ev)
                }
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            cancelLongPress()
        }

        if (action == MotionEvent.ACTION_UP && isInAllAppsBottomSheet) {
            mLauncher.getStateManager().goToState(LauncherState.NORMAL)
        }

        return result
    }

    private fun canHandleLongPress(): Boolean {
        return mLauncher.isInState(LauncherState.NORMAL)
    }

    private fun cancelLongPress() {
        mLongPressState = STATE_CANCELLED
    }

    override fun onLongPress(event: MotionEvent) {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            return
        }
        maybeShowMenu()
    }

    private fun maybeShowMenu() {
        if (mLongPressState == STATE_REQUESTED) {
            if (canHandleLongPress()) {
                mLongPressState = STATE_PENDING_PARENT_INFORM
                (mWorkspace.parent as? android.view.ViewGroup)?.requestDisallowInterceptTouchEvent(true)

                mWorkspace.performHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                )
                showWorkspaceOptions(mTouchDownPoint.x, mTouchDownPoint.y)
            } else {
                cancelLongPress()
            }
        }
    }

    private fun showWorkspaceOptions(x: Float, y: Float) {
        val options = WorkspaceLongPressOptions.getAllAsOptionItems(mLauncher)
        OptionsPopupView.show<Launcher>(mLauncher, RectF(x, y, x, y), options, true)
    }

    companion object {
        private const val STATE_CANCELLED = 0
        private const val STATE_REQUESTED = 1
        private const val STATE_PENDING_PARENT_INFORM = 2
        private const val STATE_COMPLETED = 3
    }
}
