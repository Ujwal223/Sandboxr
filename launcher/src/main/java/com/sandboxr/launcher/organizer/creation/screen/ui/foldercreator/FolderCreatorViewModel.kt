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

package com.sandboxr.launcher.organizer.creation.screen.ui.foldercreator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.FolderInfo
import com.sandboxr.launcher.organizer.OrganizerTransactionContext
import com.sandboxr.launcher.organizer.generator.CreationSession
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing the smart folder creation wizard.
 */
class FolderCreatorViewModel @Inject constructor(
    private val creationSession: CreationSession,
    private val transactionContext: OrganizerTransactionContext = OrganizerTransactionContext()
) : ViewModel() {

    private val _state = MutableStateFlow(FolderCreatorState(isLoading = true))
    val state: StateFlow<FolderCreatorState> = _state.asStateFlow()

    init {
        loadTopics()
    }

    fun loadTopics() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val classified = creationSession.startClassification()
                val counts = classified.groupingBy { it.topic }.eachCount()
                val topicData = counts.map { (topic, count) ->
                    FolderTopicData(topic = topic, appCount = count)
                }
                _state.value = _state.value.copy(
                    topics = topicData,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun toggleSelection(topic: String) {
        val current = _state.value.selectedTopics
        val updated = if (current.contains(topic)) current - topic else current + topic
        _state.value = _state.value.copy(selectedTopics = updated)
    }

    fun selectAll() {
        val all = _state.value.topics.map { it.topic }.toSet()
        _state.value = _state.value.copy(selectedTopics = all)
    }

    fun generateFolders(selectedTopics: List<String> = _state.value.selectedTopics.toList()) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = creationSession.startGeneration(selectedTopics)
            _state.value = _state.value.copy(
                generatedFolders = result,
                isLoading = false
            )
        }
    }

    fun commitFolders(onFinished: () -> Unit = {}) {
        val result = _state.value.generatedFolders
        if (result is CreationSession.GenerationResult.Folders) {
            transactionContext.addFolders(result.folders)
            _state.value = _state.value.copy(isComplete = true)
            onFinished()
        }
    }
}
