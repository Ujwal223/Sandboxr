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

package com.sandboxr.launcher.recents.domain.usecase

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import com.android.systemui.shared.recents.model.ThumbnailData
import javax.inject.Inject

data class ThumbnailPosition(
    val matrix: Matrix = Matrix(),
    val insets: Rect = Rect(),
    val isRotated: Boolean = false,
    val clippedInsets: RectF = RectF(),
)

class IsThumbnailValidUseCase @Inject constructor() {
    operator fun invoke(
        thumbnailData: ThumbnailData?,
        viewWidth: Int,
        viewHeight: Int,
    ): Boolean {
        val thumbnail = thumbnailData?.thumbnail ?: return false
        if (thumbnail.isRecycled) return false
        if (viewWidth <= 0 || viewHeight <= 0) return true
        val thumbWidth = thumbnail.width
        val thumbHeight = thumbnail.height
        if (thumbWidth <= 0 || thumbHeight <= 0) return false

        val viewAspect = viewWidth.toFloat() / viewHeight.toFloat()
        val thumbAspect = thumbWidth.toFloat() / thumbHeight.toFloat()
        // Allow within 25% aspect difference
        val ratio = viewAspect / thumbAspect
        return ratio in 0.65f..1.45f
    }
}

class GetThumbnailPositionUseCase @Inject constructor() {
    operator fun invoke(
        thumbnailData: ThumbnailData?,
        viewWidth: Int,
        viewHeight: Int,
    ): ThumbnailPosition {
        val matrix = Matrix()
        val insets = thumbnailData?.insets ?: Rect()
        val thumbnail = thumbnailData?.thumbnail
        if (thumbnail != null && viewWidth > 0 && viewHeight > 0) {
            val scaleX = viewWidth.toFloat() / thumbnail.width.toFloat()
            val scaleY = viewHeight.toFloat() / thumbnail.height.toFloat()
            val scale = maxOf(scaleX, scaleY)
            matrix.setScale(scale, scale)
        }
        return ThumbnailPosition(matrix = matrix, insets = insets)
    }
}
