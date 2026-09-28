/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import com.sandboxr.launcher.pageindicators.PageIndicator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Base paging engine for horizontally paginated views like Workspace.
 * Implements smooth scrolling, touch drag detection, velocity-based fling,
 * snap-to-page physics, overscroll clamping, and page indicator synchronization.
 */
open class PagedView<T : View> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    companion object {
        protected const val INVALID_PAGE: Int = -1
        protected const val TOUCH_STATE_REST: Int = 0
        protected const val TOUCH_STATE_SCROLLING: Int = 1
        protected const val PAGE_SNAP_ANIMATION_DURATION: Int = 300
    }

    protected var mCurrentPage: Int = 0
    protected var mNextPage: Int = INVALID_PAGE
    protected val mScroller: OverScroller = OverScroller(context)

    protected var mPageIndicator: T? = null
    protected var mPageSpacing: Int = 0

    private var mTouchState: Int = TOUCH_STATE_REST
    private var mLastMotionX: Float = 0f
    private var mLastMotionY: Float = 0f
    private var mDownMotionX: Float = 0f
    private var mTouchSlop: Int = 0
    private var mMinFlingVelocity: Int = 0
    private var mMaxFlingVelocity: Int = 0
    private var mVelocityTracker: VelocityTracker? = null

    init {
        isHapticFeedbackEnabled = false
        val vc = ViewConfiguration.get(context)
        mTouchSlop = vc.scaledTouchSlop
        mMinFlingVelocity = vc.scaledMinimumFlingVelocity
        mMaxFlingVelocity = vc.scaledMaximumFlingVelocity
    }

    open fun setPageIndicator(pageIndicator: T?) {
        mPageIndicator = pageIndicator
        updatePageIndicator()
    }

    open fun getPageIndicator(): T? = mPageIndicator

    open val pageCount: Int get() = childCount

    open fun getPageAt(index: Int): View? = if (index in 0 until childCount) getChildAt(index) else null

    open fun getCurrentPage(): Int = mCurrentPage

    open fun getNextPage(): Int = if (mNextPage != INVALID_PAGE) mNextPage else mCurrentPage

    open fun getPageSpacing(): Int = mPageSpacing

    open fun setPageSpacing(spacing: Int) {
        mPageSpacing = spacing
        requestLayout()
    }

    open fun setCurrentPage(page: Int) {
        if (!mScroller.isFinished) {
            mScroller.abortAnimation()
        }
        val count = pageCount
        mCurrentPage = page.coerceIn(0, max(0, count - 1))
        mNextPage = INVALID_PAGE
        val scrollX = getScrollForPage(mCurrentPage)
        scrollTo(scrollX, scrollY)
        updatePageIndicator()
        invalidate()
    }

    protected open fun updatePageIndicator() {
        (mPageIndicator as? PageIndicator)?.let { indicator ->
            indicator.setMarkersCount(pageCount)
            indicator.setActiveMarker(mCurrentPage)
            val maxScroll = max(0, getScrollForPage(max(0, pageCount - 1)))
            indicator.setScroll(scrollX, maxScroll)
        }
    }

    open fun getScrollForPage(page: Int): Int {
        val count = pageCount
        if (page !in 0 until count) return 0
        val child = getPageAt(page) ?: return 0
        return child.left - paddingLeft
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.currX, mScroller.currY)
            val maxScroll = max(0, getScrollForPage(max(0, pageCount - 1)))
            (mPageIndicator as? PageIndicator)?.setScroll(mScroller.currX, maxScroll)
            ViewCompat.postInvalidateOnAnimation(this)
        } else if (mNextPage != INVALID_PAGE) {
            mCurrentPage = mNextPage.coerceIn(0, max(0, pageCount - 1))
            mNextPage = INVALID_PAGE
            updatePageIndicator()
            onPageEndTransition()
        }
    }

    protected open fun onPageBeginTransition() {}

    protected open fun onPageEndTransition() {}

    open fun snapToPage(whichPage: Int, duration: Int = PAGE_SNAP_ANIMATION_DURATION): Boolean {
        val count = pageCount
        if (count == 0) return false
        val targetPage = whichPage.coerceIn(0, count - 1)
        mNextPage = targetPage
        onPageBeginTransition()

        val newX = getScrollForPage(targetPage)
        val delta = newX - scrollX
        mScroller.startScroll(scrollX, scrollY, delta, 0, duration)
        invalidate()
        return true
    }

    open fun snapToDestination() {
        snapToPage(getDestinationPage(), PAGE_SNAP_ANIMATION_DURATION)
    }

    open fun getDestinationPage(): Int {
        val count = pageCount
        if (count == 0) return 0

        val currentScroll = scrollX
        var closestPage = 0
        var minDiff = Int.MAX_VALUE

        for (i in 0 until count) {
            val pageScroll = getScrollForPage(i)
            val diff = abs(currentScroll - pageScroll)
            if (diff < minDiff) {
                minDiff = diff
                closestPage = i
            }
        }
        return closestPage
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (childCount <= 0) return super.onInterceptTouchEvent(ev)

        val action = ev.actionMasked
        if (action == MotionEvent.ACTION_MOVE && mTouchState != TOUCH_STATE_REST) {
            return true
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mDownMotionX = ev.x
                mLastMotionX = ev.x
                mLastMotionY = ev.y
                mTouchState = if (!mScroller.isFinished) TOUCH_STATE_SCROLLING else TOUCH_STATE_REST
            }
            MotionEvent.ACTION_MOVE -> {
                val x = ev.x
                val y = ev.y
                val xDiff = abs(x - mLastMotionX)
                val yDiff = abs(y - mLastMotionY)

                val xMoved = xDiff > mTouchSlop
                if (xMoved && xDiff > yDiff) {
                    mTouchState = TOUCH_STATE_SCROLLING
                    mLastMotionX = x
                    mLastMotionY = y
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                mTouchState = TOUCH_STATE_REST
            }
        }

        return mTouchState != TOUCH_STATE_REST
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (childCount <= 0) return super.onTouchEvent(ev)

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker?.addMovement(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!mScroller.isFinished) {
                    mScroller.abortAnimation()
                }
                mLastMotionX = ev.x
                mDownMotionX = ev.x
                mTouchState = TOUCH_STATE_SCROLLING
            }
            MotionEvent.ACTION_MOVE -> {
                if (mTouchState == TOUCH_STATE_SCROLLING) {
                    val deltaX = (mLastMotionX - ev.x).roundToInt()
                    mLastMotionX = ev.x

                    val maxScroll = max(0, getScrollForPage(max(0, pageCount - 1)))
                    val newScroll = (scrollX + deltaX).coerceIn(-width / 4, maxScroll + width / 4)
                    scrollTo(newScroll, scrollY)
                    (mPageIndicator as? PageIndicator)?.setScroll(newScroll, maxScroll)
                } else {
                    val x = ev.x
                    val xDiff = abs(x - mLastMotionX)
                    if (xDiff > mTouchSlop) {
                        mTouchState = TOUCH_STATE_SCROLLING
                        mLastMotionX = x
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (mTouchState == TOUCH_STATE_SCROLLING) {
                    val tracker = mVelocityTracker
                    tracker?.computeCurrentVelocity(1000, mMaxFlingVelocity.toFloat())
                    val velocityX = tracker?.xVelocity ?: 0f

                    val count = pageCount
                    if (abs(velocityX) > mMinFlingVelocity && count > 1) {
                        if (velocityX > 0 && mCurrentPage > 0) {
                            snapToPage(mCurrentPage - 1)
                        } else if (velocityX < 0 && mCurrentPage < count - 1) {
                            snapToPage(mCurrentPage + 1)
                        } else {
                            snapToDestination()
                        }
                    } else {
                        snapToDestination()
                    }

                    mTouchState = TOUCH_STATE_REST
                    mVelocityTracker?.recycle()
                    mVelocityTracker = null
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if (mTouchState == TOUCH_STATE_SCROLLING) {
                    snapToDestination()
                    mTouchState = TOUCH_STATE_REST
                    mVelocityTracker?.recycle()
                    mVelocityTracker = null
                }
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val childWidthSpec = MeasureSpec.makeMeasureSpec(width - paddingLeft - paddingRight, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(height - paddingTop - paddingBottom, MeasureSpec.EXACTLY)

        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                child.measure(childWidthSpec, childHeightSpec)
            }
        }

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        var childLeft = paddingLeft

        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight
                child.layout(childLeft, paddingTop, childLeft + childWidth, paddingTop + childHeight)
                childLeft += childWidth + mPageSpacing
            }
        }

        if (mCurrentPage in 0 until count) {
            val targetScroll = getScrollForPage(mCurrentPage)
            if (scrollX != targetScroll && mScroller.isFinished) {
                scrollTo(targetScroll, scrollY)
            }
        }
        updatePageIndicator()
    }
}
