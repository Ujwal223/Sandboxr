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

package com.sandboxr.launcher.allapps

import android.view.KeyEvent
import androidx.annotation.Nullable
import com.sandboxr.launcher.ExtendedEditText

/**
 * Interface for controlling the in-launcher application search UI.
 */
interface SearchUiManager {

    /**
     * Whether back button gestures should be intercepted by search session.
     */
    fun shouldInterceptBackButton(): Boolean = false

    /**
     * Initializes the search manager with the containing All Apps view.
     */
    fun initializeSearch(containerView: ActivityAllAppsContainerView<*>)

    /**
     * Notifies the search manager to reset and close any active search session.
     */
    fun resetSearch()

    /**
     * Returns whether the search query is currently empty.
     */
    fun isSearchQueryEmpty(): Boolean

    /**
     * Called before dispatching a key event to the search box.
     */
    fun preDispatchKeyEvent(keyEvent: KeyEvent) {}

    /**
     * Returns the underlying [ExtendedEditText] search input view.
     */
    @Nullable
    fun getEditText(): ExtendedEditText?

    /**
     * Hint to the edit text that it is about to be focused or unfocused.
     */
    fun prepareToFocusEditText(focused: Boolean) {}

    /**
     * Sets background visibility during animation transitions.
     */
    fun setBackgroundVisibility(visible: Boolean, maxAlpha: Float) {}

    /**
     * Returns whether the background is visible.
     */
    fun getBackgroundVisibility(): Boolean = false

    /**
     * Sets highlighted result title.
     */
    fun setFocusedResultTitle(
        title: CharSequence?,
        subtitle: CharSequence?,
        showArrow: Boolean
    ) {}

    /**
     * Refreshes the currently displayed search results.
     */
    fun refreshResults() {}

    /**
     * Returns whether search is in zero state (empty query).
     */
    fun inZeroState(): Boolean = false
}
