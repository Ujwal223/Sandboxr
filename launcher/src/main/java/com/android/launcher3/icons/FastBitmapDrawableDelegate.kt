/*
 * Copyright (C) 2026 The Android Open Source Project
 * Copyright (C) 2026 Sandboxr Platform
 */
package com.android.launcher3.icons

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.sandboxr.launcher.icons.BitmapInfo

interface FastBitmapDrawableDelegate {
    interface DelegateFactory {
        fun newDelegate(
            bitmapInfo: BitmapInfo,
            iconShape: IconShape,
            paint: Paint,
            host: FastBitmapDrawable,
        ): FastBitmapDrawableDelegate
    }

    fun drawContent(
        info: BitmapInfo,
        iconShape: IconShape,
        canvas: Canvas,
        bounds: Rect,
        paint: Paint,
    )

    companion object {
        @JvmStatic
        fun Canvas.drawShaderInBounds(
            bounds: Rect,
            iconShape: Any?,
            paint: Paint,
            shader: Any?,
        ) {
            drawRect(bounds, paint)
        }
    }
}
