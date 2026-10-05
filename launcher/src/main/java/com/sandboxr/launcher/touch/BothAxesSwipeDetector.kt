/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.sandboxr.launcher.Utilities

/**
 * Two dimensional scroll/drag/swipe gesture detector that reports x and y displacement/velocity.
 */
class BothAxesSwipeDetector(
    context: Context,
    private val mListener: Listener
) : BaseSwipeDetector(
    context,
    ViewConfiguration.get(context),
    Utilities.isRtl(context.resources)
) {

    interface Listener {
        fun onDragStart(start: Boolean)
        fun onDrag(displacement: PointF, motionEvent: MotionEvent): Boolean
        fun onDragEnd(velocity: PointF)
    }

    private var mScrollDirections: Int = 0

    fun setDetectableScrollConditions(scrollDirectionFlags: Int, ignoreSlop: Boolean) {
        mScrollDirections = scrollDirectionFlags
        mIgnoreSlopWhenSettling = ignoreSlop
    }

    override fun shouldScrollStart(displacement: PointF): Boolean {
        val canScrollUp = (mScrollDirections and DIRECTION_UP) > 0 && displacement.y <= -mTouchSlop
        val canScrollRight = (mScrollDirections and DIRECTION_RIGHT) > 0 && displacement.x >= mTouchSlop
        val canScrollDown = (mScrollDirections and DIRECTION_DOWN) > 0 && displacement.y >= mTouchSlop
        val canScrollLeft = (mScrollDirections and DIRECTION_LEFT) > 0 && displacement.x <= -mTouchSlop
        return canScrollUp || canScrollRight || canScrollDown || canScrollLeft
    }

    override fun reportDragStartInternal(recatch: Boolean) {
        mListener.onDragStart(!recatch)
    }

    override fun reportDraggingInternal(displacement: PointF, event: MotionEvent) {
        mListener.onDrag(displacement, event)
    }

    override fun reportDragEndInternal(velocity: PointF) {
        mListener.onDragEnd(velocity)
    }

    companion object {
        const val DIRECTION_UP: Int = 1 shl 0
        const val DIRECTION_RIGHT: Int = 1 shl 1
        const val DIRECTION_DOWN: Int = 1 shl 2
        const val DIRECTION_LEFT: Int = 1 shl 3
    }
}
