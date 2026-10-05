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

package com.sandboxr.launcher.organizer.creation.screen.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.sandboxr.launcher.organizer.creation.screen.ui.foldercreator.FolderCreator
import com.sandboxr.launcher.organizer.creation.screen.ui.spacecreator.CreateScreen
import com.sandboxr.launcher.organizer.creation.screen.ui.workspaceorganizer.WorkspaceOrganizer
import com.sandboxr.launcher.organizer.dagger.OrganizerComponentProvider

/**
 * Main activity hosting Sandboxr's smart workspace management tools:
 * 1. Workspace Organizer (page and icon layout)
 * 2. Smart Folder Creator (auto topic classification & folder generation)
 * 3. Space Creator (curated functional screen creation)
 */
class OrganizerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialMode = intent.getIntExtra(EXTRA_MODE, MODE_WORKSPACE)
        val organizerComponent = OrganizerComponentProvider.get(this)
        val workspaceViewModel = organizerComponent.getWorkspaceOrganizerViewModel()
        val folderViewModel = organizerComponent.getFolderCreatorViewModel()
        val spaceViewModel = organizerComponent.getSpaceCreatorViewModel()

        setContent {
            OrganizerContent(
                initialMode = initialMode,
                workspaceViewModel = workspaceViewModel,
                folderViewModel = folderViewModel,
                spaceViewModel = spaceViewModel,
                onDismiss = { finish() }
            )
        }
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val MODE_WORKSPACE = 0
        const val MODE_FOLDERS = 1
        const val MODE_SPACE = 2
    }
}

@Composable
fun OrganizerContent(
    initialMode: Int,
    workspaceViewModel: com.sandboxr.launcher.organizer.creation.screen.ui.workspaceorganizer.WorkspaceOrganizerViewModel,
    folderViewModel: com.sandboxr.launcher.organizer.creation.screen.ui.foldercreator.FolderCreatorViewModel,
    spaceViewModel: com.sandboxr.launcher.organizer.creation.screen.ui.spacecreator.SpaceCreatorViewModel,
    onDismiss: () -> Unit = {}
) {
    var currentMode by remember { mutableStateOf(initialMode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Navigation bar between modes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModeTab(
                title = "Workspace",
                isSelected = currentMode == OrganizerActivity.MODE_WORKSPACE,
                onClick = { currentMode = OrganizerActivity.MODE_WORKSPACE }
            )
            ModeTab(
                title = "Folder Creator",
                isSelected = currentMode == OrganizerActivity.MODE_FOLDERS,
                onClick = { currentMode = OrganizerActivity.MODE_FOLDERS }
            )
            ModeTab(
                title = "Space Creator",
                isSelected = currentMode == OrganizerActivity.MODE_SPACE,
                onClick = { currentMode = OrganizerActivity.MODE_SPACE }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (currentMode) {
                OrganizerActivity.MODE_WORKSPACE -> {
                    WorkspaceOrganizer(
                        viewModel = workspaceViewModel,
                        onDismiss = onDismiss
                    )
                }
                OrganizerActivity.MODE_FOLDERS -> {
                    FolderCreator(
                        viewModel = folderViewModel,
                        onDismiss = onDismiss
                    )
                }
                OrganizerActivity.MODE_SPACE -> {
                    CreateScreen(
                        viewModel = spaceViewModel,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF6C63FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
