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

package com.sandboxr.launcher.views

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Property
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo

/**
 * A [BubbleTextView] displaying predicted app suggestions in the hybrid hotseat dock or All Apps drawer.
 * Renders a subtle Liquid Glass glowing accent ring around the icon to indicate predictive AI ranking.
 */
class PredictedAppIcon @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BubbleTextView(context, attrs, defStyleAttr) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * resources.displayMetrics.density
        color = Color.parseColor("#704E80EE") // Liquid Glass cyan accent glow
    }

    private val iconBounds = RectF()
    private var ringScale: Float = 1.0f
    var isPredicted: Boolean = true
        private set

    companion object {
        val RING_SCALE_PROPERTY: Property<PredictedAppIcon, Float> =
            object : Property<PredictedAppIcon, Float>(Float::class.java, "ringScale") {
                override fun get(icon: PredictedAppIcon): Float = icon.ringScale
                override fun set(icon: PredictedAppIcon, value: Float) {
                    icon.ringScale = value
                    icon.invalidate()
                }
            }
    }

    init {
        display = DISPLAY_PREDICTION_ROW
    }

    fun applyFromWorkspaceItemWithAnimation(itemInfo: WorkspaceItemInfo, staggerIndex: Int): Boolean {
        applyFromWorkspaceItem(itemInfo)
        isPredicted = true
        ringScale = 0.5f
        val animator = ObjectAnimator.ofFloat(this, RING_SCALE_PROPERTY, 0.5f, 1.0f).apply {
            duration = 320
            startDelay = (staggerIndex * 40L).coerceAtLeast(0L)
        }
        animator.start()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isPredicted && ringScale > 0.01f) {
            val d = compoundDrawables[1] ?: return
            val iconLeft = (width - d.bounds.width()) / 2f
            val iconTop = paddingTop.toFloat()
            val iconRight = iconLeft + d.bounds.width()
            val iconBottom = iconTop + d.bounds.height()

            iconBounds.set(iconLeft, iconTop, iconRight, iconBottom)
            val ringRadius = (iconBounds.width() / 2f + 4f * resources.displayMetrics.density) * ringScale
            val centerX = iconBounds.centerX()
            val centerY = iconBounds.centerY()

            canvas.drawCircle(centerX, centerY, ringRadius, ringPaint)
        }
    }

    /**
     * Pins this predicted app permanently into a standard workspace item.
     */
    fun pin() {
        isPredicted = false
        invalidate()
    }
}
