/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.sandboxr.launcher.settings

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Property
import android.view.View
import androidx.preference.Preference
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.sandboxr.launcher.util.Themes

/**
 * Utility class that highlights a specific preference row in a [RecyclerView].
 *
 * Implements [ItemDecoration] to draw a colour overlay on the target row and
 * [Runnable] so it can be posted with a delay via [View.postDelayed].
 *
 * The highlight sequence:
 * 1. Scrolls the list to the target position.
 * 2. Once scrolling stops, starts a fade-in/fade-out animation using the theme accent colour.
 * 3. After [HIGHLIGHT_DURATION] ms, fades the overlay to transparent and removes itself.
 */
class PreferenceHighlighter(
    private val mRv: RecyclerView,
    private val mIndex: Int,
    private val mPreference: Preference
) : ItemDecoration(), Runnable {

    private val mPaint = Paint()
    private val mDrawRect = RectF()

    private var mHighLightStarted = false
    private var mHighlightColor: Int = END_COLOR

    override fun run() {
        mRv.addItemDecoration(this)
        mRv.smoothScrollToPosition(mIndex)
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: State) {
        val holder: ViewHolder = parent.findViewHolderForAdapterPosition(mIndex) ?: return

        if (!mHighLightStarted && state.remainingScrollVertical != 0) {
            // Wait until the list has finished scrolling before starting the animation.
            return
        }

        if (!mHighLightStarted) {
            val colorTo = setColorAlphaBound(Themes.getColorAccent(mRv.context), HIGHLIGHT_ALPHA)
            val anim = ObjectAnimator.ofArgb(this, HIGHLIGHT_COLOR_PROPERTY, END_COLOR, colorTo)
            anim.duration = HIGHLIGHT_FADE_IN_DURATION
            anim.repeatMode = ValueAnimator.REVERSE
            anim.repeatCount = HIGHLIGHT_REPEAT_COUNT
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    removeHighlight()
                }
            })
            anim.start()
            mHighLightStarted = true
        }

        val view: View = holder.itemView
        mPaint.color = mHighlightColor
        mDrawRect.set(0f, view.y, parent.width.toFloat(), view.y + view.height)
        if (mPreference is HighlightDelegate) {
            mPreference.offsetHighlight(view, mDrawRect)
        }
        c.drawRect(mDrawRect, mPaint)
    }

    private fun removeHighlight() {
        val anim = ObjectAnimator.ofArgb(
            this, HIGHLIGHT_COLOR_PROPERTY, mHighlightColor, END_COLOR
        )
        anim.duration = HIGHLIGHT_FADE_OUT_DURATION
        anim.startDelay = HIGHLIGHT_DURATION
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mRv.removeItemDecoration(this@PreferenceHighlighter)
            }
        })
        anim.start()
    }

    /**
     * Interface that a [Preference] may implement to customise the highlight draw area.
     */
    interface HighlightDelegate {
        /**
         * Allows the preference to adjust the highlight bounds drawn over its item view.
         *
         * @param prefView The inflated preference item view.
         * @param bounds   The current highlight rect; modify in place to adjust the draw area.
         */
        fun offsetHighlight(prefView: View, bounds: RectF)
    }

    companion object {

        private const val HIGHLIGHT_DURATION = 15_000L
        private const val HIGHLIGHT_FADE_OUT_DURATION = 500L
        private const val HIGHLIGHT_FADE_IN_DURATION = 200L
        private const val HIGHLIGHT_REPEAT_COUNT = 4
        private const val HIGHLIGHT_ALPHA = 66

        private val END_COLOR: Int = setColorAlphaBound(Color.WHITE, 0)

        /** Sets the alpha channel of [color] to [alpha], clamped to [0, 255]. */
        @JvmStatic
        fun setColorAlphaBound(color: Int, alpha: Int): Int {
            val clamped = alpha.coerceIn(0, 255)
            return (color and 0x00FFFFFF) or (clamped shl 24)
        }

        /**
         * [Property] used by [ObjectAnimator] to animate [mHighlightColor] and trigger
         * RecyclerView invalidation on each frame.
         */
        private val HIGHLIGHT_COLOR_PROPERTY =
            object : Property<PreferenceHighlighter, Int>(Integer.TYPE, "highlightColor") {
                override fun get(h: PreferenceHighlighter): Int = h.mHighlightColor
                override fun set(h: PreferenceHighlighter, value: Int) {
                    h.mHighlightColor = value
                    h.mRv.invalidateItemDecorations()
                }
            }
    }
}
