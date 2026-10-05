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

import android.content.res.Resources
import android.graphics.Rect
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.accessibility.AccessibilityEvent
import com.sandboxr.launcher.Utilities

open class DefaultPagedViewHandler : PagedOrientationHandler {

    override fun getPrimaryValue(x: Int, y: Int): Int = x

    override fun getSecondaryValue(x: Int, y: Int): Int = y

    override fun getPrimaryValue(x: Float, y: Float): Float = x

    override fun getSecondaryValue(x: Float, y: Float): Float = y

    override fun <T> setPrimary(target: T, action: PagedOrientationHandler.Int2DAction<T>, param: Int) {
        action.call(target, param, 0)
    }

    override fun <T> setPrimary(target: T, action: PagedOrientationHandler.Float2DAction<T>, param: Float) {
        action.call(target, param, 0f)
    }

    override fun getPrimaryDirection(event: MotionEvent, pointerIndex: Int): Float =
        event.getX(pointerIndex)

    override fun getPrimaryVelocity(velocityTracker: VelocityTracker, pointerId: Int): Float =
        velocityTracker.getXVelocity(pointerId)

    override fun getMeasuredSize(view: View): Int = view.measuredWidth

    override fun getPrimaryScroll(view: View): Int = view.scrollX

    override fun getPrimaryScale(view: View): Float = view.scaleX

    override fun setMaxScroll(event: AccessibilityEvent, maxScroll: Int) {
        event.maxScrollX = maxScroll
    }

    override fun getRecentsRtlSetting(resources: Resources): Boolean = !Utilities.isRtl(resources)

    override fun getChildStart(view: View): Int = view.left

    override fun getCenterForPage(view: View, insets: Rect): Int {
        return (view.paddingTop + view.measuredHeight + insets.top - insets.bottom - view.paddingBottom) / 2
    }

    override fun getScrollOffsetStart(view: View, insets: Rect): Int = insets.left + view.paddingLeft

    override fun getScrollOffsetEnd(view: View, insets: Rect): Int =
        view.width - view.paddingRight - insets.right

    override fun getChildBounds(
        child: View,
        childStart: Int,
        pageCenter: Int,
        layoutChild: Boolean
    ): PagedOrientationHandler.ChildBounds {
        val childWidth = child.measuredWidth
        val childRight = childStart + childWidth
        val childHeight = child.measuredHeight
        val childTop = pageCenter - childHeight / 2
        if (layoutChild) {
            child.layout(childStart, childTop, childRight, childTop + childHeight)
        }
        return PagedOrientationHandler.ChildBounds(childWidth, childHeight, childRight, childTop)
    }
}
