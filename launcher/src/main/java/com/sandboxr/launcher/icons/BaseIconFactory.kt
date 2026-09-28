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

package com.sandboxr.launcher.icons

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.annotation.ColorInt
import com.sandboxr.launcher.graphics.theme.IconThemer
import kotlin.math.max
import kotlin.math.min

/**
 * Core icon processing engine for Sandboxr Launcher.
 * Handles adaptive icon rendering, normalization scaling, shape masking,
 * badge overlays (work profile, cloned apps, private profile, virtual environment),
 * and dynamic Material You themed icons.
 */
open class BaseIconFactory(
    @JvmField protected val context: Context,
    @JvmField protected val fillResIconDpi: Int = 0,
    @JvmField val iconBitmapSize: Int = 100,
    @JvmField val shapeDetection: Boolean = false,
    @JvmField val themeController: Any? = null,
) : AutoCloseable {

    private val canvas = Canvas()
    private val oldBounds = Rect()

    protected val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    open fun createIconBitmap(iconBlob: ByteArray?): BitmapInfo {
        if (iconBlob == null || iconBlob.isEmpty()) {
            return BitmapInfo.LOW_RES_INFO
        }
        val bitmap = BitmapFactory.decodeByteArray(iconBlob, 0, iconBlob.size)
            ?: Bitmap.createBitmap(iconBitmapSize, iconBitmapSize, Bitmap.Config.ARGB_8888)
        return BitmapInfo(bitmap, extractDominantColor(bitmap))
    }

    open fun createIconBitmap(bitmap: Bitmap, isFullBleed: Boolean = false): BitmapInfo {
        val scaled = if (bitmap.width == iconBitmapSize && bitmap.height == iconBitmapSize) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, iconBitmapSize, iconBitmapSize, true)
        }
        return BitmapInfo(scaled, extractDominantColor(scaled))
    }

    open fun createBadgedIconBitmap(
        drawable: Drawable,
        user: UserHandle? = null,
        @ColorInt envColor: Int? = null,
        isThemed: Boolean = false,
    ): BitmapInfo {
        val outBitmap = Bitmap.createBitmap(iconBitmapSize, iconBitmapSize, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(outBitmap)

        oldBounds.set(drawable.bounds)

        var renderedDrawable = drawable
        if (isThemed) {
            renderedDrawable = IconThemer.applyThemedPalette(context, drawable)
        }

        if (renderedDrawable is AdaptiveIconDrawable) {
            renderAdaptiveIcon(canvas, renderedDrawable)
        } else {
            renderStandardDrawable(canvas, renderedDrawable)
        }

        // Draw badge overlays if applicable
        if (envColor != null) {
            drawEnvironmentBadge(canvas, envColor)
        } else if (user != null && isManagedProfile(user)) {
            drawWorkBadge(canvas)
        }

        canvas.setBitmap(null)
        drawable.bounds = oldBounds

        val color = extractDominantColor(outBitmap)
        val info = BitmapInfo(outBitmap, color)
        if (isThemed) {
            info.flags = info.flags or BitmapInfo.FLAG_THEMED
        }
        if (user != null && isManagedProfile(user)) {
            info.flags = info.flags or BitmapInfo.FLAG_WORK
        }
        return info
    }

    open fun createShapedIconBitmap(drawable: Drawable, shapePath: Path): BitmapInfo {
        val baseInfo = createBadgedIconBitmap(drawable)
        val shapedBitmap = Bitmap.createBitmap(iconBitmapSize, iconBitmapSize, Bitmap.Config.ARGB_8888)
        val c = Canvas(shapedBitmap)

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawPath(shapePath, maskPaint)

        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        c.drawBitmap(baseInfo.icon, 0f, 0f, maskPaint)

        return BitmapInfo(shapedBitmap, baseInfo.color)
    }

    private fun renderAdaptiveIcon(canvas: Canvas, icon: AdaptiveIconDrawable) {
        val width = iconBitmapSize
        val height = iconBitmapSize
        icon.setBounds(0, 0, width, height)
        icon.draw(canvas)
    }

    private fun renderStandardDrawable(canvas: Canvas, drawable: Drawable) {
        val intrinsicWidth = max(1, drawable.intrinsicWidth)
        val intrinsicHeight = max(1, drawable.intrinsicHeight)

        val scale = min(
            iconBitmapSize.toFloat() / intrinsicWidth,
            iconBitmapSize.toFloat() / intrinsicHeight
        ) * 0.9f

        val scaledWidth = (intrinsicWidth * scale).toInt()
        val scaledHeight = (intrinsicHeight * scale).toInt()
        val left = (iconBitmapSize - scaledWidth) / 2
        val top = (iconBitmapSize - scaledHeight) / 2

        drawable.setBounds(left, top, left + scaledWidth, top + scaledHeight)
        drawable.draw(canvas)
    }

    private fun drawEnvironmentBadge(canvas: Canvas, @ColorInt badgeColor: Int) {
        val badgeSize = (iconBitmapSize * 0.28f).toInt()
        val padding = (iconBitmapSize * 0.04f).toInt()
        val left = iconBitmapSize - badgeSize - padding
        val top = iconBitmapSize - badgeSize - padding

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeColor
            style = Paint.Style.FILL
        }

        // Draw shadow ring for contrast
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = 70
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }

        val cx = left + badgeSize / 2f
        val cy = top + badgeSize / 2f
        val radius = badgeSize / 2f

        canvas.drawCircle(cx, cy, radius, badgePaint)
        canvas.drawCircle(cx, cy, radius, shadowPaint)
    }

    private fun drawWorkBadge(canvas: Canvas) {
        val badgeSize = (iconBitmapSize * 0.32f).toInt()
        val padding = (iconBitmapSize * 0.02f).toInt()
        val left = iconBitmapSize - badgeSize - padding
        val top = iconBitmapSize - badgeSize - padding

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A73E8") // Material Google Blue
            style = Paint.Style.FILL
        }
        val cx = left + badgeSize / 2f
        val cy = top + badgeSize / 2f
        val r = badgeSize / 2f
        canvas.drawCircle(cx, cy, r, bgPaint)

        // Draw simple briefcase glyph in white
        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val caseW = r * 1.1f
        val caseH = r * 0.8f
        canvas.drawRoundRect(cx - caseW / 2, cy - caseH / 4, cx + caseW / 2, cy + caseH * 0.75f, 3f, 3f, glyphPaint)
    }

    protected open fun isManagedProfile(user: UserHandle): Boolean {
        // Compare with host process user
        return user != android.os.Process.myUserHandle()
    }

    protected fun extractDominantColor(bitmap: Bitmap): Int {
        if (bitmap.width <= 0 || bitmap.height <= 0) return Color.TRANSPARENT
        val sampleX = bitmap.width / 2
        val sampleY = bitmap.height / 2
        return bitmap.getPixel(sampleX, sampleY)
    }

    override fun close() {
        // Base close no-op
    }
}
