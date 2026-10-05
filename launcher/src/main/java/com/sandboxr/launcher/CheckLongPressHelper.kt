/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.sandboxr.launcher

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.sandboxr.launcher.util.TouchUtil

/**
 * Utility class to handle trigger long press or right click on a view with custom timeout and
 * stylus event.
 */
class CheckLongPressHelper @JvmOverloads constructor(
    private val mView: View,
    private val mListener: View.OnLongClickListener? = null
) {

    private val mSlop: Float = ViewConfiguration.get(mView.context).scaledTouchSlop.toFloat()
    private var mLongPressTimeoutFactor: Float = DEFAULT_LONG_PRESS_TIMEOUT_FACTOR

    private var mHasPerformedLongPress: Boolean = false
    private var mIsInMouseRightClick: Boolean = false
    private var mPendingCheckForLongPress: Runnable? = null

    /**
     * Handles the touch event on a view.
     */
    fun onTouchEvent(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancelLongPress()

                // Mouse right click should immediately trigger a long press
                if (TouchUtil.isMouseRightClickDownOrMove(ev)) {
                    mIsInMouseRightClick = true
                    triggerLongPress()
                    val handler: Handler? = mView.handler
                    if (handler != null) {
                        val actionUpEvent = MotionEvent.obtain(ev)
                        actionUpEvent.action = MotionEvent.ACTION_UP
                        handler.postAtFrontOfQueue {
                            mView.rootView.dispatchTouchEvent(actionUpEvent)
                            actionUpEvent.recycle()
                        }
                    }
                    return
                }

                postCheckForLongPress()
                if (isStylusButtonPressed(ev)) {
                    triggerLongPress()
                }
            }

            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                cancelLongPress()
            }

            MotionEvent.ACTION_MOVE -> {
                if (mIsInMouseRightClick || !isPointInView(mView, ev.x, ev.y, mSlop)) {
                    cancelLongPress()
                } else if (mPendingCheckForLongPress != null && isStylusButtonPressed(ev)) {
                    triggerLongPress()
                }
            }
        }
    }

    /**
     * Overrides the default long press timeout factor.
     */
    fun setLongPressTimeoutFactor(longPressTimeoutFactor: Float) {
        mLongPressTimeoutFactor = longPressTimeoutFactor
    }

    private var mFallbackHandler: Handler? = null

    private fun getHandler(): Handler {
        val h = mView.handler
        if (h != null) return h
        if (mFallbackHandler == null) {
            mFallbackHandler = Handler(mView.context.mainLooper ?: Looper.getMainLooper())
        }
        return mFallbackHandler!!
    }

    private fun postCheckForLongPress() {
        mHasPerformedLongPress = false

        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress = Runnable { triggerLongPress() }
        }
        val timeout = (ViewConfiguration.getLongPressTimeout() * mLongPressTimeoutFactor).toLong()
        getHandler().postDelayed(mPendingCheckForLongPress!!, timeout)
    }

    /**
     * Cancels any pending long press and right click.
     */
    fun cancelLongPress() {
        mIsInMouseRightClick = false
        mHasPerformedLongPress = false
        clearCallbacks()
    }

    /**
     * Returns true if long press has been performed in the current touch gesture.
     */
    fun hasPerformedLongPress(): Boolean = mHasPerformedLongPress

    private fun triggerLongPress() {
        val windowValid = if (mView.isAttachedToWindow) {
            mView.parent != null && mView.hasWindowFocus()
        } else {
            true
        }
        if (windowValid &&
            (!mView.isPressed || mListener != null) &&
            !mHasPerformedLongPress
        ) {
            val handled = if (mListener != null) {
                mListener.onLongClick(mView)
            } else {
                mView.performLongClick()
            }
            if (handled) {
                mView.cancelLongPress()
                mView.isPressed = false
                mHasPerformedLongPress = true
            }
            clearCallbacks()
        }
    }

    private fun clearCallbacks() {
        mPendingCheckForLongPress?.let {
            getHandler().removeCallbacks(it)
            mView.removeCallbacks(it)
            mPendingCheckForLongPress = null
        }
    }

    companion object {
        const val DEFAULT_LONG_PRESS_TIMEOUT_FACTOR: Float = 0.75f

        private fun isPointInView(v: View, x: Float, y: Float, slop: Float): Boolean {
            return x >= -slop && y >= -slop && x < (v.width + slop) && y < (v.height + slop)
        }

        private fun isStylusButtonPressed(event: MotionEvent): Boolean {
            return event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS &&
                    event.isButtonPressed(MotionEvent.BUTTON_SECONDARY)
        }
    }
}
