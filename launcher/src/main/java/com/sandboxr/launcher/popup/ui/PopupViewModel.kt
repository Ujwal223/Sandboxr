/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.sandboxr.launcher.popup.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sandboxr.launcher.LauncherPrefs
import com.sandboxr.launcher.model.data.ItemInfoWithIcon
import com.sandboxr.launcher.popup.PopupCategory
import com.sandboxr.launcher.popup.PopupPopulator
import com.sandboxr.launcher.popup.ui.PopupMenuItemDimens.popupMenuItemHeight
import kotlin.math.min

/**
 * ViewModel for managing the UI state and interactions of the Launcher3 popup menu.
 */
class PopupViewModel {

    /** The current UI state of the popup */
    var state: PopupUiState by mutableStateOf(PopupUiState())
        private set

    /** The currently expanded section in an accordion popup menu, or null if none are expanded. */
    var expandedSection: ExpandedSection? by mutableStateOf(null)
        private set

    /** The maximum number of rows that can be displayed */
    private var maxRows = DEFAULT_MAX_ROWS

    /** The [LauncherPrefs] used to persist the state of the popup menu. */
    private var launcherPrefs: LauncherPrefs? = null

    /**
     * Initializes the ViewModel with the initial set of system shortcuts and a placeholder count
     * for deep shortcuts.
     */
    fun init(
        systemShortcuts: List<PopupItem>,
        deepShortcutCount: Int,
        availableHeightDp: Float,
        launcherPrefs: LauncherPrefs,
    ) {
        this.launcherPrefs = launcherPrefs
        maxRows = calculateMaxRows(availableHeightDp)
        updateState(systemShortcuts, deepShortcutCount)
    }

    private fun calculateMaxRows(availableHeightDp: Float): Int {
        return min(DEFAULT_MAX_ROWS, (availableHeightDp / popupMenuItemHeight.value).toInt())
    }

    private fun updateState(systemShortcuts: List<PopupItem>, deepShortcutCount: Int) {
        val numDeep = min(deepShortcutCount, PopupPopulator.MAX_SHORTCUTS)
        val deepPlaceholder = List(numDeep) { null }
        expandedSection = null

        if (systemShortcuts.size + numDeep <= maxRows) {
            state =
                PopupUiState(
                    standardSystemShortcuts = systemShortcuts,
                    deepShortcuts = deepPlaceholder,
                )
            return
        }

        val maxStandardItemsAccordion = maxRows - 1
        val maxStandardItemsWithTopBar = maxRows - 1
        val maxStandardItemsWithAccordionWithTopBar = maxRows - 2

        val (fixed, compactEligible) =
            systemShortcuts.partition { it.category == PopupCategory.SYSTEM_SHORTCUT_FIXED }
        val isAccordion = numDeep >= DEEP_SHORTCUTS_ACCORDION_THRESHOLD
        val totalSystemItems = fixed.size + compactEligible.size

        val numToPromote =
            if (totalSystemItems <= maxStandardItemsAccordion) {
                compactEligible.size
            } else {
                val targetSize =
                    if (isAccordion) maxStandardItemsWithAccordionWithTopBar
                    else maxStandardItemsWithTopBar - numDeep
                val availableSlots = maxOf(0, targetSize - fixed.size)
                min(compactEligible.size, availableSlots)
            }

        if (isAccordion) {
            expandedSection = launcherPrefs?.get(LauncherPrefs.EXPANDED_POPUP_MENU_SECTION)
        }

        state =
            PopupUiState(
                compactSystemShortcuts = compactEligible.drop(numToPromote),
                standardSystemShortcuts = fixed + compactEligible.take(numToPromote),
                deepShortcuts = deepPlaceholder,
                mainSegmentsStyle =
                    if (isAccordion) MainSegmentsStyle.ACCORDION else MainSegmentsStyle.LIST,
            )
    }

    fun onDeepShortcutsLoaded(deepShortcuts: List<ItemInfoWithIcon>) {
        state = state.copy(deepShortcuts = deepShortcuts)
    }

    fun expandSection(sectionToExpand: ExpandedSection) {
        if (
            state.mainSegmentsStyle == MainSegmentsStyle.ACCORDION &&
                expandedSection != sectionToExpand
        ) {
            expandedSection = sectionToExpand
            launcherPrefs?.putSync(LauncherPrefs.EXPANDED_POPUP_MENU_SECTION.to(sectionToExpand))
        }
    }

    companion object {
        private const val DEFAULT_MAX_ROWS = 7
        private const val DEEP_SHORTCUTS_ACCORDION_THRESHOLD = 2
    }
}
