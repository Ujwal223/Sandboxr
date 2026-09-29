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

package com.sandboxr.launcher.allapps.search

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.EditorInfo
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.sandboxr.launcher.ExtendedEditText
import com.sandboxr.launcher.Insettable
import com.sandboxr.launcher.R
import com.sandboxr.launcher.allapps.ActivityAllAppsContainerView
import com.sandboxr.launcher.allapps.AllAppsStore
import com.sandboxr.launcher.allapps.BaseAllAppsAdapter.AdapterItem
import com.sandboxr.launcher.allapps.SearchUiManager
import com.sandboxr.launcher.search.SearchCallback
import com.sandboxr.launcher.views.ActivityContext
import java.util.ArrayList

/**
 * Liquid Glass search container layout and input view for All Apps drawer.
 * Coordinates input events, real-time filtering, clear action, and profile search results.
 */
class AppsSearchContainerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ExtendedEditText(context, attrs, defStyleAttr),
    SearchUiManager,
    SearchCallback<AdapterItem>,
    AllAppsStore.OnUpdateListener,
    Insettable {

    private val searchBarController = AllAppsSearchBarController()
    private var appsView: ActivityAllAppsContainerView<*>? = null
    private var isSearchSessionActive = false

    init {
        id = R.id.search_container_all_apps
        hint = "Search apps..."
        setHintTextColor(Color.parseColor("#70FFFFFF"))
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        gravity = Gravity.CENTER_VERTICAL
        maxLines = 1
        isSingleLine = true
        imeOptions = EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        val density = resources.displayMetrics.density
        val padH = (16 * density).toInt()
        val padV = (10 * density).toInt()
        setPadding(padH, padV, padH, padV)
        compoundDrawablePadding = (10 * density).toInt()

        setBackgroundResource(R.drawable.bg_all_apps_searchbox)
        updateCompoundDrawables()

        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isSearchSessionActive = !s.isNullOrEmpty()
                updateCompoundDrawables()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateCompoundDrawables() {
        val density = resources.displayMetrics.density
        val iconSize = (20 * density).toInt()

        val searchIcon = AppCompatResources.getDrawable(context, R.drawable.ic_allapps_search)?.apply {
            setBounds(0, 0, iconSize, iconSize)
        }

        val clearIcon = if (!text.isNullOrEmpty()) {
            AppCompatResources.getDrawable(context, R.drawable.ic_search_clear)?.apply {
                setBounds(0, 0, iconSize, iconSize)
            }
        } else {
            null
        }

        setCompoundDrawablesRelative(searchIcon, null, clearIcon, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && !text.isNullOrEmpty()) {
            val clearDrawable = compoundDrawablesRelative[2]
            if (clearDrawable != null) {
                val clearX = width - paddingEnd - clearDrawable.bounds.width() - 20
                if (event.x >= clearX) {
                    reset()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        appsView?.getAppsStore()?.addUpdateListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        appsView?.getAppsStore()?.removeUpdateListener(this)
    }

    override fun initializeSearch(containerView: ActivityAllAppsContainerView<*>) {
        this.appsView = containerView
        val activityContext = ActivityContext.lookupContext<ActivityContext>(context)
        val algorithm = DefaultAppSearchAlgorithm(
            context = context,
            appsStore = containerView.getAppsStore(),
            uiExecutor = activityContext.getUiExecutor(),
            addNoResultsMessage = true
        )
        searchBarController.initialize(algorithm, this, activityContext, this)
        containerView.getAppsStore().addUpdateListener(this)
    }

    override fun onAppsUpdated() {
        searchBarController.refreshSearchResult()
    }

    override fun resetSearch() {
        isSearchSessionActive = false
        searchBarController.reset()
        updateCompoundDrawables()
    }

    override fun isSearchQueryEmpty(): Boolean {
        return text.isNullOrEmpty()
    }

    override fun shouldInterceptBackButton(): Boolean {
        return isSearchSessionActive && !isSearchQueryEmpty()
    }

    override fun preDispatchKeyEvent(keyEvent: KeyEvent) {
        if (!searchBarController.isSearchFieldFocused() && keyEvent.action == KeyEvent.ACTION_DOWN) {
            val unicode = keyEvent.unicodeChar
            if (unicode > 0 && !Character.isWhitespace(unicode)) {
                searchBarController.focusSearchField()
            }
        }
    }

    override fun getEditText(): ExtendedEditText = this

    override fun onSearchResult(query: String, items: ArrayList<AdapterItem>) {
        appsView?.setSearchResults(items)
    }

    override fun clearSearchResult() {
        appsView?.setSearchResults(null)
    }

    override fun setInsets(insets: Rect) {
        val lp = layoutParams as? MarginLayoutParams ?: return
        val density = resources.displayMetrics.density
        lp.topMargin = insets.top + (8 * density).toInt()
        layoutParams = lp
    }

    companion object {
        private const val TAG = "AppsSearchContainerLayout"
    }
}
