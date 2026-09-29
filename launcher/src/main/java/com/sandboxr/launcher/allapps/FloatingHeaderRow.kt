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

package com.sandboxr.launcher.allapps

import android.view.View
import com.sandboxr.launcher.views.ActivityContext

/**
 * Interface implemented by rows that appear inside the [FloatingHeaderView].
 * Each row can react to scroll events, visibility changes, and vertical offsets.
 */
interface FloatingHeaderRow {

    /**
     * Called after all rows have been added to the [FloatingHeaderView].
     * Implementations should store the [parent] reference for later use.
     */
    fun setup(parent: FloatingHeaderView, rows: Array<FloatingHeaderRow>, tabsHidden: Boolean)

    /**
     * Called when the All Apps list scrolls vertically.
     *
     * @param dy The delta in pixels (positive = scrolling down, negative = up).
     * @param overScroll Pixels of overscroll (positive = over-scrolled down).
     */
    fun onScrollChanged(scrollY: Int, isHeaderVisible: Boolean)

    /**
     * Returns true if this row should be visible at the current scroll position.
     */
    fun isVisible(): Boolean

    /**
     * Returns the height in pixels this row currently occupies.
     */
    fun getExpectedHeight(): Int

    /**
     * Returns the [View] for this row.
     */
    fun asView(): View

    /**
     * Called when the active profile tab changes.
     * @param tabIndex 0 = Personal, 1 = Work, 2 = Private
     */
    fun setActiveTab(tabIndex: Int) {}

    /**
     * Releases resources and cleans up any registered listeners.
     */
    fun destroy() {}
}
