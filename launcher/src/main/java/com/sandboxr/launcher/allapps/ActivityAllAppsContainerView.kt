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

package com.sandboxr.launcher.allapps

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.FrameLayout
import com.sandboxr.launcher.Insettable
import com.sandboxr.launcher.views.ActivityContext

/**
 * Main container view for the All Apps drawer screen.
 */
open class ActivityAllAppsContainerView<T : ActivityContext> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), Insettable {

    private val insets = Rect()

    override fun setInsets(insets: Rect) {
        this.insets.set(insets)
    }

    open fun isSearching(): Boolean = false

    open fun resetSearch() {}

    open fun switchToTab(tab: Int) {}
}
