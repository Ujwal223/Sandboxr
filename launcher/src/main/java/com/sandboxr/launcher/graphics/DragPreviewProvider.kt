/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.sandboxr.launcher.graphics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import com.sandboxr.launcher.icons.FastBitmapDrawable
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Generates elevated preview bitmaps and drawables when dragging icons, folders, or widgets
 * across the workspace grid.
 */
open class DragPreviewProvider(
    @JvmField val view: View,
    context: Context = view.context,
) {
    private val tempRect = Rect()

    val blurSizeOutline: Int = try {
        val id = context.resources.getIdentifier("blur_size_medium_outline", "dimen", context.packageName)
        if (id != 0) context.resources.getDimensionPixelSize(id) else 16
    } catch (e: Exception) {
        16
    }

    val previewPadding: Int = blurSizeOutline

    open fun drawDragView(destCanvas: Canvas, scale: Float) {
        val saveCount = destCanvas.save()
        destCanvas.scale(scale, scale)
        destCanvas.translate(blurSizeOutline / 2f, blurSizeOutline / 2f)
        view.draw(destCanvas)
        destCanvas.restoreToCount(saveCount)
    }

    open fun createDrawable(): Drawable {
        val width = max(view.width, 1)
        val height = max(view.height, 1)
        val scale = view.scaleX

        val bmp = Bitmap.createBitmap(
            width + blurSizeOutline,
            height + blurSizeOutline,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        drawDragView(canvas, scale)

        return FastBitmapDrawable(bmp)
    }

    open fun getContentView(): View? = null

    open fun getScaleAndPosition(preview: Drawable, outPos: IntArray): Float {
        view.getLocationInWindow(outPos)
        val scale = view.scaleX

        outPos[0] = (outPos[0] - (preview.intrinsicWidth - scale * view.width * scale) / 2).roundToInt()
        outPos[1] = (outPos[1] - (1 - scale) * preview.intrinsicHeight / 2 - previewPadding / 2).roundToInt()
        return scale
    }

    open fun getScaleAndPosition(targetView: View, outPos: IntArray): Float {
        targetView.getLocationInWindow(outPos)
        val scale = targetView.scaleX

        outPos[0] = (outPos[0] - (targetView.width - scale * targetView.width * scale) / 2).roundToInt()
        outPos[1] = (outPos[1] - (1 - scale) * targetView.height / 2 - previewPadding / 2).roundToInt()
        return scale
    }

    open fun convertPreviewToAlphaBitmap(preview: Bitmap): Bitmap {
        return preview.copy(Bitmap.Config.ALPHA_8, true)
    }
}
