/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.view.View
import com.sandboxr.launcher.PagedView

/**
 * PagedView for showing different tabbed views (e.g. Personal, Work, Virtual Environment)
 * within the All Apps container.
 */
open class AllAppsPagedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PagedView<View>(context, attrs, defStyleAttr) {

    fun interface OnActivePageChangedListener {
        fun onActivePageChanged(currentActivePage: Int)
    }

    private var activePageChangedListener: OnActivePageChangedListener? = null

    fun setActivePageChangedListener(listener: OnActivePageChangedListener?) {
        activePageChangedListener = listener
    }

    override fun snapToPage(whichPage: Int, duration: Int): Boolean {
        val prevPage = mCurrentPage
        val snapped = super.snapToPage(whichPage, duration)
        if (snapped && whichPage != prevPage) {
            activePageChangedListener?.onActivePageChanged(whichPage)
        }
        return snapped
    }

    companion object {
        const val PAGE_PERSONAL = 0
        const val PAGE_WORK = 1
        const val PAGE_VIRTUAL = 2
    }
}
