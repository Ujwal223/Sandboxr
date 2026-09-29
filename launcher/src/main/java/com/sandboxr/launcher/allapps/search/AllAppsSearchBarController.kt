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

package com.sandboxr.launcher.allapps.search

import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.sandboxr.launcher.ExtendedEditText
import com.sandboxr.launcher.allapps.BaseAllAppsAdapter.AdapterItem
import com.sandboxr.launcher.search.SearchAlgorithm
import com.sandboxr.launcher.search.SearchCallback
import com.sandboxr.launcher.views.ActivityContext

/**
 * Controller connecting the [ExtendedEditText] search input bar to the search algorithm and callback.
 * Dispatches real-time text updates, handles keyboard action events, and manages back-key dismissal.
 */
class AllAppsSearchBarController :
    TextWatcher,
    TextView.OnEditorActionListener,
    ExtendedEditText.OnBackKeyListener {

    private var launcher: ActivityContext? = null
    private var callback: SearchCallback<AdapterItem>? = null
    private var input: ExtendedEditText? = null
    private var query: String? = null
    private var searchAlgorithm: SearchAlgorithm<AdapterItem>? = null

    /**
     * Initializes the controller with the algorithm, input view, launcher context, and results callback.
     */
    fun initialize(
        algorithm: SearchAlgorithm<AdapterItem>,
        inputView: ExtendedEditText,
        activityContext: ActivityContext,
        searchCallback: SearchCallback<AdapterItem>
    ) {
        this.searchAlgorithm = algorithm
        this.input = inputView
        this.launcher = activityContext
        this.callback = searchCallback

        inputView.addTextChangedListener(this)
        inputView.setOnEditorActionListener(this)
        inputView.setOnBackKeyListener(this)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        val q = s?.toString()?.trim() ?: ""
        query = q
        val algorithm = searchAlgorithm ?: return
        val cb = callback ?: return

        if (q.isEmpty()) {
            algorithm.cancel(true)
            cb.clearSearchResult()
        } else {
            algorithm.cancel(false)
            algorithm.doSearch(q, cb)
        }
    }

    fun refreshSearchResult() {
        val q = query
        if (!q.isNullOrEmpty()) {
            searchAlgorithm?.cancel(false)
            callback?.let { searchAlgorithm?.doSearch(q, it) }
        }
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
            actionId == EditorInfo.IME_ACTION_GO ||
            (actionId == EditorInfo.IME_NULL && event?.action == KeyEvent.ACTION_DOWN)
        ) {
            val appsView = launcher?.getAppsView()
            val mainAdapter = appsView?.mainAdapterProvider
            if (mainAdapter != null && mainAdapter.launchHighlightedItem()) {
                return true
            }
        }
        return false
    }

    override fun onBackKey(): Boolean {
        val currentText = input?.text?.toString()?.trim() ?: ""
        if (currentText.isNotEmpty()) {
            reset()
            return true
        }
        return false
    }

    /**
     * Resets search query, clears search results, and hides soft keyboard.
     */
    fun reset() {
        callback?.clearSearchResult()
        input?.reset()
        input?.clearFocus()
        input?.hideKeyboard()
        query = null
    }

    fun focusSearchField() {
        input?.showKeyboard()
    }

    fun isSearchFieldFocused(): Boolean {
        return input?.isFocused == true
    }

    companion object {
        private const val TAG = "AllAppsSearchBarController"
    }
}
