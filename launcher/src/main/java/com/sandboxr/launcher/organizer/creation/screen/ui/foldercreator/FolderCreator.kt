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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandboxr.launcher.organizer.generator.CreationSession

@Composable
fun FolderCreator(
    viewModel: FolderCreatorViewModel,
    onDismiss: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Smart Folder Creation",
                color = Color.White,
                fontSize = 22.sp,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Select categories to automatically generate organized folders on your home screen.",
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF6C5CE7))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.topics) { topicData ->
                        val isSelected = state.selectedTopics.contains(topicData.topic)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF2A2A3E) else Color(0xFF161622))
                                .clickable { viewModel.toggleSelection(topicData.topic) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = topicData.topic,
                                    color = if (isSelected) Color.White else Color(0xFFDDDDDD),
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${topicData.appCount} apps",
                                    color = Color(0xFF888888),
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = if (isSelected) "✓" else "+",
                                color = if (isSelected) Color(0xFF00E676) else Color(0xFF666666),
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val gen = state.generatedFolders
                        if (gen is CreationSession.GenerationResult.Folders) {
                            viewModel.commitFolders(onDismiss)
                        } else {
                            viewModel.generateFolders()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = state.selectedTopics.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
                ) {
                    val label = if (state.generatedFolders != null) "Add to Home" else "Create (${state.selectedTopics.size})"
                    Text(label, color = Color.White)
                }
            }
        }
    }
}
