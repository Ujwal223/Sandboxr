package com.ujwal.sandboxr.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ujwal.sandboxr.data.db.EnvironmentEntity
import com.ujwal.sandboxr.installer.InstallerActivity
import com.sandboxr.launcher.theme.SandboxrColors
import com.sandboxr.launcher.theme.SandboxrFontFamilies
import com.sandboxr.launcher.theme.SandboxrSpacingTokens
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.launcher.ui.AppIconGrid
import com.sandboxr.launcher.ui.AppItem
import com.sandboxr.launcher.ui.EnvironmentCard
import com.sandboxr.launcher.ui.EnvironmentCardData
import com.sandboxr.launcher.ui.EnvironmentIconType
import com.sandboxr.launcher.ui.EnvironmentLazyRow
import com.sandboxr.launcher.ui.PhosphorIcon
import com.sandboxr.launcher.ui.PhosphorIconView
import com.sandboxr.launcher.ui.glassCard
import com.ujwal.sandboxr.ui.model.toCardData

/**
 * Main Launcher Home View displaying the Environment Carousel and the App Icon Grid.
 * Strictly adheres to DESIGN.md Section 5 & GrapheneOS security hierarchy.
 */
@Composable
fun EnvironmentsTab(
    environments: List<EnvironmentEntity>,
    activeEnvironment: EnvironmentEntity?,
    apps: List<AppItem>,
    appCounts: Map<String, Int>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onEnvironmentSelect: (String) -> Unit,
    onEnvironmentLongClick: (EnvironmentEntity) -> Unit,
    onAddEnvironmentClick: () -> Unit,
    onAppClick: (AppItem) -> Unit,
    onCloneClick: () -> Unit,
    onUninstallApp: (packageName: String, envId: String) -> Unit,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedAppForMenu by remember { mutableStateOf<AppItem?>(null) }

    // Map EnvironmentEntity to EnvironmentCardData
    val cardDataList = remember(environments, activeEnvironment, appCounts) {
        environments.map { env ->
            val count = appCounts[env.id] ?: 0
            val isActive = env.id == activeEnvironment?.id
            env.toCardData(
                appCount = count,
                isActive = isActive
            )
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // ── 1. Environments Carousel Section ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SandboxrSpacingTokens.Normal, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SANDBOX CONTAINERS",
                    style = SandboxrTheme.typography.label.copy(letterSpacing = 1.2.sp),
                    color = SandboxrColors.TextSecondaryDark
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SandboxrColors.SurfaceLevel2Dark)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${environments.size}",
                        fontFamily = SandboxrFontFamilies.JetBrainsMono,
                        fontSize = 10.sp,
                        color = SandboxrColors.PrimaryAccent
                    )
                }
            }

            // Quick "+ Add Container"
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SandboxrColors.PrimaryAccent.copy(alpha = 0.15f))
                    .clickable(onClick = onAddEnvironmentClick)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PhosphorIconView(
                    icon = PhosphorIcon.PLUS,
                    color = SandboxrColors.PrimaryAccent,
                    size = 14.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "New",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SandboxrColors.PrimaryAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Carousel of 160dp x 220dp cards
        EnvironmentLazyRow(
            environments = cardDataList,
            onEnvironmentSelected = { card -> onEnvironmentSelect(card.id) },
            onEnvironmentLongClick = { card ->
                val entity = environments.find { it.id == card.id }
                if (entity != null) onEnvironmentLongClick(entity)
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ── 2. Search & Active Environment Header ─────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SandboxrSpacingTokens.Normal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search in ${activeEnvironment?.displayName ?: "apps"}...",
                        color = SandboxrColors.TextTertiaryDark,
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                leadingIcon = {
                    PhosphorIconView(
                        icon = PhosphorIcon.TERMINAL,
                        color = SandboxrColors.TextSecondaryDark,
                        size = 18.dp
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onSearchQueryChange("") },
                            contentAlignment = Alignment.Center
                        ) {
                            PhosphorIconView(
                                icon = PhosphorIcon.X,
                                color = SandboxrColors.TextSecondaryDark,
                                size = 16.dp
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SandboxrColors.PrimaryAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                    focusedTextColor = SandboxrColors.TextPrimaryDark,
                    unfocusedTextColor = SandboxrColors.TextPrimaryDark,
                    focusedContainerColor = SandboxrColors.SurfaceLevel1Dark,
                    unfocusedContainerColor = SandboxrColors.SurfaceLevel1Dark
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            )

            // App Drawer Button (All Apps)
            Box(
                modifier = Modifier
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SandboxrColors.SurfaceLevel1Dark)
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenDrawer)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PhosphorIconView(
                        icon = PhosphorIcon.GRID,
                        color = SandboxrColors.PrimaryAccent,
                        size = 16.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Apps",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SandboxrColors.TextPrimaryDark
                    )
                }
            }

            // Clone Button if in virtual container
            if (activeEnvironment != null && !activeEnvironment.isSystem) {
                Box(
                    modifier = Modifier
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SandboxrColors.SurfaceLevel1Dark)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onCloneClick)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PhosphorIconView(
                            icon = PhosphorIcon.COPY,
                            color = SandboxrColors.PrimaryAccent,
                            size = 16.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Clone",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SandboxrColors.TextPrimaryDark
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Subtitle Info Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SandboxrSpacingTokens.Normal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val envName = activeEnvironment?.displayName ?: "System"
            Text(
                text = "$envName (${apps.size} installed)",
                style = SandboxrTheme.typography.label,
                color = SandboxrColors.TextSecondaryDark
            )

            if (activeEnvironment != null && !activeEnvironment.isSystem) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Security pill: GMS
                    val gmsLabel = if (activeEnvironment.gmsEnabled) "GMS: PASS" else "GMS: CUTOFF"
                    val gmsColor = if (activeEnvironment.gmsEnabled) SandboxrColors.Warning else SandboxrColors.Success
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(gmsColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = gmsLabel,
                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                            fontSize = 9.sp,
                            color = gmsColor
                        )
                    }

                    // Security pill: Network
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SandboxrColors.SurfaceLevel2Dark)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "NET: ${activeEnvironment.networkConfig.name}",
                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                            fontSize = 9.sp,
                            color = SandboxrColors.TextMonospaceDark
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 3. App Grid / Empty State ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = SandboxrColors.PrimaryAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                apps.isEmpty() && activeEnvironment != null && !activeEnvironment.isSystem -> {
                    // Empty Virtual Container Slate
                    EmptyContainerState(
                        environment = activeEnvironment,
                        onCloneClick = onCloneClick,
                        onInstallApkClick = {
                            val intent = Intent(context, InstallerActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }

                apps.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching apps found" else "No applications installed",
                            style = SandboxrTheme.typography.body,
                            color = SandboxrColors.TextSecondaryDark
                        )
                    }
                }

                else -> {
                    AppIconGrid(
                        apps = apps,
                        contentPadding = PaddingValues(
                            start = SandboxrSpacingTokens.Normal,
                            end = SandboxrSpacingTokens.Normal,
                            top = 8.dp,
                            bottom = 90.dp // Clear bottom floating navigation bar
                        ),
                        onAppClick = onAppClick,
                        onAppLongClick = { app -> selectedAppForMenu = app }
                    )
                }
            }
        }
    }

    // App Options Sheet on Long Click
    selectedAppForMenu?.let { app ->
        AppOptionsSheet(
            app = app,
            onDismissRequest = { selectedAppForMenu = null },
            onLaunch = {
                onAppClick(app)
                selectedAppForMenu = null
            },
            onAppInfo = {
                if (app.isSystemApp) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                    }
                    context.startActivity(intent)
                }
                selectedAppForMenu = null
            },
            onUninstall = {
                onUninstallApp(app.packageName, app.envId)
                selectedAppForMenu = null
            }
        )
    }
}

