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

package com.sandboxr.launcher

/**
 * Interface implemented by any container/view that hosts CellLayout children.
 */
interface CellLayoutContainer {

    /**
     * Get the CellLayoutId for the given cellLayout.
     */
    fun getCellLayoutId(cellLayout: CellLayout): Int

    /**
     * Get the index of the given CellLayout out of all the other CellLayouts.
     */
    fun getCellLayoutIndex(cellLayout: CellLayout): Int

    /**
     * The total number of CellLayout panels in the container.
     */
    fun getPanelCount(): Int

    /**
     * Used for accessibility, returns the string spoken when referring to the CellLayout.
     */
    fun getPageDescription(pageIndex: Int): String
}
