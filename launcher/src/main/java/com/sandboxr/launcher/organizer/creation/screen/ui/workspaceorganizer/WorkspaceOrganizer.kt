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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandboxr.launcher.model.data.ItemInfo

@Composable
fun WorkspaceOrganizer(
    viewModel: WorkspaceOrganizerViewModel,
    onDismiss: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

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
                        text = "Workspace Organizer",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rearrange screen pages and organize app icons",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }

                Row {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.commitChanges(onDismiss) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Layout", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Screen tabs / horizontal page selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.screens) { index, screen ->
                        val isSelected = index == state.selectedPageIndex
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF6C63FF) else Color(0xFF1E1E1E))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.selectPage(index) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Page ${index + 1} (${screen.items.size})",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Add screen button
                Button(
                    onClick = { viewModel.addScreen() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ Add Page", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Page controls (move page left / right / delete)
            val currentScreen = state.screens.getOrNull(state.selectedPageIndex)
            if (currentScreen != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (state.selectedPageIndex > 0) {
                                    viewModel.reorderPages(state.selectedPageIndex, state.selectedPageIndex - 1)
                                }
                            },
                            enabled = state.selectedPageIndex > 0,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("← Move Page Left", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (state.selectedPageIndex < state.screens.size - 1) {
                                    viewModel.reorderPages(state.selectedPageIndex, state.selectedPageIndex + 1)
                                }
                            },
                            enabled = state.selectedPageIndex < state.screens.size - 1,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Move Page Right →", fontSize = 12.sp)
                        }
                    }

                    if (state.screens.size > 1) {
                        OutlinedButton(
                            onClick = { viewModel.removeScreen(currentScreen.screenId) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Delete Page", color = Color(0xFFFF5252), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Workspace grid preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(16.dp)
                ) {
                    if (currentScreen.items.isEmpty()) {
                        Text(
                            text = "This screen is empty.\nDrag apps here or add them via space creator.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Items on Page ${state.selectedPageIndex + 1}:",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Display items on current screen
                            for (item in currentScreen.items) {
                                val isSelected = state.selectedItem?.id == item.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF33334D) else Color(0xFF262626))
                                        .clickable { viewModel.selectItem(item) }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color(0xFF6C63FF) else Color.Gray)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = item.title?.toString() ?: "App #${item.id}",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Text(
                                        text = "Col: ${item.cellX}, Row: ${item.cellY}",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Item movement toolbar if an item is selected
                val selected = state.selectedItem
                if (selected != null && currentScreen.items.any { it.id == selected.id }) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF242436))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selected: ${selected.title ?: "Item"}",
                            color = Color.White,
                            fontSize = 13.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val newX = (selected.cellX - 1).coerceAtLeast(0)
                                    viewModel.moveItem(selected.id, currentScreen.screenId, currentScreen.screenId, newX, selected.cellY)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("← Left", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val newX = (selected.cellX + 1).coerceAtMost(3)
                                    viewModel.moveItem(selected.id, currentScreen.screenId, currentScreen.screenId, newX, selected.cellY)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Right →", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val newY = (selected.cellY - 1).coerceAtLeast(0)
                                    viewModel.moveItem(selected.id, currentScreen.screenId, currentScreen.screenId, selected.cellX, newY)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("↑ Up", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val newY = (selected.cellY + 1).coerceAtMost(4)
                                    viewModel.moveItem(selected.id, currentScreen.screenId, currentScreen.screenId, selected.cellX, newY)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("↓ Down", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