/**
 * Clean Liquid Glass empty state card when an isolated container has 0 apps.
 */
@Composable
private fun EmptyContainerState(
    environment: EnvironmentEntity,
    onCloneClick: () -> Unit,
    onInstallApkClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val shape = RoundedCornerShape(20.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(SandboxrColors.SurfaceLevel1Dark)
                .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(SandboxrColors.PrimaryAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorIconView(
                        icon = PhosphorIcon.BOX,
                        color = SandboxrColors.PrimaryAccent,
                        size = 28.dp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Pure Slate Sandbox",
                    style = SandboxrTheme.typography.title,
                    color = SandboxrColors.TextPrimaryDark
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "This container has zero OEM telemetry and zero bloatware. Install an APK directly or clone an app from System.",
                    style = SandboxrTheme.typography.body.copy(fontSize = 13.sp),
                    color = SandboxrColors.TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onCloneClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SandboxrColors.TextPrimaryDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Clone System App", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onInstallApkClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SandboxrColors.PrimaryAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Install APK", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * App Actions Sheet on long-press.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppOptionsSheet(
    app: AppItem,
    onDismissRequest: () -> Unit,
    onLaunch: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = SandboxrColors.SurfaceLevel1Dark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = app.label,
                style = SandboxrTheme.typography.title,
                color = SandboxrColors.TextPrimaryDark
            )
            Text(
                text = app.packageName,
                fontFamily = SandboxrFontFamilies.JetBrainsMono,
                fontSize = 12.sp,
                color = SandboxrColors.TextSecondaryDark,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            // Open
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onLaunch)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PhosphorIconView(icon = PhosphorIcon.CHECK, color = SandboxrColors.Success, size = 18.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Launch Application", color = SandboxrColors.TextPrimaryDark)
            }

            if (app.isSystemApp) {
                // App Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onAppInfo)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PhosphorIconView(icon = PhosphorIcon.GEAR, color = SandboxrColors.PrimaryAccent, size = 18.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("App Info & OS Permissions", color = SandboxrColors.TextPrimaryDark)
                }
            } else {
                // Uninstall from container
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onUninstall)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PhosphorIconView(icon = PhosphorIcon.TRASH, color = SandboxrColors.Danger, size = 18.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Uninstall from this Container", color = SandboxrColors.Danger)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
