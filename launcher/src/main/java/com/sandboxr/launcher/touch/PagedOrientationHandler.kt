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

package com.sandboxr.launcher.touch

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.accessibility.AccessibilityEvent

/**
 * Abstraction layer to separate horizontal and vertical specific implementations for PagedView.
 */
interface PagedOrientationHandler {

    fun interface Int2DAction<T> {
        fun call(target: T, x: Int, y: Int)
    }

    fun interface Float2DAction<T> {
        fun call(target: T, x: Float, y: Float)
    }

    fun <T> setPrimary(target: T, action: Int2DAction<T>, param: Int)
    fun <T> setPrimary(target: T, action: Float2DAction<T>, param: Float)
    fun getPrimaryDirection(event: MotionEvent, pointerIndex: Int): Float
    fun getPrimaryVelocity(velocityTracker: VelocityTracker, pointerId: Int): Float
    fun getMeasuredSize(view: View): Int
    fun getPrimaryScroll(view: View): Int
    fun getPrimaryScale(view: View): Float
    fun getChildStart(view: View): Int
    fun getCenterForPage(view: View, insets: Rect): Int
    fun getScrollOffsetStart(view: View, insets: Rect): Int
    fun getScrollOffsetEnd(view: View, insets: Rect): Int
    fun getChildBounds(child: View, childStart: Int, pageCenter: Int, layoutChild: Boolean): ChildBounds
    fun setMaxScroll(event: AccessibilityEvent, maxScroll: Int)
    fun getRecentsRtlSetting(resources: Resources): Boolean

    fun getPrimaryValue(x: Int, y: Int): Int
    fun getSecondaryValue(x: Int, y: Int): Int

    fun getPrimaryValue(x: Float, y: Float): Float
    fun getSecondaryValue(x: Float, y: Float): Float

    class ChildBounds(
        @JvmField val primaryDimension: Int,
        @JvmField val secondaryDimension: Int,
        @JvmField val childPrimaryEnd: Int,
        @JvmField val childSecondaryEnd: Int
    )

    companion object {
        @JvmField
        val DEFAULT: PagedOrientationHandler = DefaultPagedViewHandler()

        @JvmField
        val VIEW_SCROLL_BY = Int2DAction<View> { target, x, y -> target.scrollBy(x, y) }

        @JvmField
        val VIEW_SCROLL_TO = Int2DAction<View> { target, x, y -> target.scrollTo(x, y) }

        @JvmField
        val CANVAS_TRANSLATE = Float2DAction<Canvas> { target, x, y -> target.translate(x, y) }

        @JvmField
        val MATRIX_POST_TRANSLATE = Float2DAction<Matrix> { target, x, y -> target.postTranslate(x, y) }
    }
}
