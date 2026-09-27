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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ujwal.sandboxr.data.db.EnvironmentEntity
import com.sandboxr.launcher.theme.SandboxrFontFamilies
import com.sandboxr.launcher.theme.SandboxrSpacingTokens
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.launcher.ui.AppIconGrid
import com.sandboxr.launcher.ui.AppItem
import com.sandboxr.launcher.ui.PhosphorIcon
import com.sandboxr.launcher.ui.PhosphorIconView
import com.ujwal.sandboxr.ui.dialogs.DrawerProfileSwitcherPill

/**
 * GrapheneOS-style All Apps Drawer.
 *
 * Capabilities:
 * - Real-time fuzzy app search bar
 * - Profile Switcher mechanism located in header (in place of static app count)
 * - Renders superellipse squircle icons on 72dp touch targets
 * - Dynamic light/dark theme adaptation matching device system theme
 */
import android.speech.RecognizerIntent
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.HorizontalDivider
import com.sandboxr.launcher.ui.AppIconItem
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Pixel Launcher & GrapheneOS-style All Apps Drawer.
 *
 * Capabilities:
 * - Authentic Pixel-style Google search bar with Voice & Lens shortcuts
 * - Minimized, intuitive Profile Switcher avatar pill
 * - Top Suggested Apps row with divider matching Pixel Launcher
 * - Alphabetical A-Z fast scroller on the right margin
 * - Dynamically adapts to user's icon shape (Circle, Squircle, Rounded Square, etc.)
 * - Zero lag, 120Hz smooth scrolling
 */
