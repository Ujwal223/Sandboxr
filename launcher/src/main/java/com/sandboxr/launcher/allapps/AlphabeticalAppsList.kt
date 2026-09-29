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

import android.os.UserHandle
import androidx.recyclerview.widget.DiffUtil
import com.sandboxr.launcher.allapps.BaseAllAppsAdapter.AdapterItem
import com.sandboxr.launcher.allapps.PersonalWorkSlidingTabStrip.Companion.TAB_PERSONAL
import com.sandboxr.launcher.allapps.PersonalWorkSlidingTabStrip.Companion.TAB_PRIVATE
import com.sandboxr.launcher.allapps.PersonalWorkSlidingTabStrip.Companion.TAB_WORK
import com.sandboxr.launcher.compat.AlphabeticIndexCompat
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.views.ActivityContext
import com.sandboxr.launcher.workprofile.WorkProfileManager
import java.util.Objects

/**
 * Generates an alphabetically ordered, sectioned list of applications for the All Apps drawer
 * and calculates fast scroller section indices.
 *
 * Supports filtering by profile tab (Personal / Work / Private) via [setActiveProfileTab].
 */
class AlphabeticalAppsList(
    private val activityContext: ActivityContext,
    private val appsStore: AllAppsStore? = null,
    private val workProfileManager: WorkProfileManager? = null,
    private val privateProfileManager: PrivateProfileManager? = null
) : AllAppsStore.OnUpdateListener {

    data class FastScrollSectionInfo(
        val sectionName: CharSequence,
        val position: Int
    )

    private val apps = ArrayList<AppInfo>()
    private val adapterItems = ArrayList<AdapterItem>()
    private val fastScrollerSections = ArrayList<FastScrollSectionInfo>()
    private val searchResults = ArrayList<AdapterItem>()

    private var adapter: BaseAllAppsAdapter? = null
    private val appNameComparator = AppInfoComparator(activityContext.asContext())
    private val alphabeticIndex = AlphabeticIndexCompat(activityContext.asContext())
    private var numAppsPerRow: Int = 4
    private var numAppRowsInAdapter: Int = 0
    private var itemFilter: ((ItemInfo) -> Boolean)? = null

    /** The currently active profile tab index (Personal=0, Work=1, Private=2). */
    private var activeProfileTab: Int = TAB_PERSONAL

    init {
        val cols = activityContext.getDeviceProfile().numShownAllAppsColumns
        numAppsPerRow = if (cols > 0) cols else 4
        appsStore?.addUpdateListener(this)
        onAppsUpdated()
    }

    fun setAdapter(adapter: BaseAllAppsAdapter?) {
        this.adapter = adapter
    }

    fun setNumAppsPerRow(count: Int) {
        numAppsPerRow = maxOf(1, count)
        updateAdapterItems()
    }

    fun updateItemFilter(filter: ((ItemInfo) -> Boolean)?) {
        itemFilter = filter
        onAppsUpdated()
    }

    /**
     * Updates the active profile tab and re-filters the apps list.
     * @param tabIndex 0=Personal, 1=Work, 2=Private
     */
    fun setActiveProfileTab(tabIndex: Int) {
        activeProfileTab = tabIndex
        updateAdapterItems()
    }

    fun getFastScrollerSections(): List<FastScrollSectionInfo> = fastScrollerSections

    fun getAdapterItems(): List<AdapterItem> = adapterItems

    fun getNumAppRows(): Int = numAppRowsInAdapter

    fun getNumFilteredApps(): Int = adapterItems.count { it.isCountedForAccessibility() }

    fun hasSearchResults(): Boolean = searchResults.isNotEmpty()

    fun setSearchResults(results: List<AdapterItem>?): Boolean {
        if (Objects.equals(results, searchResults)) {
            return false
        }
        searchResults.clear()
        if (results != null) {
            searchResults.addAll(results)
        }
        updateAdapterItems()
        return true
    }

    override fun onAppsUpdated() {
        apps.clear()
        val sourceApps = appsStore?.getApps() ?: AppInfo.EMPTY_ARRAY

        val filtered = sourceApps.filter { info ->
            itemFilter?.invoke(info) ?: true
        }.sortedWith(appNameComparator)

        for (app in filtered) {
            val title = app.appTitle ?: app.title ?: ""
            app.sectionName = alphabeticIndex.computeSectionName(title)
            apps.add(app)
        }

        if (searchResults.isEmpty()) {
            updateAdapterItems()
        }
    }

    fun updateAdapterItems() {
        val oldItems = ArrayList(adapterItems)
        adapterItems.clear()
        fastScrollerSections.clear()

        if (hasSearchResults()) {
            adapterItems.addAll(searchResults)
        } else {
            // Determine which user handles should be shown for the current tab
            val personalUser = android.os.Process.myUserHandle()
            val workUser = workProfileManager?.workProfileUser
            val privateUser = privateProfileManager?.privateProfileUser

            // Filter apps based on active tab
            val filteredApps = apps.filter { app ->
                when (activeProfileTab) {
                    TAB_WORK -> app.user == workUser && workUser != null
                    TAB_PRIVATE -> app.user == privateUser && privateUser != null
                    else -> {
                        // Personal tab: show personal apps (not work, not private)
                        app.user == personalUser
                            || (workUser == null && privateUser == null)
                    }
                }
            }

            var position = 0
            var lastSectionName: String? = null

            for (app in filteredApps) {
                val section = app.sectionName ?: ""
                if (section != lastSectionName) {
                    lastSectionName = section
                    fastScrollerSections.add(FastScrollSectionInfo(section, position))
                }
                adapterItems.add(AdapterItem.asApp(app))
                position++
            }

            if (fastScrollerSections.isNotEmpty()) {
                adapterItems.add(AdapterItem.asBottomSpacer())
                val lastSection = fastScrollerSections.last().sectionName
                fastScrollerSections.add(FastScrollSectionInfo(lastSection, position))
            }
        }

        // Calculate rows and indices
        if (numAppsPerRow > 0) {
            var numAppsInSection = 0
            var numAppsInRow = 0
            var rowIndex = -1

            for (item in adapterItems) {
                item.rowIndex = 0
                if (BaseAllAppsAdapter.isDividerViewType(item.viewType)) {
                    numAppsInSection = 0
                } else if (BaseAllAppsAdapter.isIconViewType(item.viewType)) {
                    if (numAppsInSection % numAppsPerRow == 0) {
                        numAppsInRow = 0
                        rowIndex++
                    }
                    item.rowIndex = rowIndex
                    item.rowAppIndex = numAppsInRow
                    numAppsInSection++
                    numAppsInRow++
                }
            }
            numAppRowsInAdapter = rowIndex + 1
        }

        val currentAdapter = adapter
        if (currentAdapter != null) {
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldItems.size
                override fun getNewListSize(): Int = adapterItems.size
                override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean =
                    oldItems[oldPos].isSameAs(adapterItems[newPos])
                override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean =
                    oldItems[oldPos].isContentSame(adapterItems[newPos])
            }, false).dispatchUpdatesTo(currentAdapter)
        }
    }

    companion object {
        const val TAG = "AlphabeticalAppsList"
    }
}
