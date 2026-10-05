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

package com.sandboxr.launcher.organizer.creation.screen.ui.spacecreator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandboxr.launcher.organizer.creation.screen.ui.spacecreator.chooselayout.ChooseLayout
import com.sandboxr.launcher.organizer.creation.screen.ui.spacecreator.chooselayout.ChooseLayoutState
import com.sandboxr.launcher.organizer.generator.CreationSession

@Composable
fun CreateScreen(
    viewModel: SpaceCreatorViewModel,
    onDismiss: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var layoutState by remember { mutableStateOf(ChooseLayoutState()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF6C63FF)
            )
            return
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Space Creator",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Generate curated desktop pages organized by purpose",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val genResult = state.generatedResult
            if (genResult is CreationSession.GenerationResult.Screens) {
                // Preview of generated pages
                Text(
                    text = "Generated ${genResult.pages.size} New Space Screens",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(genResult.pages) { pageItems ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E1E))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Space Screen (${pageItems.size} items)",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = pageItems.joinToString(", ") { it.title?.toString() ?: "App" },
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { viewModel.commitSpaces(onDismiss) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Spaces to Home Screen", color = Color.White)
                    }
                }
            } else {
                // Topic selection and layout configuration
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ChooseLayout(
                            state = layoutState,
                            onOptionSelected = { optionId ->
                                layoutState = layoutState.copy(selectedOptionId = optionId)
                                viewModel.setLayoutType(optionId)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Categories",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Select All",
                                color = Color(0xFF6C63FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { viewModel.selectAll() }
                            )
                        }
                    }

                    items(state.availableTopics) { topicData ->
                        val isSelected = state.selectedTopics.contains(topicData.topic)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF2E2B52) else Color(0xFF1E1E1E))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF6C63FF) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.toggleTopic(topicData.topic) }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = topicData.topic,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF6C63FF) else Color(0xFF2A2A2A))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${topicData.appCount} apps",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { viewModel.generateSpaces() },
                        enabled = state.selectedTopics.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Generate Spaces (${state.selectedTopics.size})", color = Color.White)
                    }
                }
            }
        }
    }
}
