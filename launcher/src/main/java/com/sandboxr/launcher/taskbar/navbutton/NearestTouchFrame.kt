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

package com.sandboxr.launcher.taskbar.navbutton

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import java.util.ArrayList
import java.util.HashMap

/**
 * Redirects touch events that aren't handled by any child view to the nearest clickable child.
 */
class NearestTouchFrame @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val clickableChildren = ArrayList<View>()
    private val touchableRegions = HashMap<View, Rect>()
    private val tmpInt = IntArray(2)
    private val windowOffset = IntArray(2)
    private var touchingChild: View? = null
    var isActive: Boolean = true

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        clickableChildren.clear()
        touchableRegions.clear()
        findClickableChildren(this)
        getLocationInWindow(windowOffset)
        cacheChildLocations()
    }

    private fun findClickableChildren(group: ViewGroup) {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            if (child.isShown && (child.isClickable || child.isLongClickable)) {
                clickableChildren.add(child)
            } else if (child is ViewGroup) {
                findClickableChildren(child)
            }
        }
    }

    private fun cacheChildLocations() {
        for (child in clickableChildren) {
            val rect = Rect()
            child.getHitRect(rect)
            touchableRegions[child] = rect
        }
    }

    private fun findNearestChild(x: Int, y: Int): View? {
        var minDistance = Float.MAX_VALUE
        var nearestChild: View? = null

        for (child in clickableChildren) {
            val rect = touchableRegions[child] ?: continue
            val cx = rect.centerX()
            val cy = rect.centerY()
            val dist = Math.hypot((x - cx).toDouble(), (y - cy).toDouble()).toFloat()
            if (dist < minDistance) {
                minDistance = dist
                nearestChild = child
            }
        }
        return nearestChild
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isActive || clickableChildren.isEmpty()) {
            return super.onTouchEvent(event)
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchingChild = findNearestChild(event.x.toInt(), event.y.toInt())
                touchingChild?.let {
                    event.offsetLocation(-it.left.toFloat(), -it.top.toFloat())
                    val handled = it.dispatchTouchEvent(event)
                    event.offsetLocation(it.left.toFloat(), it.top.toFloat())
                    return handled
                }
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchingChild?.let {
                    event.offsetLocation(-it.left.toFloat(), -it.top.toFloat())
                    val handled = it.dispatchTouchEvent(event)
                    event.offsetLocation(it.left.toFloat(), it.top.toFloat())
                    if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                        touchingChild = null
                    }
                    return handled
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
