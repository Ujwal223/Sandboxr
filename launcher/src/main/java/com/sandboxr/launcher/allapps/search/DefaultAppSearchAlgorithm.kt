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

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.AnyThread
import com.sandboxr.launcher.allapps.AllAppsStore
import com.sandboxr.launcher.allapps.BaseAllAppsAdapter
import com.sandboxr.launcher.allapps.BaseAllAppsAdapter.AdapterItem
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.search.SearchAlgorithm
import com.sandboxr.launcher.search.SearchCallback
import com.sandboxr.launcher.search.StringMatcherUtility
import com.sandboxr.launcher.util.Executors
import com.sandboxr.launcher.util.LooperExecutor
import java.util.ArrayList

/**
 * Default search algorithm performing prefix, word-boundary, and fuzzy search across installed apps
 * with matching character highlighting and real-time result delivery.
 */
class DefaultAppSearchAlgorithm @JvmOverloads constructor(
    private val context: Context,
    private val appsStore: AllAppsStore,
    uiExecutor: LooperExecutor = Executors.MAIN_EXECUTOR,
    private val addNoResultsMessage: Boolean = true,
    private val bgExecutor: java.util.concurrent.Executor = Executors.MODEL_EXECUTOR
) : SearchAlgorithm<AdapterItem> {

    private val resultHandler = Handler(uiExecutor.looper)
    private var searchGeneration: Long = 0L

    companion object {
        private const val MAX_RESULTS_COUNT = 30
        private const val HIGHLIGHT_COLOR = 0xFF4E80EE.toInt() // Liquid Glass vibrant blue accent
    }

    override fun cancel(interruptActiveRequests: Boolean) {
        searchGeneration++
        if (interruptActiveRequests) {
            resultHandler.removeCallbacksAndMessages(null)
        }
    }

    override fun doSearch(query: String, callback: SearchCallback<AdapterItem>) {
        searchGeneration++
        val currentGeneration = searchGeneration

        bgExecutor.execute {
            val trimmedQuery = query.trim()
            val apps = appsStore.getApps()
            val scoredResults = ArrayList<ScoredApp>()

            for (app in apps) {
                val titleStr = app.title?.toString() ?: continue
                val score = StringMatcherUtility.fuzzyScore(trimmedQuery, titleStr)
                if (score > 0) {
                    val highlighted = StringMatcherUtility.highlightMatches(
                        title = titleStr,
                        query = trimmedQuery,
                        highlightColor = HIGHLIGHT_COLOR
                    )
                    scoredResults.add(ScoredApp(app, score, highlighted))
                }
            }

            // Sort by score descending (exact & prefix matches first), then alphabetically
            scoredResults.sortWith { a, b ->
                val scoreDiff = b.score.compareTo(a.score)
                if (scoreDiff != 0) scoreDiff
                else a.app.title.toString().compareTo(b.app.title.toString(), ignoreCase = true)
            }

            val finalItems = ArrayList<AdapterItem>()
            val count = minOf(scoredResults.size, MAX_RESULTS_COUNT)
            for (i in 0 until count) {
                val scored = scoredResults[i]
                finalItems.add(AdapterItem.asApp(scored.app, scored.highlightedTitle))
            }

            if (addNoResultsMessage && finalItems.isEmpty()) {
                val emptyItem = AdapterItem.asEmptySearch()
                finalItems.add(emptyItem)
            }

            resultHandler.post {
                if (currentGeneration == searchGeneration) {
                    callback.onSearchResult(query, finalItems)
                }
            }
        }
    }

    private data class ScoredApp(
        val app: AppInfo,
        val score: Int,
        val highlightedTitle: CharSequence
    )
}
