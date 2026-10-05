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

package com.sandboxr.launcher.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

/**
 * Bitmap information wrapper encapsulating icon bitmap, primary badge color, and decoration flags.
 */
open class BitmapInfo(
    @JvmField val icon: Bitmap,
    @ColorInt @JvmField val color: Int = 0,
) {
    @JvmField var flags: Int = 0
    @JvmField var defaultIconShape: Any? = null

    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER)
    annotation class DrawableCreationFlags

    open fun isLowRes(): Boolean = this === LOW_RES_INFO || icon === LOW_RES_BITMAP

    open fun isFullBleed(): Boolean = false

    open fun getMatchingLookupFlag(): com.sandboxr.launcher.icons.cache.CacheLookupFlag =
        com.sandboxr.launcher.icons.cache.CacheLookupFlag.DEFAULT_LOOKUP_FLAG

    open fun newIcon(context: Context): FastBitmapDrawable =
        newIcon(context, 0, null)

    open fun newIcon(
        context: Context,
        @DrawableCreationFlags creationFlags: Int,
        iconShape: Any? = null,
    ): FastBitmapDrawable = FastBitmapDrawable(icon, color)

    companion object {
        const val FLAG_THEMED: Int = 1 shl 0
        const val FLAG_NO_BADGE: Int = 1 shl 1
        const val FLAG_SKIP_USER_BADGE: Int = 1 shl 2
        const val FLAG_INSTANT: Int = 1 shl 3
        const val FLAG_WORK: Int = 1 shl 4
        const val FLAG_CLONED: Int = 1 shl 5
        const val FLAG_PRIVATE: Int = 1 shl 6

        @JvmField
        val LOW_RES_BITMAP: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)

        @JvmField
        val LOW_RES_INFO: BitmapInfo = fromBitmap(LOW_RES_BITMAP)

        @JvmStatic
        fun fromBitmap(bitmap: Bitmap): BitmapInfo = BitmapInfo(bitmap, 0)

        @JvmStatic
        fun fromBitmap(bitmap: Bitmap, @ColorInt color: Int): BitmapInfo = BitmapInfo(bitmap, color)
    }
}

/**
 * A fast lightweight Drawable wrapping a Bitmap.
 */
open class FastBitmapDrawable @JvmOverloads constructor(
    @JvmField val bitmap: Bitmap,
    @ColorInt @JvmField val iconColor: Int = 0,
) : Drawable() {

    protected val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, null, bounds, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = bitmap.width

    override fun getIntrinsicHeight(): Int = bitmap.height

    var isDisabled: Boolean = false
        private set

    open fun setDisabled(disabled: Boolean) {
        if (this.isDisabled != disabled) {
            this.isDisabled = disabled
            invalidateSelf()
        }
    }
}
