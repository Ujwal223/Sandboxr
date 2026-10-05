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
import androidx.annotation.VisibleForTesting
import com.sandboxr.launcher.Utilities

/**
 * One dimensional scroll/drag/swipe gesture detector (either HORIZONTAL or VERTICAL).
 */
class SingleAxisSwipeDetector : BaseSwipeDetector {

    interface Listener {
        fun onDragStart(start: Boolean, startDisplacement: Float)
        fun onDrag(displacement: Float): Boolean
        fun onDrag(displacement: Float, event: MotionEvent): Boolean = onDrag(displacement)
        fun onDrag(displacement: Float, orthogonalDisplacement: Float, ev: MotionEvent): Boolean =
            onDrag(displacement, ev)
        fun onDragEnd(velocity: Float)
    }

    abstract class Direction {
        abstract fun isPositive(displacement: Float): Boolean
        abstract fun isNegative(displacement: Float): Boolean
        abstract fun extractDirection(point: PointF): Float
        abstract fun extractOrthogonalDirection(point: PointF): Float
    }

    private val mDir: Direction
    private val mListener: Listener
    private var mScrollDirections: Int = 0
    private var mTouchSlopMultiplier: Float = 1f

    constructor(context: Context, l: Listener, dir: Direction) : super(
        context,
        ViewConfiguration.get(context),
        Utilities.isRtl(context.resources)
    ) {
        mListener = l
        mDir = dir
    }

    @VisibleForTesting
    constructor(
        context: Context,
        config: ViewConfiguration,
        l: Listener,
        dir: Direction,
        isRtl: Boolean
    ) : super(context, config, isRtl) {
        mListener = l
        mDir = dir
    }

    fun setTouchSlopMultiplier(touchSlopMultiplier: Float) {
        mTouchSlopMultiplier = touchSlopMultiplier
    }

    fun setDetectableScrollConditions(scrollDirectionFlags: Int, ignoreSlop: Boolean) {
        mScrollDirections = scrollDirectionFlags
        mIgnoreSlopWhenSettling = ignoreSlop
    }

    fun wasInitialTouchPositive(): Boolean {
        return mDir.isPositive(mDir.extractDirection(mSubtractDisplacement))
    }

    override fun shouldScrollStart(displacement: PointF): Boolean {
        val minDisplacement = Math.max(
            mTouchSlop * mTouchSlopMultiplier,
            Math.abs(mDir.extractOrthogonalDirection(displacement))
        )
        if (Math.abs(mDir.extractDirection(displacement)) < minDisplacement) {
            return false
        }

        val displacementComponent = mDir.extractDirection(displacement)
        return canScrollNegative(displacementComponent) || canScrollPositive(displacementComponent)
    }

    private fun canScrollNegative(displacement: Float): Boolean {
        return (mScrollDirections and DIRECTION_NEGATIVE) > 0 && mDir.isNegative(displacement)
    }

    private fun canScrollPositive(displacement: Float): Boolean {
        return (mScrollDirections and DIRECTION_POSITIVE) > 0 && mDir.isPositive(displacement)
    }

    override fun reportDragStartInternal(recatch: Boolean) {
        val startDisplacement = mDir.extractDirection(mSubtractDisplacement)
        mListener.onDragStart(!recatch, startDisplacement)
    }

    override fun reportDraggingInternal(displacement: PointF, event: MotionEvent) {
        mListener.onDrag(
            mDir.extractDirection(displacement),
            mDir.extractOrthogonalDirection(displacement),
            event
        )
    }

    override fun reportDragEndInternal(velocity: PointF) {
        val velocityComponent = mDir.extractDirection(velocity)
        mListener.onDragEnd(velocityComponent)
    }

    companion object {
        const val DIRECTION_POSITIVE: Int = 1 shl 0
        const val DIRECTION_NEGATIVE: Int = 1 shl 1
        const val DIRECTION_BOTH: Int = DIRECTION_NEGATIVE or DIRECTION_POSITIVE

        @JvmField
        val VERTICAL: Direction = object : Direction() {
            override fun isPositive(displacement: Float): Boolean = displacement < 0 // Up
            override fun isNegative(displacement: Float): Boolean = displacement > 0 // Down
            override fun extractDirection(point: PointF): Float = point.y
            override fun extractOrthogonalDirection(point: PointF): Float = point.x
            override fun toString(): String = "VERTICAL"
        }

        @JvmField
        val HORIZONTAL: Direction = object : Direction() {
            override fun isPositive(displacement: Float): Boolean = displacement > 0 // Right
            override fun isNegative(displacement: Float): Boolean = displacement < 0 // Left
            override fun extractDirection(point: PointF): Float = point.x
            override fun extractOrthogonalDirection(point: PointF): Float = point.y
            override fun toString(): String = "HORIZONTAL"
        }
    }
}
