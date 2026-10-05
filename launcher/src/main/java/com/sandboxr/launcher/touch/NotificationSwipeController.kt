/*
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

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.MotionEvent
import com.sandboxr.launcher.AbstractFloatingView
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.util.TouchController

/**
 * TouchController that listens for downward swipe gestures on the home workspace
 * and triggers notification shade expansion.
 */
class NotificationSwipeController(
    private val mLauncher: Launcher,
    private val onExpandAction: (() -> Unit)? = null
) : TouchController, SingleAxisSwipeDetector.Listener {

    private val mDetector: SingleAxisSwipeDetector =
        SingleAxisSwipeDetector(mLauncher, this, SingleAxisSwipeDetector.VERTICAL)

    private var mCanIntercept = false
    private var mActionTriggered = false

    override fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            mActionTriggered = false
            mCanIntercept = canInterceptTouch(ev)
            if (mCanIntercept) {
                // Downward swipe is DIRECTION_NEGATIVE in SingleAxisSwipeDetector.VERTICAL
                mDetector.setDetectableScrollConditions(SingleAxisSwipeDetector.DIRECTION_NEGATIVE, false)
            } else {
                mDetector.setDetectableScrollConditions(0, false)
            }
        }

        if (!mCanIntercept) {
            return false
        }

        mDetector.onTouchEvent(ev)
        return mDetector.isDraggingOrSettling()
    }

    override fun onControllerTouchEvent(ev: MotionEvent): Boolean {
        return mDetector.onTouchEvent(ev)
    }

    private fun canInterceptTouch(ev: MotionEvent): Boolean {
        if (AbstractFloatingView.getTopOpenView(mLauncher) != null) {
            return false
        }
        return mLauncher.isInState(LauncherState.NORMAL)
    }

    override fun onDragStart(start: Boolean, startDisplacement: Float) {
        mActionTriggered = false
    }

    override fun onDrag(displacement: Float): Boolean {
        // In SingleAxisSwipeDetector.VERTICAL, downward swipe has displacement > 0
        if (!mActionTriggered && displacement > TRIGGER_THRESHOLD_PX) {
            triggerExpandNotifications()
            mActionTriggered = true
        }
        return true
    }

    override fun onDragEnd(velocity: Float) {
        if (!mActionTriggered) {
            // Check if user flung downward (velocity > 0)
            if (velocity > 0.5f || mDetector.isFling(velocity)) {
                triggerExpandNotifications()
                mActionTriggered = true
            }
        }
        mDetector.finishedScrolling()
    }

    private fun triggerExpandNotifications() {
        if (onExpandAction != null) {
            onExpandAction.invoke()
        } else {
            expandNotifications(mLauncher)
        }
    }

    companion object {
        private const val TAG = "NotificationSwipeController"
        private const val TRIGGER_THRESHOLD_PX = 120f

        @JvmStatic
        fun expandNotifications(context: Context) {
            try {
                val statusBarService = context.getSystemService("statusbar")
                val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
                val expand = statusBarManagerClass.getMethod("expandNotificationsPanel")
                expand.invoke(statusBarService)
            } catch (e: Exception) {
                try {
                    val intent = Intent("android.intent.action.EXPAND_STATUS_BAR")
                    context.sendBroadcast(intent)
                } catch (ex: Exception) {
                    Log.w(TAG, "Failed to expand notification shade", ex)
                }
            }
        }
    }
}
