/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.sandboxr.launcher.allapps.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sandboxr.launcher.views.ActivityContext

/**
 * A UI extension provider supplying search results view holders and interactions.
 */
abstract class SearchAdapterProvider<T : ActivityContext>(protected val launcher: T) {

    abstract fun launchHighlightedItem(): Boolean

    abstract fun getHighlightedItem(): View?

    abstract fun clearHighlightedItem()

    abstract fun isViewSupported(viewType: Int): Boolean

    abstract fun onBindView(holder: RecyclerView.ViewHolder, position: Int)

    abstract fun onCreateViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder?

    open fun getItemsPerRow(viewType: Int, appsPerRow: Int): Int = appsPerRow

    open fun getSupportedItemsPerRowArray(): IntArray = intArrayOf()
}
