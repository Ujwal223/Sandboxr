/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.sandboxr.launcher.DragSource
import com.sandboxr.launcher.Insettable
import com.sandboxr.launcher.allapps.search.AppsSearchContainerLayout
import com.sandboxr.launcher.allapps.search.DefaultSearchAdapterProvider
import com.sandboxr.launcher.allapps.search.SearchAdapterProvider
import com.sandboxr.launcher.appprediction.AppPredictionManager
import com.sandboxr.launcher.appprediction.PredictionRowView
import com.sandboxr.launcher.views.ActivityContext
import com.sandboxr.launcher.views.RecyclerViewFastScroller
import com.sandboxr.launcher.views.ScrimView
import com.sandboxr.launcher.workprofile.WorkProfileManager
import com.sandboxr.launcher.workprofile.WorkProfilePausedView

/**
 * Main container view for the All Apps drawer screen.
 * Hosts the alphabetical application grid, floating header (tab strip + search slot),
 * work/private profile state management, and edge fast scroller with letter overlay.
 */
open class ActivityAllAppsContainerView<T : ActivityContext> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), Insettable, DragSource, AllAppsStore.OnUpdateListener {

    @Suppress("UNCHECKED_CAST")
    protected val activityContext: T = ActivityContext.lookupContext(context)
    protected val insets = Rect()

    private val allAppsStore: AllAppsStore
    private val workProfileManager: WorkProfileManager
    private val privateProfileManager: PrivateProfileManager
    private val appsList: AlphabeticalAppsList
    private val gridAdapter: AllAppsGridAdapter
    private val recyclerView: AllAppsRecyclerView
    private val fastScroller: RecyclerViewFastScroller
    private val fastScrollerPopup: TextView
    private val searchContainer: FrameLayout
    private val searchLayout: AppsSearchContainerLayout

    // Profile tab system
    private val floatingHeader: FloatingHeaderView
    private val predictionRow: PredictionRowView
    private val tabStrip: PersonalWorkSlidingTabStrip
    private val workPausedView: WorkProfilePausedView

    private var isSearching: Boolean = false

    private val predictionListener = AppPredictionManager.PredictionListener { predictions ->
        predictionRow.setPredictedApps(predictions)
    }

    private val workProfileStateListener = WorkProfileManager.WorkProfileStateListener { state ->
        post { onWorkProfileStateChanged(state) }
    }

    private val privateProfileStateListener = PrivateProfileManager.PrivateProfileStateListener { state ->
        post { onPrivateProfileStateChanged(state) }
    }

    init {
        // Resolve or create store
        allAppsStore = activityContext.getAppsStore() ?: AllAppsStore()
        allAppsStore.addUpdateListener(this)

        // Initialize profile managers
        workProfileManager = WorkProfileManager.get(context)
        privateProfileManager = PrivateProfileManager(context)

        appsList = AlphabeticalAppsList(
            activityContext,
            allAppsStore,
            workProfileManager,
            privateProfileManager
        )

        val inflater = activityContext.getLayoutInflater() ?: LayoutInflater.from(context)
        gridAdapter = AllAppsGridAdapter(
            activityContext,
            inflater,
            appsList,
            DefaultSearchAdapterProvider(activityContext)
        )
        appsList.setAdapter(gridAdapter)

        // ── Search Container Slot ──────────────────────────────────────
        searchContainer = FrameLayout(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP
            }
        }

