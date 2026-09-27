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
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

open class InsettableFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), Insettable {

    @JvmField
    protected var mInsets = Rect()

    open val insets: Rect
        get() = mInsets

    open fun setFrameLayoutChildInsets(child: View, newInsets: Rect, oldInsets: Rect) {
        val lp = child.layoutParams
        if (child is Insettable) {
            child.setInsets(newInsets)
        } else if (lp is LayoutParams && !lp.ignoreInsets) {
            lp.topMargin += (newInsets.top - oldInsets.top)
            lp.leftMargin += (newInsets.left - oldInsets.left)
            lp.rightMargin += (newInsets.right - oldInsets.right)
            lp.bottomMargin += (newInsets.bottom - oldInsets.bottom)
            child.layoutParams = lp
        }
    }

    override fun setInsets(insets: Rect) {
        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            setFrameLayoutChildInsets(child, insets, mInsets)
        }
        mInsets.set(insets)
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

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        if (isAttachedToWindow) {
            setFrameLayoutChildInsets(child, mInsets, Rect())
        }
    }

    class LayoutParams : FrameLayout.LayoutParams {
        @JvmField
        var ignoreInsets: Boolean = false

        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
            if (attrs != null) {
                val a = c.obtainStyledAttributes(attrs, R.styleable.InsettableFrameLayout_Layout)
                ignoreInsets = a.getBoolean(
                    R.styleable.InsettableFrameLayout_Layout_layout_ignoreInsets,
                    false
                )
                a.recycle()
            }
        }

        constructor(width: Int, height: Int) : super(width, height)

        constructor(lp: ViewGroup.LayoutParams) : super(lp)

        constructor(source: LayoutParams) : super(source) {
            ignoreInsets = source.ignoreInsets
        }
    }

    companion object {
        @JvmStatic
        fun dispatchInsets(parent: ViewGroup, insets: Rect) {
            val n = parent.childCount
            for (i in 0 until n) {
                val child = parent.getChildAt(i)
                if (child is Insettable) {
                    child.setInsets(insets)
                }
            }
        }
    }
}
