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

package com.sandboxr.launcher.views

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.android.launcher3.Utilities
import com.sandboxr.launcher.InsettableFrameLayout
import com.sandboxr.launcher.util.TouchController
import java.util.ArrayList

/**
 * ViewGroup root for touch interception, drag-and-drop operations, and floating views.
 */
open class BaseDragLayer<T : ActivityContext> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : InsettableFrameLayout(context, attrs) {

    @Suppress("UNCHECKED_CAST")
    protected val mActivity: T by lazy {
        ActivityContext.lookupContext(context)
    }

    protected val mTmpXY = FloatArray(2)
    protected val mTmpRectPoints = FloatArray(4)
    protected val mHitRect = Rect()

    protected var mActiveController: TouchController? = null
    protected val mControllers = ArrayList<TouchController>()

    open class LayoutParams : InsettableFrameLayout.LayoutParams {
        @JvmField var x: Int = 0
        @JvmField var y: Int = 0
        @JvmField var customPosition: Boolean = false

        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs)
        constructor(width: Int, height: Int) : super(width, height)
        constructor(lp: ViewGroup.LayoutParams) : super(lp)
    }

    override fun generateLayoutParams(attrs: AttributeSet): FrameLayout.LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): FrameLayout.LayoutParams {
        return LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): ViewGroup.LayoutParams {
        return LayoutParams(p)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            val flp = child.layoutParams
            if (flp is LayoutParams && flp.customPosition) {
                child.layout(flp.x, flp.y, flp.x + flp.width, flp.y + flp.height)
            }
        }
    }

    fun getDescendantRectRelativeToSelf(descendant: View, r: Rect): Float {
        mTmpRectPoints[0] = 0f
        mTmpRectPoints[1] = 0f
        mTmpRectPoints[2] = descendant.width.toFloat()
        mTmpRectPoints[3] = descendant.height.toFloat()
        val s = getDescendantCoordRelativeToSelf(descendant, mTmpRectPoints)
        r.left = Math.round(minOf(mTmpRectPoints[0], mTmpRectPoints[2]))
        r.top = Math.round(minOf(mTmpRectPoints[1], mTmpRectPoints[3]))
        r.right = Math.round(maxOf(mTmpRectPoints[0], mTmpRectPoints[2]))
        r.bottom = Math.round(maxOf(mTmpRectPoints[1], mTmpRectPoints[3]))
        return s
    }

    fun getLocationInDragLayer(child: View, loc: IntArray): Float {
        loc[0] = 0
        loc[1] = 0
        return getDescendantCoordRelativeToSelf(child, loc)
    }

    fun getDescendantCoordRelativeToSelf(descendant: View, coord: IntArray): Float {
        mTmpXY[0] = coord[0].toFloat()
        mTmpXY[1] = coord[1].toFloat()
        val scale = getDescendantCoordRelativeToSelf(descendant, mTmpXY)
        Utilities.roundArray(mTmpXY, coord)
        return scale
    }

    fun getDescendantCoordRelativeToSelf(descendant: View, coord: FloatArray): Float {
        return getDescendantCoordRelativeToSelf(descendant, coord, false)
    }

    fun getDescendantCoordRelativeToSelf(
        descendant: View,
        coord: FloatArray,
        includeRootScroll: Boolean
    ): Float {
        return Utilities.getDescendantCoordRelativeToAncestor(descendant, this, coord, includeRootScroll)
    }

    fun mapRectInSelfToDescendant(descendant: View, rect: Rect) {
        Utilities.mapRectInSelfToDescendant(descendant, this, rect)
    }

    fun mapCoordInSelfToDescendant(descendant: View, coord: FloatArray) {
        Utilities.mapCoordInSelfToDescendant(descendant, this, coord)
    }

    fun mapCoordInSelfToDescendant(descendant: View, coord: IntArray) {
        mTmpXY[0] = coord[0].toFloat()
        mTmpXY[1] = coord[1].toFloat()
        Utilities.mapCoordInSelfToDescendant(descendant, this, mTmpXY)
        Utilities.roundArray(mTmpXY, coord)
    }

    fun getViewRectRelativeToSelf(v: View, r: Rect) {
        val loc = getViewLocationRelativeToSelf(v)
        r.set(loc[0], loc[1], loc[0] + v.measuredWidth, loc[1] + v.measuredHeight)
    }

    fun getViewLocationRelativeToSelf(v: View): IntArray {
        val loc = IntArray(2)
        getLocationInWindow(loc)
        val x = loc[0]
        val y = loc[1]
        v.getLocationInWindow(loc)
        loc[0] -= x
        loc[1] -= y
        return loc
    }

    fun isEventOverView(view: View, ev: MotionEvent): Boolean {
        getDescendantRectRelativeToSelf(view, mHitRect)
        return mHitRect.contains(ev.x.toInt(), ev.y.toInt())
    }

    open fun recreateControllers() {
        mControllers.clear()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            findActiveController(ev)
        }
        return mActiveController?.onControllerInterceptTouchEvent(ev) ?: false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return mActiveController?.onControllerTouchEvent(ev) ?: false
    }

    protected open fun findActiveController(ev: MotionEvent): Boolean {
        mActiveController = null
        for (controller in mControllers) {
            if (controller.onControllerInterceptTouchEvent(ev)) {
                mActiveController = controller
                return true
            }
        }
        return false
    }
}