        searchLayout = AppsSearchContainerLayout(context).apply {
            val density = resources.displayMetrics.density
            val h = (48 * density).toInt()
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                h
            ).apply {
                val marginH = (16 * density).toInt()
                val marginV = (4 * density).toInt()
                setMargins(marginH, marginV, marginH, marginV)
            }
        }
        searchContainer.addView(searchLayout)
        searchLayout.initializeSearch(this)

        // ── Recycler View ──────────────────────────────────────────────
        recyclerView = AllAppsRecyclerView(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = gridAdapter.getLayoutManager()
            adapter = gridAdapter
            setApps(appsList)
            clipToPadding = false
        }

        // ── Letter Overlay Popup ───────────────────────────────────────
        val density = context.resources.displayMetrics.density
        val popupSize = (64 * density).toInt()
        fastScrollerPopup = TextView(context).apply {
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            alpha = 0f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E61E1E2E"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#4D80A0FF"))
            }
            layoutParams = LayoutParams(popupSize, popupSize).apply {
                gravity = Gravity.TOP or Gravity.END
                marginEnd = (48 * density).toInt()
            }
            elevation = 16 * density
        }

        // ── Fast Scroller ──────────────────────────────────────────────
        fastScroller = RecyclerViewFastScroller(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(
                (36 * density).toInt(),
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.END
            }
            setPopupView(fastScrollerPopup)
        }

        recyclerView.bindFastScrollbar(fastScroller)

        // ── Prediction Row ─────────────────────────────────────────────
        predictionRow = PredictionRowView(context).apply {
            id = View.generateViewId()
        }

        // ── Profile Tab Strip ──────────────────────────────────────────
        tabStrip = PersonalWorkSlidingTabStrip(context).apply {
            id = View.generateViewId()
            setOnTabSelectedListener { tabIndex ->
                switchToTab(tabIndex)
            }
        }

        floatingHeader = FloatingHeaderView(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP
            }
            addRow(predictionRow)
            addRow(tabStrip)
        }

        // ── Work Profile Paused View ───────────────────────────────────
        workPausedView = WorkProfilePausedView(context).apply {
            id = View.generateViewId()
            visibility = View.GONE
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setOnEnableClickedListener {
                workProfileManager.setWorkProfileEnabled(true)
            }
        }

        // ── Assemble View Hierarchy ────────────────────────────────────
        setBackgroundColor(Color.parseColor("#E60D0D14"))
        addView(recyclerView)
        addView(searchContainer)
        addView(floatingHeader)
        addView(fastScroller)
        addView(fastScrollerPopup)
        addView(workPausedView)

        // Wire up profile managers and prediction manager
        workProfileManager.init()
        privateProfileManager.init()
        workProfileManager.addListener(workProfileStateListener)
        privateProfileManager.addListener(privateProfileStateListener)
        AppPredictionManager.get(context).addListener(predictionListener)

        // Configure tabs based on profile availability
        post { setupProfileTabs() }
    }

    /**
     * Configures the tab strip based on which profiles exist.
     */
    private fun setupProfileTabs() {
        val hasWork = workProfileManager.hasWorkProfile()
        val hasPrivate = privateProfileManager.hasPrivateProfile()

        val tabs = buildList {
            add("Personal")
            if (hasWork) add("Work")
            if (hasPrivate) add("Private")
        }

        val tabsHidden = tabs.size <= 1
        if (tabsHidden) {
            tabStrip.visibility = View.GONE
        } else {
            tabStrip.visibility = View.VISIBLE
            tabStrip.setTabs(tabs)
        }
        floatingHeader.setup(tabsHidden)
    }

    private fun onWorkProfileStateChanged(state: WorkProfileManager.WorkProfileState) {
        setupProfileTabs()
        workPausedView.applyState(state)
        // Re-filter the apps list in case work user appeared/disappeared
        appsList.onAppsUpdated()
    }

    private fun onPrivateProfileStateChanged(state: PrivateProfileManager.PrivateProfileState) {
        setupProfileTabs()
        appsList.onAppsUpdated()
    }

    override fun setInsets(insets: Rect) {
        this.insets.set(insets)
        val density = context.resources.displayMetrics.density
        val searchBarHeight = (56 * density).toInt()
        val topPadding = insets.top + searchBarHeight + (16 * density).toInt()
        val bottomPadding = insets.bottom + (16 * density).toInt()

        recyclerView.setPadding(
            recyclerView.paddingLeft,
            topPadding,
            recyclerView.paddingRight,
            bottomPadding
        )

        val searchLp = searchContainer.layoutParams as LayoutParams
        searchLp.topMargin = insets.top + (8 * density).toInt()
        searchContainer.layoutParams = searchLp

        val scrollerLp = fastScroller.layoutParams as LayoutParams
        scrollerLp.topMargin = topPadding
        scrollerLp.bottomMargin = insets.bottom
        fastScroller.layoutParams = scrollerLp
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        AppPredictionManager.get(context).removeListener(predictionListener)
        workProfileManager.removeListener(workProfileStateListener)
        privateProfileManager.removeListener(privateProfileStateListener)
        floatingHeader.destroy()
        privateProfileManager.destroy()
    }

    fun getAppsStore(): AllAppsStore = allAppsStore

    fun getAppsList(): AlphabeticalAppsList = appsList

    fun getGridAdapter(): AllAppsGridAdapter = gridAdapter

    fun getRecyclerView(): AllAppsRecyclerView = recyclerView

    fun getFastScroller(): RecyclerViewFastScroller = fastScroller

    fun getFastScrollerPopup(): TextView = fastScrollerPopup

    fun getSearchContainer(): FrameLayout = searchContainer

    fun getSearchUiManager(): SearchUiManager = searchLayout

    val mainAdapterProvider: SearchAdapterProvider<*>?
        get() = gridAdapter.adapterProvider

    fun getFloatingHeader(): FloatingHeaderView = floatingHeader

    fun getPredictionRow(): PredictionRowView = predictionRow

    fun getTabStrip(): PersonalWorkSlidingTabStrip = tabStrip

    open fun isSearching(): Boolean = isSearching

    open fun setSearchResults(results: List<BaseAllAppsAdapter.AdapterItem>?) {
        isSearching = !results.isNullOrEmpty()
        appsList.setSearchResults(results)
    }

    open fun resetSearch() {
        isSearching = false
        searchLayout.resetSearch()
        appsList.setSearchResults(null)
        recyclerView.scrollToTop()
    }

    /**
     * Switches the visible profile tab to [tabIndex].
     * 0 = Personal, 1 = Work, 2 = Private.
     */
    open fun switchToTab(tab: Int) {
        tabStrip.selectTab(tab, animate = true)
        appsList.setActiveProfileTab(tab)

        // Show/hide the work paused overlay when switching to the Work tab
        if (tab == PersonalWorkSlidingTabStrip.TAB_WORK) {
            workPausedView.applyState(workProfileManager.currentState)
        } else {
            workPausedView.visibility = View.GONE
        }
    }

    var scrimView: ScrimView? = null

    open fun shouldBackExitSearch(): Boolean = isSearching || searchLayout.shouldInterceptBackButton()

    open fun getActiveRecyclerView(): AllAppsRecyclerView = recyclerView

    open fun getNavBarScrimHeight(): Int = insets.bottom

    open fun isInAllApps(): Boolean = true

    override fun onDropCompleted(target: View?, d: com.sandboxr.launcher.DropTarget.DragObject, success: Boolean) {
        // Drag originating from all apps completed
    }

    override fun onAppsUpdated() {
        appsList.onAppsUpdated()
        AppPredictionManager.get(context).updatePredictions(allAppsStore.getApps())
    }

    companion object {
        const val TAG = "ActivityAllAppsContainerView"
    }
}
