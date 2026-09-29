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
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.views.ActivityContext

/**
 * Default search adapter provider for in-launcher app search.
 */
class DefaultSearchAdapterProvider(
    launcher: ActivityContext
) : SearchAdapterProvider<ActivityContext>(launcher) {

    private var highlightedView: View? = null

    override fun launchHighlightedItem(): Boolean {
        val view = highlightedView
        if (view is BubbleTextView && view.tag is ItemInfo) {
            val itemInfo = view.tag as ItemInfo
            return launcher.startActivitySafely(view, itemInfo.intent, itemInfo)
        }
        return false
    }

    override fun getHighlightedItem(): View? = highlightedView

    override fun clearHighlightedItem() {
        highlightedView = null
    }

    override fun isViewSupported(viewType: Int): Boolean = false

    override fun onBindView(holder: RecyclerView.ViewHolder, position: Int) {
        if (position == 0) {
            highlightedView = holder.itemView
        }
    }

    override fun onCreateViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder? = null
}
