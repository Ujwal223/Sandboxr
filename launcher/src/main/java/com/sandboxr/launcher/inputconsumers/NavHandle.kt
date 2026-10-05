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

package com.sandboxr.launcher.inputconsumers

import android.graphics.Rect
import android.graphics.RectF

/**
 * Interface representing the navigation handle on screen, exposing its geometry and animation hooks.
 */
interface NavHandle {

    /**
     * Returns the bounding rectangle of the nav handle on screen.
     */
    fun getBoundsOnScreen(): Rect

    /**
     * Animates the nav handle during long press touch down / release.
     */
    fun animateNavBarLongPress(isTouchDown: Boolean, shrink: Boolean, durationMs: Long) {}
}

/**
 * Simple default implementation of [NavHandle] using Rect bounds.
 */
class DefaultNavHandle(
    private val bounds: Rect = Rect()
) : NavHandle {

    override fun getBoundsOnScreen(): Rect = bounds

    fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        bounds.set(left, top, right, bottom)
    }

    var isAnimating: Boolean = false
        private set

    override fun animateNavBarLongPress(isTouchDown: Boolean, shrink: Boolean, durationMs: Long) {
        isAnimating = isTouchDown
    }
}
