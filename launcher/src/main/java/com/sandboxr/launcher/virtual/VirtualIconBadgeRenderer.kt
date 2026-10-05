/*
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

package com.sandboxr.launcher.virtual

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.ColorInt

/**
 * Utility for rendering environment color badges onto app icons across both
 * View-based (BubbleTextView) and Jetpack Compose launcher surfaces.
 *
 * Adheres strictly to DESIGN.md Section 5.2:
 * - 8dp to 12dp badge circle placed at the bottom-right corner of the icon.
 * - 1.5dp high-contrast outline border matching the dark glass theme (#0A0A0F)
 *   to ensure visual separation on any wallpaper or icon background.
 */
object VirtualIconBadgeRenderer {

    private val badgeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val badgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#0A0A0F")
    }

    private val badgeShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#40000000")
    }

    /**
     * Draws an environment badge dot on the specified canvas within the icon bounds.
     *
     * @param canvas Target canvas (e.g. inside BubbleTextView.onDraw)
     * @param iconBounds The rectangular bounds of the rendered app icon
     * @param badgeColor Int ARGB color of the target virtual environment
     * @param radius Badge circle radius in pixels (default calculated from icon bounds)
     */
    fun drawBadge(
        canvas: Canvas,
        iconBounds: Rect,
        @ColorInt badgeColor: Int,
        radius: Float = iconBounds.width() * 0.14f
    ) {
        if (badgeColor == 0) return

        val cx = iconBounds.right - radius * 0.85f
        val cy = iconBounds.bottom - radius * 0.85f

        // Draw subtle drop shadow for depth
        canvas.drawCircle(cx, cy + 2f, radius + 1.5f, badgeShadowPaint)

        // Draw protective background outline
        canvas.drawCircle(cx, cy, radius + 2f, badgeStrokePaint)

        // Draw vibrant environment color fill
        badgeFillPaint.color = badgeColor
        canvas.drawCircle(cx, cy, radius, badgeFillPaint)
    }

    /**
     * Creates a new [Bitmap] with the environment badge baked directly into the
     * bottom-right quadrant.
     */
    fun createBadgedBitmap(
        baseBitmap: Bitmap,
        @ColorInt badgeColor: Int
    ): Bitmap {
        if (badgeColor == 0) return baseBitmap

        val output = Bitmap.createBitmap(
            baseBitmap.width,
            baseBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        canvas.drawBitmap(baseBitmap, 0f, 0f, null)

        val iconBounds = Rect(0, 0, baseBitmap.width, baseBitmap.height)
        drawBadge(canvas, iconBounds, badgeColor)

        return output
    }
}
