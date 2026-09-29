/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.sandboxr.launcher.allapps.BaseAllAppsAdapter.AdapterItem

/**
 * A [RecyclerView.ItemDecoration] that draws a visual separator and label above
 * the private apps section in the All Apps grid.
 *
 * The decorator identifies items tagged as [AdapterItem.VIEW_TYPE_PRIVATE_SPACE_HEADER]
 * and draws a pill-shaped "Private" label above them with the Liquid Glass aesthetic.
 */
class PrivateAppsSectionDecorator : RecyclerView.ItemDecoration() {

    companion object {
        private const val SECTION_HEADER_TOP_MARGIN_DP = 24f
        private const val PILL_HORIZONTAL_PADDING_DP = 16f
        private const val PILL_VERTICAL_PADDING_DP = 6f
        private const val LABEL_TEXT_SIZE_SP = 12f
    }

    // Pill background paint
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A80A0FF") // Very subtle frosted glass tint
        style = Paint.Style.FILL
    }

    // Pill border paint (specular glint)
    private val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33B0C8FF")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    // Text paint for the "Private" label
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99FFFFFF")
        textSize = LABEL_TEXT_SIZE_SP * 3f // will be converted from sp
        textAlign = Paint.Align.CENTER
    }

    // Separator line paint
    private val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1AFFFFFF")
        strokeWidth = 1f
    }

    private val pillRect = RectF()
    private var density: Float = 1f
    private var initialized = false

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (!initialized) {
            val ctx = parent.context
            density = ctx.resources.displayMetrics.density
            textPaint.textSize = LABEL_TEXT_SIZE_SP * ctx.resources.displayMetrics.scaledDensity
            initialized = true
        }

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val pos = parent.getChildAdapterPosition(child)
            if (pos == RecyclerView.NO_ID.toInt()) continue

            val adapter = parent.adapter as? BaseAllAppsAdapter ?: continue
            val viewType = adapter.getItemViewType(pos)

            if (viewType == BaseAllAppsAdapter.VIEW_TYPE_PRIVATE_SPACE_HEADER) {
                drawPrivateSectionHeader(canvas, child, parent)
            }
        }
    }

    private fun drawPrivateSectionHeader(canvas: Canvas, itemView: View, parent: RecyclerView) {
        val parentWidth = parent.width.toFloat()
        val topY = itemView.top.toFloat() - SECTION_HEADER_TOP_MARGIN_DP * density

        // Draw horizontal separator line
        val lineY = topY - 4 * density
        canvas.drawLine(
            parent.paddingLeft.toFloat() + 32 * density,
            lineY,
            parentWidth - parent.paddingRight - 32 * density,
            lineY,
            separatorPaint
        )

        // Draw "Private" label pill
        val label = "Private"
        val textWidth = textPaint.measureText(label)
        val pillHPad = PILL_HORIZONTAL_PADDING_DP * density
        val pillVPad = PILL_VERTICAL_PADDING_DP * density
        val pillWidth = textWidth + pillHPad * 2
        val pillHeight = textPaint.textSize + pillVPad * 2

        val pillLeft = (parentWidth - pillWidth) / 2f
        val pillTop = topY - pillHeight - 2 * density
        val pillRight = pillLeft + pillWidth
        val pillBottom = pillTop + pillHeight

        pillRect.set(pillLeft, pillTop, pillRight, pillBottom)
        val pillRadius = pillHeight / 2f

        canvas.drawRoundRect(pillRect, pillRadius, pillRadius, pillPaint)
        canvas.drawRoundRect(pillRect, pillRadius, pillRadius, pillBorderPaint)

        // Draw label text centered in pill
        val textY = pillBottom - pillVPad - textPaint.descent()
        canvas.drawText(label, parentWidth / 2f, textY, textPaint)
    }
}
