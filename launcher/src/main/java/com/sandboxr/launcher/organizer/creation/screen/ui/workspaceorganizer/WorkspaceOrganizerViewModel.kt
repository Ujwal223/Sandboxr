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

package com.sandboxr.launcher.organizer.creation.screen.ui.workspaceorganizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandboxr.launcher.LauncherSettings.Favorites
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.repository.HomeScreenRepository
import com.sandboxr.launcher.organizer.OrganizerTransactionContext
import java.util.Collections
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing workspace page reordering and icon rearrangement.
 */
class WorkspaceOrganizerViewModel @Inject constructor(
    private val transactionContext: OrganizerTransactionContext = OrganizerTransactionContext(),
    private val homeScreenRepository: HomeScreenRepository? = null
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceOrganizerState(isLoading = true))
    val state: StateFlow<WorkspaceOrganizerState> = _state.asStateFlow()

    init {
        loadScreens()
    }

    fun loadScreens(initialScreens: List<WorkspaceScreenData>? = null) {
        viewModelScope.launch {
            if (initialScreens != null) {
                _state.value = _state.value.copy(
                    screens = initialScreens,
                    isLoading = false
                )
                return@launch
            }

            val repo = homeScreenRepository
            if (repo != null) {
                val wsData = repo.workspaceState.value
                val screenIds = wsData.collectWorkspaceScreens()
                val screensList = mutableListOf<WorkspaceScreenData>()
                for (i in 0 until screenIds.size()) {
                    val sId = screenIds.get(i)
                    val itemsOnScreen = wsData.filter { it.container == Favorites.CONTAINER_DESKTOP && it.screenId == sId }
                    screensList.add(WorkspaceScreenData(screenId = sId, pageIndex = i, items = itemsOnScreen))
                }
                _state.value = _state.value.copy(
                    screens = screensList,
                    isLoading = false
                )
            } else {
                // Default empty workspace with 2 screens
                val defaultScreens = listOf(
                    WorkspaceScreenData(screenId = 0, pageIndex = 0, items = emptyList()),
                    WorkspaceScreenData(screenId = 1, pageIndex = 1, items = emptyList())
                )
                _state.value = _state.value.copy(
                    screens = defaultScreens,
                    isLoading = false
                )
            }
        }
    }

    fun selectPage(index: Int) {
        if (index in 0 until _state.value.screens.size) {
            _state.value = _state.value.copy(selectedPageIndex = index)
        }
    }

    fun selectItem(item: ItemInfo?) {
        _state.value = _state.value.copy(selectedItem = item)
    }

    fun reorderPages(fromIndex: Int, toIndex: Int) {
        val currentScreens = _state.value.screens.toMutableList()
        if (fromIndex !in currentScreens.indices || toIndex !in currentScreens.indices || fromIndex == toIndex) {
            return
        }

        val item = currentScreens.removeAt(fromIndex)
        currentScreens.add(toIndex, item)

        val updatedScreens = currentScreens.mapIndexed { idx, scr ->
            scr.copy(pageIndex = idx)
        }

        val newSelectedIndex = if (_state.value.selectedPageIndex == fromIndex) {
            toIndex
        } else {
            _state.value.selectedPageIndex
        }

        _state.value = _state.value.copy(
            screens = updatedScreens,
            selectedPageIndex = newSelectedIndex
        )
    }

    fun moveItem(itemId: Int, fromScreenId: Int, toScreenId: Int, targetCellX: Int, targetCellY: Int) {
        val currentScreens = _state.value.screens
        val fromScreen = currentScreens.find { it.screenId == fromScreenId } ?: return
        val itemToMove = fromScreen.items.find { it.id == itemId } ?: return

        // Update item coordinates
        itemToMove.screenId = toScreenId
        itemToMove.cellX = targetCellX
        itemToMove.cellY = targetCellY

        val updatedScreens = currentScreens.map { screen ->
            when (screen.screenId) {
                fromScreenId -> {
                    if (fromScreenId == toScreenId) {
                        // Same screen re-layout
                        val updatedItems = screen.items.map { if (it.id == itemId) itemToMove else it }
                        screen.copy(items = updatedItems)
                    } else {
                        screen.copy(items = screen.items.filter { it.id != itemId })
                    }
                }
                toScreenId -> {
                    screen.copy(items = screen.items + itemToMove)
                }
                else -> screen
            }
        }

        _state.value = _state.value.copy(
            screens = updatedScreens,
            selectedItem = itemToMove
        )
    }

    fun addScreen() {
        val currentScreens = _state.value.screens
        val nextScreenId = (currentScreens.maxOfOrNull { it.screenId } ?: -1) + 1
        val newScreen = WorkspaceScreenData(
            screenId = nextScreenId,
            pageIndex = currentScreens.size,
            items = emptyList()
        )
        _state.value = _state.value.copy(
            screens = currentScreens + newScreen,
            selectedPageIndex = currentScreens.size
        )
    }

    fun removeScreen(screenId: Int) {
        val currentScreens = _state.value.screens
        if (currentScreens.size <= 1) return // Keep at least one screen

        val filtered = currentScreens.filter { it.screenId != screenId }
        val updated = filtered.mapIndexed { idx, scr -> scr.copy(pageIndex = idx) }
        val newPageIndex = _state.value.selectedPageIndex.coerceAtMost(updated.size - 1)

        _state.value = _state.value.copy(
            screens = updated,
            selectedPageIndex = newPageIndex
        )
    }

    fun commitChanges(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val screens = _state.value.screens
            val pageOrder = screens.map { it.screenId }
            transactionContext.reorderPages(pageOrder)

            for (screen in screens) {
                for (item in screen.items) {
                    transactionContext.addScreen(listOf(item), screen.screenId)
                }
            }

            _state.value = _state.value.copy(isSaved = true)
            onComplete()
        }
    }
}
