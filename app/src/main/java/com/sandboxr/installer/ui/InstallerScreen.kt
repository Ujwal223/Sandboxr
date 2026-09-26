package com.sandboxr.installer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandboxr.data.db.EnvironmentEntity
import com.sandboxr.data.db.NetworkConfig
import com.sandboxr.installer.StagedApkInfo

/**
 * State representing installation progress and results.
 */
sealed class InstallerInstallState {
    data object Idle : InstallerInstallState()
    data class Installing(val targetName: String) : InstallerInstallState()
    data class Success(val targetName: String, val message: String = "App successfully installed.") : InstallerInstallState()
    data class Error(val message: String) : InstallerInstallState()
}

/**
 * Full Compose UI screen for APK inspection and target environment selection.
 *
 * Implements the SANDBOXR Liquid Glass + GrapheneOS design language:
 * - Displays staged APK metadata (name, package, version, size, API level)
 * - Highlights 1-tap installation to Host System (zero virtualization)
 * - Lists all isolated Virtual Environments with color badges and network mode
 * - Provides inline dialog to dynamically create and target a "+ New Environment"
 */
@Composable
fun InstallerScreen(
    apkInfo: StagedApkInfo,
    environments: List<EnvironmentEntity>,
    installState: InstallerInstallState,
    onInstallToSystem: (StagedApkInfo) -> Unit,
    onInstallToVirtual: (StagedApkInfo, EnvironmentEntity) -> Unit,
    onCreateNewEnvironment: (name: String, colorTag: Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF0A0A0F) // Obsidian base
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // 1. APK Header Card
                ApkDetailsCard(apkInfo = apkInfo)

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Target Selector Heading
                Text(
                    text = "SELECT INSTALLATION TARGET",
                    color = Color(0xFF8C8CA0),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Scrollable List of Destinations
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // System Environment Option
                    item {
                        SystemTargetCard(
                            onClick = { onInstallToSystem(apkInfo) },
                            enabled = installState is InstallerInstallState.Idle
                        )
                    }

                    // Section Divider
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFF22223A)
                            )
                            Text(
                                text = "ISOLATED VAULTS",
                                color = Color(0xFF5C5C70),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFF22223A)
                            )
                        }
                    }

                    // Virtual Environments List
                    val virtualEnvs = environments.filter { !it.isSystem }
                    items(virtualEnvs, key = { it.id }) { env ->
                        VirtualTargetCard(
                            environment = env,
                            onClick = { onInstallToVirtual(apkInfo, env) },
                            enabled = installState is InstallerInstallState.Idle
                        )
                    }

                    // + Create New Environment Card
                    item {
                        CreateEnvironmentCard(
                            onClick = { showCreateDialog = true },
                            enabled = installState is InstallerInstallState.Idle
                        )
                    }
                }

                // 4. Dismiss / Cancel Action
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF22223A)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF8C8CA0)
                    )
                ) {
                    Text(
                        text = "Cancel Installation",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Installation Progress & Feedback Modal Overlay
            AnimatedVisibility(
                visible = installState !is InstallerInstallState.Idle,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                InstallationStatusOverlay(
                    state = installState,
                    onDismiss = onDismiss
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateEnvironmentDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, color ->
                showCreateDialog = false
                onCreateNewEnvironment(name, color)
            }
        )
    }
}

@Composable
private fun ApkDetailsCard(apkInfo: StagedApkInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF12121A))
            .border(1.dp, Color(0xFF22223A), RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // App Icon or Monogram
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1A1A26))
                    .border(1.dp, Color(0xFF5B8AF5).copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = apkInfo.label.take(1).uppercase(),
                    color = Color(0xFF5B8AF5),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = apkInfo.label,
                    color = Color(0xFFF0F0F5),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = apkInfo.packageName,
                    color = Color(0xFF8C8CA0),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Spec Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1A26))
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SpecTag(label = "VERSION", value = "v${apkInfo.versionName}")
            SpecTag(
                label = "SIZE",
                value = "${String.format("%.1f", apkInfo.fileSizeBytes / (1024.0 * 1024.0))} MB"
            )
            SpecTag(label = "TARGET", value = "API ${apkInfo.targetSdk}")
        }
    }
}

