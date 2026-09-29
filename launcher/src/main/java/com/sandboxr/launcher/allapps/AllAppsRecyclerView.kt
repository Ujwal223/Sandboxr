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
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import com.sandboxr.launcher.FastScrollRecyclerView
import com.sandboxr.launcher.views.ActivityContext

/**
 * Custom RecyclerView dedicated to the All Apps drawer, integrating alphabetical fast scrolling
 * and letter overlay synchronization.
 */
class AllAppsRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FastScrollRecyclerView(context, attrs, defStyleAttr) {

    private val fastScrollHelper = AllAppsFastScrollHelper(this)
    private var apps: AlphabeticalAppsList? = null

    init {
        // Optimize view pooling for standard icon views
        recycledViewPool.setMaxRecycledViews(BaseAllAppsAdapter.VIEW_TYPE_ICON, 30)
        recycledViewPool.setMaxRecycledViews(BaseAllAppsAdapter.VIEW_TYPE_ALL_APPS_DIVIDER, 2)
        recycledViewPool.setMaxRecycledViews(BaseAllAppsAdapter.VIEW_TYPE_EMPTY_SEARCH, 1)

        addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                onUpdateScrollbar(dy)
            }
        })
    }

    fun setApps(appsList: AlphabeticalAppsList) {
        this.apps = appsList
    }

    fun getApps(): AlphabeticalAppsList? = apps

    override fun scrollToPositionAtProgress(touchFraction: Float): CharSequence {
        val appList = apps ?: return ""
        val sections = appList.getFastScrollerSections()
        if (sections.isEmpty()) return ""

        val clampedFraction = maxOf(0f, minOf(1f, touchFraction))
        val index = (clampedFraction * (sections.size - 1)).toInt()
        val section = sections[index]
        fastScrollHelper.smoothScrollToSection(section)
        return section.sectionName
    }

    override fun onFastScrollCompleted() {
        super.onFastScrollCompleted()
        fastScrollHelper.onFastScrollCompleted()
    }

    fun onSearchResultsChanged() {
        scrollToTop()
    }

    override fun onUpdateScrollbar(dy: Int) {
        val scrollY = computeVerticalScrollOffset()
        val availableScrollHeight = getAvailableScrollHeight()
        synchronizeScrollBarThumbOffsetToViewScroll(scrollY, availableScrollHeight)
    }

    companion object {
        const val TAG = "AllAppsRecyclerView"
    }
}
