/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.graphics.PointF
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.INVALID_POINTER_ID
import android.view.VelocityTracker
import android.view.ViewConfiguration
import androidx.annotation.VisibleForTesting
import com.sandboxr.launcher.MotionEventsUtils.isTrackpadMotionEvent
import com.sandboxr.launcher.R
import java.util.LinkedList
import java.util.Queue

/**
 * Scroll/drag/swipe gesture detector base class.
 *
 * Handles gesture detection, pointer tracking, state transitions (IDLE -> DRAGGING -> SETTLING),
 * and velocity computation.
 */
abstract class BaseSwipeDetector protected constructor(
    @JvmField protected val mContext: Context,
    config: ViewConfiguration,
    @JvmField protected val mIsRtl: Boolean
) {

    private val mReleaseVelocity: Float = mContext.resources
        .getDimensionPixelSize(R.dimen.base_swift_detector_fling_release_velocity).toFloat()

    private val mDownPos = PointF()
    private val mLastPos = PointF()
    @JvmField protected val mTouchSlop: Float = config.scaledTouchSlop.toFloat()
    @JvmField protected val mMaxVelocity: Float = config.scaledMaximumFlingVelocity.toFloat()
    private val mSetStateQueue: Queue<Runnable> = LinkedList()

    private var mActivePointerId = INVALID_POINTER_ID
    private var mVelocityTracker: VelocityTracker? = null
    private val mLastDisplacement = PointF()
    private val mDisplacement = PointF()
    @JvmField protected val mSubtractDisplacement = PointF()

    @VisibleForTesting
    @JvmField internal var mState = ScrollState.IDLE
    private var mIsSettingState = false
    @JvmField protected var mIsTrackpadGesture = false

    @JvmField protected var mIgnoreSlopWhenSettling = false

    enum class ScrollState {
        IDLE,
        DRAGGING, // onDragStart, onDrag
        SETTLING  // onDragEnd
    }

    val downX: Int
        get() = mDownPos.x.toInt()

    val downY: Int
        get() = mDownPos.y.toInt()

    fun isIdleState(): Boolean = mState == ScrollState.IDLE

    fun isSettlingState(): Boolean = mState == ScrollState.SETTLING

    fun isDraggingState(): Boolean = mState == ScrollState.DRAGGING

    fun isDraggingOrSettling(): Boolean =
        mState == ScrollState.DRAGGING || mState == ScrollState.SETTLING

    fun isTrackpadGesture(): Boolean = mIsTrackpadGesture

    fun finishedScrolling() {
        setState(ScrollState.IDLE)
    }

    fun isFling(velocity: Float): Boolean = Math.abs(velocity) > mReleaseVelocity

    open fun onTouchEvent(ev: MotionEvent): Boolean {
        val actionMasked = ev.actionMasked
        if (actionMasked == MotionEvent.ACTION_DOWN && mVelocityTracker != null) {
            mVelocityTracker?.clear()
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker?.addMovement(ev)

        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mActivePointerId = ev.getPointerId(0)
                mDownPos.set(ev.x, ev.y)
                mLastPos.set(mDownPos)
                mLastDisplacement.set(0f, 0f)
                mDisplacement.set(0f, 0f)
                mIsTrackpadGesture = isTrackpadMotionEvent(ev)
                if (mState == ScrollState.SETTLING && mIgnoreSlopWhenSettling) {
                    setState(ScrollState.DRAGGING)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val ptrIdx = ev.actionIndex
                val ptrId = ev.getPointerId(ptrIdx)
                if (ptrId == mActivePointerId) {
                    val newPointerIdx = if (ptrIdx == 0) 1 else 0
                    mDownPos.set(
                        ev.getX(newPointerIdx) - (mLastPos.x - mDownPos.x),
                        ev.getY(newPointerIdx) - (mLastPos.y - mDownPos.y)
                    )
                    mLastPos.set(ev.getX(newPointerIdx), ev.getY(newPointerIdx))
                    mActivePointerId = ev.getPointerId(newPointerIdx)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = ev.findPointerIndex(mActivePointerId)
                if (pointerIndex != INVALID_POINTER_ID) {
                    mDisplacement.set(
                        ev.getX(pointerIndex) - mDownPos.x,
                        ev.getY(pointerIndex) - mDownPos.y
                    )
                    if (mIsRtl) {
                        mDisplacement.x = -mDisplacement.x
                    }

                    if (mState != ScrollState.DRAGGING && shouldScrollStart(mDisplacement)) {
                        setState(ScrollState.DRAGGING)
                    }
                    if (mState == ScrollState.DRAGGING) {
                        reportDragging(ev)
                    }
                    mLastPos.set(ev.getX(pointerIndex), ev.getY(pointerIndex))
                }
            }

            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                if (mState == ScrollState.DRAGGING) {
                    setState(ScrollState.SETTLING)
                }
                mVelocityTracker?.recycle()
                mVelocityTracker = null
            }
        }
        return true
    }

    private fun setState(newState: ScrollState) {
        if (mIsSettingState) {
            mSetStateQueue.add(Runnable { setState(newState) })
            return
        }
        mIsSettingState = true

        if (DBG) {
            Log.d(TAG, "setState:$mState->$newState")
        }

        if (newState == ScrollState.DRAGGING) {
            initializeDragging()
            if (mState == ScrollState.IDLE) {
                reportDragStart(false /* recatch */)
            } else if (mState == ScrollState.SETTLING) {
                reportDragStart(true /* recatch */)
            }
        }
        if (newState == ScrollState.SETTLING) {
            reportDragEnd()
        }

        mState = newState
        mIsSettingState = false
        if (!mSetStateQueue.isEmpty()) {
            mSetStateQueue.remove().run()
        }
    }

    private fun initializeDragging() {
        if (mState == ScrollState.SETTLING && mIgnoreSlopWhenSettling) {
            mSubtractDisplacement.set(0f, 0f)
        } else {
            mSubtractDisplacement.x = if (mDisplacement.x > 0) mTouchSlop else -mTouchSlop
            mSubtractDisplacement.y = if (mDisplacement.y > 0) mTouchSlop else -mTouchSlop
        }
    }

    protected abstract fun shouldScrollStart(displacement: PointF): Boolean

    private fun reportDragStart(recatch: Boolean) {
        reportDragStartInternal(recatch)
        if (DBG) {
            Log.d(TAG, "onDragStart recatch:$recatch")
        }
    }

    protected abstract fun reportDragStartInternal(recatch: Boolean)

    private fun reportDragging(event: MotionEvent) {
        if (mDisplacement != mLastDisplacement) {
            mLastDisplacement.set(mDisplacement)
            sTempPoint.set(
                mDisplacement.x - mSubtractDisplacement.x,
                mDisplacement.y - mSubtractDisplacement.y
            )
            reportDraggingInternal(sTempPoint, event)
        }
    }

    protected abstract fun reportDraggingInternal(displacement: PointF, event: MotionEvent)

    private fun reportDragEnd() {
        val tracker = mVelocityTracker
        val velocity = PointF(0f, 0f)
        if (tracker != null) {
            tracker.computeCurrentVelocity(1000, mMaxVelocity)
            velocity.set(tracker.xVelocity / 1000f, tracker.yVelocity / 1000f)
        }
        if (mIsRtl) {
            velocity.x = -velocity.x
        }
        if (DBG) {
            Log.d(TAG, String.format("onScrollEnd disp=%s, velocity=%s", mDisplacement, velocity))
        }

        reportDragEndInternal(velocity)
    }

    protected abstract fun reportDragEndInternal(velocity: PointF)

    open fun dump(): String = "\tBaseSwipeDetector mState=$mState"

    companion object {
        private const val DBG = false
        private const val TAG = "BaseSwipeDetector"
        private const val ANIMATION_DURATION = 1200f
        private val sTempPoint = PointF()

        @JvmStatic
        fun calculateDuration(velocity: Float, progressNeeded: Float): Long {
            val velocityDivisor = Math.max(2f, Math.abs(0.5f * velocity))
            val travelDistance = Math.max(0.2f, progressNeeded)
            val duration = (Math.max(100f, ANIMATION_DURATION / velocityDivisor * travelDistance)).toLong()
            if (DBG) {
                Log.d(TAG, String.format("calculateDuration=%d, v=%f, d=%f", duration, velocity, progressNeeded))
            }
            return duration
        }
    }
}