@Composable
private fun SpecTag(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            color = Color(0xFF5C5C70),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = Color(0xFFC8C8D0),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SystemTargetCard(
    onClick: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161624))
            .border(1.dp, Color(0xFF2A2A40), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // System Tag Dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(0xFF34C97A)) // Emerald Green
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "System Environment",
                    color = Color(0xFFF0F0F5),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF34C97A).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "HOST OS",
                        color = Color(0xFF34C97A),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Unvirtualized · Host PackageInstaller handoff",
                color = Color(0xFF8C8CA0),
                fontSize = 12.sp
            )
        }

        Text(
            text = "Install",
            color = Color(0xFF5B8AF5),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun VirtualTargetCard(
    environment: EnvironmentEntity,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val envColor = Color(environment.colorTag)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF12121A))
            .border(1.dp, Color(0xFF22223A), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Environment Color Badge
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(envColor)
                .border(2.dp, Color(0xFF0A0A0F), CircleShape)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = environment.displayName,
                color = Color(0xFFF0F0F5),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Net: ${environment.networkConfig.name}",
                    color = Color(0xFF8C8CA0),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "•",
                    color = Color(0xFF5C5C70),
                    fontSize = 11.sp
                )
                Text(
                    text = if (environment.gmsEnabled) "GMS Active" else "GMS Blocked",
                    color = if (environment.gmsEnabled) Color(0xFFF5A623) else Color(0xFF5C5C70),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A26))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Add to Vault",
                color = envColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CreateEnvironmentCard(
    onClick: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F0F17))
            .border(
                1.dp,
                Color(0xFF5B8AF5).copy(alpha = 0.35f),
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "+ New Environment",
            color = Color(0xFF5B8AF5),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InstallationStatusOverlay(
    state: InstallerInstallState,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F).copy(alpha = 0.88f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF161622))
                .border(1.dp, Color(0xFF28283C), RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                is InstallerInstallState.Installing -> {
                    CircularProgressIndicator(
                        color = Color(0xFF5B8AF5),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Installing to ${state.targetName}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Executing userspace package registration...",
                        color = Color(0xFF8C8CA0),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
                is InstallerInstallState.Success -> {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34C97A).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            color = Color(0xFF34C97A),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Installation Complete",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "App installed into ${state.targetName}.",
                        color = Color(0xFFA0A0AB),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5B8AF5)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Done")
                    }
                }
                is InstallerInstallState.Error -> {
                    Text(
                        text = "Installation Failed",
                        color = Color(0xFFFF453A),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = state.message,
                        color = Color(0xFFC8C8D0),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF28283C)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Close")
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun CreateEnvironmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val defaultColors = listOf(
        0xFF3A4A6BL to "Slate",
        0xFF4A3A6BL to "Violet",
        0xFF6B3A4AL to "Rose",
        0xFF6B5A2AL to "Amber",
        0xFF2A5A5AL to "Teal",
        0xFF3A5A2AL to "Moss",
        0xFF6B2A2AL to "Crimson",
        0xFF3A3A3AL to "Graphite"
    )
    var selectedColor by remember { mutableStateOf(defaultColors[0].first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161622),
        title = {
            Text(
                text = "New Environment Vault",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "Name your isolated vault container:",
                    color = Color(0xFF8C8CA0),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Work, Banking, Travel", color = Color(0xFF5C5C70)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF5B8AF5),
                        unfocusedBorderColor = Color(0xFF28283C)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Color Badge:",
                    color = Color(0xFF8C8CA0),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    defaultColors.take(4).forEach { (colorVal, _) ->
                        ColorSelectDot(
                            color = Color(colorVal),
                            isSelected = selectedColor == colorVal,
                            onClick = { selectedColor = colorVal }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    defaultColors.drop(4).forEach { (colorVal, _) ->
                        ColorSelectDot(
                            color = Color(colorVal),
                            isSelected = selectedColor == colorVal,
                            onClick = { selectedColor = colorVal }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), selectedColor)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5B8AF5)
                )
            ) {
                Text("Create & Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8C8CA0))
            }
        }
    )
}

@Composable
private fun ColorSelectDot(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.White else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}
