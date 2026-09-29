/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.Context
import android.view.LayoutInflater
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sandboxr.launcher.allapps.search.SearchAdapterProvider
import com.sandboxr.launcher.views.ActivityContext
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Grid layout adapter rendering apps in columns with section headers and fast scroller alignment.
 */
class AllAppsGridAdapter(
    activityContext: ActivityContext,
    inflater: LayoutInflater,
    apps: AlphabeticalAppsList,
    adapterProvider: SearchAdapterProvider<*>? = null
) : BaseAllAppsAdapter(activityContext, inflater, apps, adapterProvider) {

    fun interface OnLayoutCompletedListener {
        fun onLayoutCompleted()
    }

    private val onLayoutCompletedListeners = CopyOnWriteArrayList<OnLayoutCompletedListener>()
    private val gridLayoutMgr: AppsGridLayoutManager

    init {
        gridLayoutMgr = AppsGridLayoutManager(activityContext.asContext())
        gridLayoutMgr.spanSizeLookup = GridSpanSizer()
        val columns = activityContext.getDeviceProfile().numShownAllAppsColumns
        setAppsPerRow(if (columns > 0) columns else 4)
    }

    fun addOnLayoutCompletedListener(listener: OnLayoutCompletedListener) {
        onLayoutCompletedListeners.add(listener)
    }

    fun removeOnLayoutCompletedListener(listener: OnLayoutCompletedListener) {
        onLayoutCompletedListeners.remove(listener)
    }

    override fun getLayoutManager(): AppsGridLayoutManager = gridLayoutMgr

    override fun setAppsPerRow(appsPerRow: Int) {
        super.setAppsPerRow(maxOf(1, appsPerRow))
        gridLayoutMgr.spanCount = mAppsPerRow
    }

    fun getSpanIndex(adapterIndex: Int): Int {
        return gridLayoutMgr.spanSizeLookup.getSpanIndex(adapterIndex, gridLayoutMgr.spanCount)
    }

    inner class GridSpanSizer : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            val items = apps.getAdapterItems()
            if (position < 0 || position >= items.size) return 1
            val item = items[position]
            return if (isIconViewType(item.viewType)) {
                1
            } else {
                mAppsPerRow
            }
        }
    }

    inner class AppsGridLayoutManager(context: Context) : GridLayoutManager(context, 4) {
        override fun onLayoutCompleted(state: RecyclerView.State?) {
            super.onLayoutCompleted(state)
            for (listener in onLayoutCompletedListeners) {
                listener.onLayoutCompleted()
            }
        }
    }

    companion object {
        const val TAG = "AllAppsGridAdapter"
    }
}
