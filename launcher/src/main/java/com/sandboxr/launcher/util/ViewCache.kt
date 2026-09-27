/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.sandboxr.launcher.util

import android.content.Context
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import com.sandboxr.launcher.R

/**
 * Utility class to cache views at an Activity level.
 */
open class ViewCache {

    protected val mCache = SparseArray<CacheEntry>()

    fun setCacheSize(layoutId: Int, size: Int) {
        mCache.put(layoutId, CacheEntry(size))
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : View> getView(layoutId: Int, context: Context, parent: ViewGroup?): T {
        var entry = mCache.get(layoutId)
        if (entry == null) {
            entry = CacheEntry(1)
            mCache.put(layoutId, entry)
        }

        val result: T
        if (entry.currentSize > 0) {
            entry.currentSize--
            result = entry.views[entry.currentSize] as T
            entry.views[entry.currentSize] = null
        } else {
            result = LayoutInflater.from(context).inflate(layoutId, parent, false) as T
            result.setTag(R.id.cache_entry_tag_id, entry)
        }
        return result
    }

    fun recycleView(layoutId: Int, view: View) {
        val entry = mCache.get(layoutId)
        if (entry == null || entry !== view.getTag(R.id.cache_entry_tag_id)) {
            return
        }
        if (entry.currentSize < entry.maxSize) {
            entry.views[entry.currentSize] = view
            entry.currentSize++
        }
    }

    @VisibleForTesting
    class CacheEntry(val maxSize: Int) {
        val views: Array<View?> = arrayOfNulls(maxSize)
        var currentSize: Int = 0
    }
}
