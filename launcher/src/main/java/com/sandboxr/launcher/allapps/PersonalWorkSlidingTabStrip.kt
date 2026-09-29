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

package com.sandboxr.launcher.allapps

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt

/**
 * A sliding tab strip used for switching between Personal, Work, and optionally Private
 * profile tabs in the All Apps drawer.
 *
 * Design: Liquid Glass pill selector that slides between tab labels,
 * using a frosted-glass indicator track with specular highlight.
 */
class PersonalWorkSlidingTabStrip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), FloatingHeaderRow {

    companion object {
        const val TAB_PERSONAL = 0
        const val TAB_WORK = 1
        const val TAB_PRIVATE = 2

        private const val INDICATOR_ANIM_DURATION_MS = 250L
    }

    private var parent: FloatingHeaderView? = null
    private var onTabSelectedListener: OnTabSelectedListener? = null

    private var activeTabIndex: Int = TAB_PERSONAL
    private var indicatorLeft: Float = 0f
    private var indicatorRight: Float = 0f
    private var animatedIndicatorLeft: Float = 0f
    private var animatedIndicatorRight: Float = 0f

    private var indicatorAnimator: ValueAnimator? = null

    private val tabLabels = mutableListOf<TextView>()
    private val tabTexts = mutableListOf<String>()

    private val indicatorRect = RectF()

    // Paint for the pill indicator background
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A80A0FF") // Frosted glass blue tint
        style = Paint.Style.FILL
    }

    // Paint for the pill indicator border (specular glint)
    private val indicatorBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4D80A0FF")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * context.resources.displayMetrics.density
    }

    @ColorInt
    private val activeTextColor = Color.parseColor("#FFFFFF")

    @ColorInt
    private val inactiveTextColor = Color.parseColor("#99FFFFFF")

    private var isVisible: Boolean = true

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setWillNotDraw(false)
        clipToPadding = false
        val density = context.resources.displayMetrics.density
        val vertPad = (12 * density).toInt()
        val horizPad = (16 * density).toInt()
        setPadding(horizPad, vertPad, horizPad, vertPad)
    }

    fun interface OnTabSelectedListener {
        fun onTabSelected(tabIndex: Int)
    }

    fun setOnTabSelectedListener(listener: OnTabSelectedListener) {
        onTabSelectedListener = listener
    }

    /**
     * Configures the available tabs. [tabTitles] must be a non-empty list of labels.
     * This clears any previously added tabs.
     */
    fun setTabs(tabTitles: List<String>) {
        removeAllViews()
        tabLabels.clear()
        tabTexts.clear()
        tabTexts.addAll(tabTitles)

        val density = context.resources.displayMetrics.density

        tabTitles.forEachIndexed { index, title ->
            val tab = TextView(context).apply {
                text = title
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(if (index == activeTabIndex) activeTextColor else inactiveTextColor)
                isSingleLine = true
                layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    val tabVertPad = (8 * density).toInt()
                    setPadding(0, tabVertPad, 0, tabVertPad)
                }
                setOnClickListener { onTabClicked(index) }
            }
            addView(tab)
            tabLabels.add(tab)
        }
        invalidate()
    }

    private fun onTabClicked(index: Int) {
        if (index == activeTabIndex) return
        selectTab(index, animate = true)
        onTabSelectedListener?.onTabSelected(index)
    }

    fun selectTab(tabIndex: Int, animate: Boolean = false) {
        val clamped = tabIndex.coerceIn(0, tabLabels.size - 1)
        activeTabIndex = clamped

        tabLabels.forEachIndexed { i, label ->
            label.setTextColor(if (i == clamped) activeTextColor else inactiveTextColor)
        }

        updateIndicatorBounds(animate)
        parent?.setActiveTab(clamped)
    }

    private fun updateIndicatorBounds(animate: Boolean) {
        if (tabLabels.isEmpty()) return
        val tab = tabLabels.getOrNull(activeTabIndex) ?: return

        // Indicator fills the tab's bounds with a small horizontal inset
        val density = context.resources.displayMetrics.density
        val inset = (8 * density)
        val targetLeft = tab.left + inset
        val targetRight = tab.right - inset

        if (!isLaidOut || !animate) {
            animatedIndicatorLeft = targetLeft
            animatedIndicatorRight = targetRight
            indicatorLeft = targetLeft
            indicatorRight = targetRight
            invalidate()
            return
        }

        indicatorAnimator?.cancel()
        val startLeft = animatedIndicatorLeft
        val startRight = animatedIndicatorRight

        indicatorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = INDICATOR_ANIM_DURATION_MS
            addUpdateListener { anim ->
                val frac = anim.animatedFraction
                animatedIndicatorLeft = startLeft + frac * (targetLeft - startLeft)
                animatedIndicatorRight = startRight + frac * (targetRight - startRight)
                invalidate()
            }
            start()
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        // Recalculate indicator without animation after layout
        updateIndicatorBounds(animate = false)
    }

    override fun draw(canvas: Canvas) {
        // Draw indicator pill behind children
        if (tabLabels.isNotEmpty() && animatedIndicatorRight > animatedIndicatorLeft) {
            val density = context.resources.displayMetrics.density
            val radius = (height / 2f - 4 * density).coerceAtLeast(8 * density)
            indicatorRect.set(animatedIndicatorLeft, paddingTop.toFloat(), animatedIndicatorRight, (height - paddingBottom).toFloat())
            canvas.drawRoundRect(indicatorRect, radius, radius, indicatorPaint)
            canvas.drawRoundRect(indicatorRect, radius, radius, indicatorBorderPaint)
        }
        super.draw(canvas)
    }

    // ======================== FloatingHeaderRow ========================

    override fun setup(parent: FloatingHeaderView, rows: Array<FloatingHeaderRow>, tabsHidden: Boolean) {
        this.parent = parent
        isVisible = !tabsHidden
        visibility = if (tabsHidden) View.GONE else View.VISIBLE
    }

    override fun onScrollChanged(scrollY: Int, isHeaderVisible: Boolean) {
        // The tab strip doesn't change appearance on scroll
    }

    override fun isVisible(): Boolean = isVisible

    override fun getExpectedHeight(): Int = if (isVisible) measuredHeight else 0

    override fun asView(): View = this

    override fun setActiveTab(tabIndex: Int) {
        if (tabIndex != activeTabIndex) {
            selectTab(tabIndex, animate = true)
        }
    }

    override fun destroy() {
        indicatorAnimator?.cancel()
    }
}
