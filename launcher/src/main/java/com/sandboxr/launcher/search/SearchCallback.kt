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

package com.sandboxr.launcher.search

import java.util.ArrayList

/**
 * An interface for receiving search results asynchronously.
 *
 * @param T Search Result item type
 */
interface SearchCallback<T> {

    companion object {
        const val UNKNOWN = 0
        const val INTERMEDIATE = 1
        const val FINAL = 2
    }

    /**
     * Called when search results are available.
     *
     * @param query the query string that generated these results
     * @param items list of matching search result items
     */
    fun onSearchResult(query: String, items: ArrayList<T>)

    /**
     * Called when search results are available with status code.
     */
    fun onSearchResult(query: String, items: ArrayList<T>, searchResultCode: Int) {
        onSearchResult(query, items)
    }

    /**
     * Called when search results should be cleared.
     */
    fun clearSearchResult()
}
