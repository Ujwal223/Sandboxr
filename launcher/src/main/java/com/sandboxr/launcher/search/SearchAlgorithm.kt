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

/**
 * An interface for performing search queries asynchronously.
 *
 * @param T Search Result type
 */
interface SearchAlgorithm<T> {

    /**
     * Performs search and sends the result to [callback].
     */
    fun doSearch(query: String, callback: SearchCallback<T>)

    /**
     * Performs search with [query] and [suggestedQueries].
     */
    fun doSearch(query: String, suggestedQueries: Array<String>?, callback: SearchCallback<T>) {
        doSearch(query, callback)
    }

    /**
     * Cancels any active search requests.
     */
    fun cancel(interruptActiveRequests: Boolean)

    /**
     * Cleans up resources when search is destroyed.
     */
    fun destroy() {}
}