@Composable
fun AllAppsDrawer(
    apps: List<AppItem>,
    searchQuery: String,
    isLoading: Boolean,
    activeEnv: EnvironmentEntity?,
    selectedFilterEnvId: String?,
    onOpenProfileSwitcher: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAppClick: (AppItem) -> Unit,
    onCloseDrawer: () -> Unit,
    onUninstallApp: (packageName: String, envId: String) -> Unit,
    onOpenCloneSheet: () -> Unit = {},
    onVoiceSearch: () -> Unit = {},
    onCamera: () -> Unit = {},
    showAppLabels: Boolean = true,
    showSearchBar: Boolean = true,
    columnsCount: Int = 5,
    iconShape: String = "Circle",
    isThemed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    var selectedAppForMenu by remember { mutableStateOf<AppItem?>(null) }
    var activeFastScrollLetter by remember { mutableStateOf<Char?>(null) }

    // Distinct alphabetical list of apps
    val sortedApps = remember(apps) {
        apps.sortedBy { it.label.lowercase() }
    }

    // Top predicted / suggested apps (first 5 or commonly launched apps)
    val predictedApps = remember(apps) {
        if (apps.size >= 5) apps.take(5) else apps
    }

    // Alphabet index map for rapid jump
    val alphabetMap = remember(sortedApps) {
        val map = mutableMapOf<Char, Int>()
        sortedApps.forEachIndexed { index, app ->
            val firstChar = app.label.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (firstChar in 'A'..'Z') firstChar else '#'
            if (!map.containsKey(key)) {
                map[key] = index
            }
        }
        map
    }

    val alphabetList = remember {
        listOf('#') + ('A'..'Z').toList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top Pixel Search Bar & Profile Avatar Header ───────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Close / Back button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SandboxrTheme.colors.surface1)
                    .border(1.dp, SandboxrTheme.colors.glassBorder, CircleShape)
                    .clickable(onClick = onCloseDrawer),
                contentAlignment = Alignment.Center
            ) {
                PhosphorIconView(
                    icon = PhosphorIcon.ARROW_LEFT,
                    color = SandboxrTheme.colors.textPrimary,
                    size = 18.dp
                )
            }

            // Pixel-style Search Pill
            if (showSearchBar) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SandboxrTheme.colors.surface2)
                        .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Privacy-friendly Search Glyph
                        PhosphorIconView(
                            icon = PhosphorIcon.SEARCH,
                            color = SandboxrTheme.colors.primaryAccent,
                            size = 18.dp
                        )

                        // Editable Query / Placeholder
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = {
                                Text(
                                    text = "Search apps & more...",
                                    style = SandboxrTheme.typography.body.copy(fontSize = 13.sp),
                                    color = SandboxrTheme.colors.textSecondary
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = SandboxrTheme.colors.textPrimary,
                                unfocusedTextColor = SandboxrTheme.colors.textPrimary,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )

                        // Action icons on right
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
                                    color = SandboxrTheme.colors.textSecondary,
                                    size = 14.dp
                                )
                            }
                        } else {
                            // Mic (Voice Search)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onVoiceSearch),
                                contentAlignment = Alignment.Center
                            ) {
                                PhosphorIconView(
                                    icon = PhosphorIcon.MIC,
                                    color = SandboxrTheme.colors.textPrimary,
                                    size = 16.dp
                                )
                            }

                            // Camera shortcut
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onCamera),
                                contentAlignment = Alignment.Center
                            ) {
                                PhosphorIconView(
                                    icon = PhosphorIcon.CAMERA,
                                    color = SandboxrTheme.colors.textPrimary,
                                    size = 16.dp
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Minimized, highly intuitive Profile Avatar Switcher Pill
            DrawerProfileSwitcherPill(
                activeEnv = activeEnv,
                selectedFilterEnvId = selectedFilterEnvId,
                appCount = apps.size,
                onClick = onOpenProfileSwitcher
            )
        }

        // ── Main Drawer Content Area with Fast Scroller ──────────────────────────
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
                            color = SandboxrTheme.colors.primaryAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                sortedApps.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                Text(
                                    text = "No apps matching \"$searchQuery\"",
                                    style = SandboxrTheme.typography.title,
                                    color = SandboxrTheme.colors.textPrimary
                                )
                                Text(
                                    text = "Try a different search term or search the web",
                                    style = SandboxrTheme.typography.body,
                                    color = SandboxrTheme.colors.textSecondary
                                )
                            } else if ((selectedFilterEnvId != null && selectedFilterEnvId != "system") || (activeEnv != null && !activeEnv.isSystem)) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(SandboxrTheme.colors.surface2),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PhosphorIconView(
                                        icon = PhosphorIcon.BOX,
                                        color = SandboxrTheme.colors.primaryAccent,
                                        size = 28.dp
                                    )
                                }
                                Text(
                                    text = "No apps in this sandbox",
                                    style = SandboxrTheme.typography.title,
                                    color = SandboxrTheme.colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Clone apps from your personal profile or install APKs to run isolated in this profile.",
                                    style = SandboxrTheme.typography.body,
                                    color = SandboxrTheme.colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = onOpenCloneSheet,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SandboxrTheme.colors.primaryAccent
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PhosphorIconView(
                                            icon = PhosphorIcon.PLUS,
                                            color = Color.White,
                                            size = 16.dp
                                        )
                                        Text(
                                            text = "Clone App to Sandbox",
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "No applications found",
                                    style = SandboxrTheme.typography.body,
                                    color = SandboxrTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Two-pane: Main Grid on Left, Fast Scroller A-Z on Right
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            // Top Predicted / Suggested Apps Row (matching Pixel Launcher uploaded image)
                            if (searchQuery.isEmpty() && predictedApps.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        predictedApps.take(5).forEach { app ->
                                            AppIconItem(
                                                app = app,
                                                iconShape = iconShape,
                                                isThemed = isThemed,
                                                showLabel = showAppLabels,
                                                onClick = { onAppClick(app) },
                                                onLongClick = { selectedAppForMenu = app }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(
                                        color = SandboxrTheme.colors.glassBorder.copy(alpha = 0.5f),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }

                            // Full Alphabetical App Grid
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(columnsCount),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(
                                    start = 12.dp,
                                    end = 8.dp,
                                    top = 6.dp,
                                    bottom = 24.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(
                                    SandboxrSpacingTokens.AppGridHorizontalSpacing,
                                    Alignment.CenterHorizontally
                                ),
                                verticalArrangement = Arrangement.spacedBy(SandboxrSpacingTokens.AppGridVerticalSpacing)
                            ) {
                                items(
                                    items = sortedApps,
                                    key = { "${it.envId}_${it.packageName}" },
                                    contentType = { "app_item" }
                                ) { app ->
                                    AppIconItem(
                                        app = app,
                                        iconShape = iconShape,
                                        isThemed = isThemed,
                                        showLabel = showAppLabels,
                                        onClick = { onAppClick(app) },
                                        onLongClick = { selectedAppForMenu = app }
                                    )
                                }
                            }
                        }

                        // Right Margin A-Z Fast Scroller (Pixel Launcher style)
                        if (searchQuery.isEmpty() && sortedApps.size > 15) {
                            Column(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxSize()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                alphabetList.forEach { letter ->
                                    val isAvailable = alphabetMap.containsKey(letter)
                                    Text(
                                        text = letter.toString(),
                                        fontFamily = SandboxrFontFamilies.Inter,
                                        fontSize = 9.sp,
                                        fontWeight = if (isAvailable) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isAvailable) SandboxrTheme.colors.primaryAccent else SandboxrTheme.colors.textSecondary.copy(alpha = 0.35f),
                                        modifier = Modifier
                                            .clickable(enabled = isAvailable) {
                                                activeFastScrollLetter = letter
                                                alphabetMap[letter]?.let { targetIndex ->
                                                    coroutineScope.launch {
                                                        gridState.scrollToItem(targetIndex)
                                                    }
                                                }
                                            }
                                            .padding(vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Active Letter Floating Bubble
                    activeFastScrollLetter?.let { letter ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SandboxrTheme.colors.surface3)
                                .border(2.dp, SandboxrTheme.colors.primaryAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter.toString(),
                                fontFamily = SandboxrFontFamilies.Inter,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = SandboxrTheme.colors.primaryAccent
                            )
                        }
                    }
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
