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

package com.sandboxr.launcher.anim

import android.graphics.Rect

/**
 * A [RevealOutlineAnimation] that interpolates between two radii and two [Rect]s.
 */
open class RoundedRectRevealOutlineProvider(
    private val startRadius: Float,
    private val endRadius: Float,
    private val startRect: Rect,
    private val endRect: Rect
) : RevealOutlineAnimation() {

    override fun shouldRemoveElevationDuringAnimation(): Boolean = false

    override fun setProgress(progress: Float) {
        mOutlineRadius = (1f - progress) * startRadius + progress * endRadius

        mOutline.left = ((1f - progress) * startRect.left + progress * endRect.left).toInt()
        mOutline.top = ((1f - progress) * startRect.top + progress * endRect.top).toInt()
        mOutline.right = ((1f - progress) * startRect.right + progress * endRect.right).toInt()
        mOutline.bottom = ((1f - progress) * startRect.bottom + progress * endRect.bottom).toInt()
    }
}
