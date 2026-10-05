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

package com.sandboxr.launcher.remoteanimations

import android.graphics.Rect
import android.graphics.RectF
import android.view.Surface

/**
 * Transfers coordinates and bounding boxes between surface target space and launcher workspace,
 * accounting for display rotation.
 */
class RemoteAnimationCoordinateTransfer(
    val displayWidth: Int = 1080,
    val displayHeight: Int = 2400,
    val displayRotation: Int = Surface.ROTATION_0
) {

    private val tmpResult = Rect()

    /**
     * Transfers [currentRect] in source surface space to launcher display coordinates.
     */
    fun transferRectToLauncher(
        currentRect: RectF,
        targetRotation: Int = Surface.ROTATION_0,
        resultRect: RectF
    ) {
        val rotationDelta = (displayRotation - targetRotation + 4) % 4
        if (rotationDelta != Surface.ROTATION_0) {
            currentRect.round(tmpResult)
            rotateBounds(tmpResult, displayWidth, displayHeight, rotationDelta)
            resultRect.set(tmpResult)
        } else {
            resultRect.set(currentRect)
        }
    }

    /**
     * Transfers [currentRect] in launcher display space to target surface coordinates.
     */
    fun transferRectToTarget(
        currentRect: RectF,
        targetRotation: Int = Surface.ROTATION_0,
        resultRect: RectF
    ) {
        val rotationDelta = (targetRotation - displayRotation + 4) % 4
        if (rotationDelta != Surface.ROTATION_0) {
            currentRect.round(tmpResult)
            rotateBounds(tmpResult, displayWidth, displayHeight, rotationDelta)
            resultRect.set(tmpResult)
        } else {
            resultRect.set(currentRect)
        }
    }

    private fun rotateBounds(bounds: Rect, parentWidth: Int, parentHeight: Int, delta: Int) {
        val origLeft = bounds.left
        val origTop = bounds.top
        val origRight = bounds.right
        val origBottom = bounds.bottom

        when (delta) {
            1 -> { // ROTATION_90
                bounds.left = origTop
                bounds.top = parentWidth - origRight
                bounds.right = origBottom
                bounds.bottom = parentWidth - origLeft
            }
            2 -> { // ROTATION_180
                bounds.left = parentWidth - origRight
                bounds.top = parentHeight - origBottom
                bounds.right = parentWidth - origLeft
                bounds.bottom = parentHeight - origTop
            }
            3 -> { // ROTATION_270
                bounds.left = parentHeight - origBottom
                bounds.top = origLeft
                bounds.right = parentHeight - origTop
                bounds.bottom = origRight
            }
        }
    }
}
