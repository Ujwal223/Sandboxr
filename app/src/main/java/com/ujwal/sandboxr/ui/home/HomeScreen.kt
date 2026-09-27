package com.ujwal.sandboxr.ui.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sandboxr.launcher.theme.SandboxrFontFamilies
import com.sandboxr.launcher.theme.SandboxrSpacingTokens
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.launcher.ui.AppIconGrid
import com.sandboxr.launcher.ui.AppIconItem
import com.sandboxr.launcher.ui.AppItem
import com.sandboxr.launcher.ui.PhosphorIcon
import com.sandboxr.launcher.ui.PhosphorIconView
import com.sandboxr.launcher.ui.rememberSandboxrHaptics
import com.ujwal.sandboxr.ui.dialogs.CloneAppSheet
import com.ujwal.sandboxr.ui.dialogs.CreateEnvironmentDialog
import com.ujwal.sandboxr.ui.dialogs.GrapheneHomeMenuDialog
import com.ujwal.sandboxr.ui.dialogs.HardwareProfileDialog
import com.ujwal.sandboxr.ui.dialogs.ProfileSwitcherDialog
import com.ujwal.sandboxr.ui.settings.HomeSettingsDialog
import com.ujwal.sandboxr.ui.viewmodel.LauncherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Root Composable for the SANDBOXR GrapheneOS-style Home Launcher.
 * Features:
 * - Live wallpaper backdrop with adjustable dimming scrim
 * - GrapheneOS At-a-Glance widget linking to system Clock & Calendar
 * - Workspace desktop grid with configurable columns & rows
 * - Profile switcher mechanism integrated directly into App Drawer header
 * - Complete GrapheneOS Home Settings customization dialog
 * - Long-press wallpaper context menu for Wallpaper, Home Settings & Profiles
 * - Swipe up for App Drawer & swipe down for notification shade
 */
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberSandboxrHaptics()

    val launcherSettings by viewModel.launcherSettings.collectAsState()
    val activeEnvironment by viewModel.activeEnvironment.collectAsState()
    val environments by viewModel.environments.collectAsState()
    val appCounts by viewModel.appCounts.collectAsState()
    val selectedProfileFilter by viewModel.selectedProfileFilter.collectAsState()

    val homeApps by viewModel.homeApps.collectAsState()
    val dockApps by viewModel.dockApps.collectAsState()
    val allApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()
    val isDrawerMode by viewModel.isDrawerMode.collectAsState()
    val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsState()
    val statusNotice by viewModel.statusNotice.collectAsState()

    val showHomeSettings by viewModel.showHomeSettings.collectAsState()
    val showProfileSwitcher by viewModel.showProfileSwitcher.collectAsState()
    val showHomeMenu by viewModel.showHomeMenu.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val cloneSheetEnv by viewModel.cloneSheetEnv.collectAsState()
    val hardwareProfileEnv by viewModel.hardwareProfileEnv.collectAsState()

    val showWallpaperStyle by viewModel.showWallpaperStyle.collectAsState()
    val showWidgetsDialog by viewModel.showWidgetsDialog.collectAsState()
    val showManageScreensDialog by viewModel.showManageScreensDialog.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()

    var selectedAppForMenu by remember { mutableStateOf<AppItem?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Load initial settings and apps
    LaunchedEffect(Unit) {
        viewModel.loadLauncherSettings(context)
        viewModel.refreshApps(context)
    }

    LaunchedEffect(activeEnvironment?.id) {
        val env = activeEnvironment
        if (env != null) {
            viewModel.loadAppsForEnvironment(context, env)
        }
        viewModel.refreshAllAppCounts(context)
    }

    // Auto-dismiss status notice after 3 seconds
    LaunchedEffect(statusNotice) {
        if (statusNotice != null) {
            delay(3000)
            viewModel.clearStatusNotice()
        }
    }

    val dimAlpha = launcherSettings.wallpaperDimming

    // Multi-page Workspace Pager State
    val pageCount = launcherSettings.pageCount.coerceAtLeast(1)
    val defaultPage = launcherSettings.defaultPageIndex.coerceIn(0, pageCount - 1)
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = defaultPage) { pageCount }

    // Reset pager to default page when active profile changes
    LaunchedEffect(activeEnvironment?.id) {
        if (pageCount > 1) {
            pagerState.scrollToPage(defaultPage)
        }
    }

    // Synchronize pager state with ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentPageIndex(pagerState.currentPage)
    }

    // Wallpaper Background Brush based on settings
    val wallpaperBrush = remember(launcherSettings.wallpaperPreset, dimAlpha) {
        when (launcherSettings.wallpaperPreset) {
            "pixel_dark" -> Brush.verticalGradient(
                listOf(Color(0xFF14171C), Color(0xFF222730), Color(0xFF0E1014))
            )
            "amoled_obsidian" -> Brush.verticalGradient(
                listOf(Color.Black, Color(0xFF050505), Color.Black)
            )
            "midnight_aurora" -> Brush.verticalGradient(
                listOf(Color(0xFF0B1021), Color(0xFF1A1B4B), Color(0xFF090A0F))
            )
            "cyber_grid" -> Brush.verticalGradient(
                listOf(Color(0xFF1A1D20), Color(0xFF0F1113), Color(0xFF08090A))
            )
            else -> Brush.verticalGradient(
                listOf(
                    Color.Black.copy(alpha = dimAlpha * 0.7f),
                    Color.Transparent,
                    Color.Black.copy(alpha = dimAlpha)
                )
            )
        }
    }

    // Root Container over Wallpaper
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(wallpaperBrush)
            .pointerInput(isDrawerMode, launcherSettings) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -35 && !isDrawerMode) {
                        if (launcherSettings.swipeUpForDrawer) {
                            haptics.onCardPress()
                            viewModel.setDrawerMode(true)
                        }
                    } else if (dragAmount > 35) {
                        if (isDrawerMode) {
                            viewModel.setDrawerMode(false)
                        } else if (launcherSettings.swipeDownForNotifications) {
                            haptics.onCardPress()
                            viewModel.expandNotifications(context)
                        }
                    }
                }
            }
            .pointerInput(isDrawerMode, launcherSettings) {
                if (!isDrawerMode) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (launcherSettings.doubleTapToLock) {
                                haptics.onUnlockFail()
                            }
                        },
                        onLongPress = {
                            haptics.onDragStart()
                            viewModel.openHomeMenu()
                        }
                    )
                }
            }
    ) {
        // Scrim Dimming Layer
        if (dimAlpha > 0f && launcherSettings.wallpaperPreset == "system_default") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
            )
        }

        if (!isDrawerMode) {
            // ── Home Screen Workspace (Desktop with Multi-Page Pager) ─────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top At-a-Glance & Quick Action Cards (Weather & AI)
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (launcherSettings.showAtAGlance) {
                        GrapheneAtAGlance(
                            isDefaultLauncher = isDefaultLauncher,
                            onRequestDefaultLauncher = { viewModel.requestDefaultLauncher(context) },
                            onTimeClick = { viewModel.openClock(context) },
                            onDateClick = { viewModel.openCalendar(context) }
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Sandboxr OS Privacy & Container Status Cards (Zero AI, Zero Google)
                    if (launcherSettings.showWidgets) {
                        SandboxrQuickStatusCards(
                            activeEnvName = activeEnvironment?.displayName ?: "System",
                            onEnvClick = { viewModel.openProfileSwitcher() },
                            onSecurityClick = { viewModel.openHomeSettings() }
                        )
                    }
                }

                // Desktop Workspace Horizontal Pager (multi-screen paging)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        // Apps mapped to this workspace page
                        val pageApps = remember(homeApps, page, pageCount) {
                            if (pageCount == 1) {
                                homeApps
                            } else {
                                val perPage = (homeApps.size / pageCount).coerceAtLeast(1)
                                val start = (page * perPage).coerceAtMost(homeApps.size)
                                val end = if (page == pageCount - 1) homeApps.size else ((page + 1) * perPage).coerceAtMost(homeApps.size)
                                homeApps.subList(start, end)
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (pageApps.isNotEmpty()) {
                                AppIconGrid(
                                    apps = pageApps,
                                    columns = GridCells.Fixed(launcherSettings.gridColumns),
                                    showLabel = launcherSettings.showHomeLabels,
                                    iconShape = launcherSettings.iconShape,
                                    isThemed = launcherSettings.themedIcons,
                                    onAppClick = { app ->
                                        haptics.onCardPress()
                                        viewModel.launchApp(context, app)
                                    },
                                    onAppLongClick = { app ->
                                        haptics.onDragStart()
                                        selectedAppForMenu = app
                                    }
                                )
                            } else if (!isLoadingApps) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PhosphorIconView(
                                        icon = PhosphorIcon.GRID,
                                        color = SandboxrTheme.colors.textSecondary.copy(alpha = 0.5f),
                                        size = 32.dp
                                    )
                                    Text(
                                        text = if (page == defaultPage) "Swipe up for all applications" else "Screen ${page + 1} (Empty)",
                                        fontFamily = SandboxrFontFamilies.Inter,
                                        fontSize = 13.sp,
                                        color = SandboxrTheme.colors.textSecondary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Workspace Page Indicator Dots (with Home glyph on default page)
                if (pageCount > 1) {
                    WorkspacePageIndicator(
                        pageCount = pageCount,
                        currentPage = pagerState.currentPage,
                        defaultPage = defaultPage,
                        onPageClick = { targetPage ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(targetPage)
                            }
                        }
                    )
                }

                // Bottom Hotseat Dock + Quick Search Bar (NO Google 'G' Logo)
                GrapheneHotseat(
                    dockApps = dockApps.take(launcherSettings.hotseatIconCount),
                    showDockSearchBar = launcherSettings.showDockSearchBar,
                    iconShape = launcherSettings.iconShape,
                    isThemed = launcherSettings.themedIcons,
                    onAppClick = { app ->
                        haptics.onCardPress()
                        viewModel.launchApp(context, app)
                    },
                    onAppLongClick = { app ->
                        haptics.onDragStart()
                        selectedAppForMenu = app
                    },
                    onOpenDrawer = {
                        haptics.onCardPress()
                        viewModel.setDrawerMode(true)
                    },
                    onOpenSearch = {
                        haptics.onCardPress()
                        viewModel.setDrawerMode(true)
                    },
                    onVoiceSearch = {
                        haptics.onCardPress()
                        viewModel.openVoiceSearchForActiveProfile(context)
                    },
                    onCamera = {
                        haptics.onCardPress()
                        viewModel.openCameraForActiveProfile(context)
                    }
                )
            }
        }

        // ── Full-Screen App Drawer (Swipe Up / Tap) ──────────────────────────
        AnimatedVisibility(
            visible = isDrawerMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SandboxrTheme.colors.background.copy(alpha = 0.96f))
            ) {
                AllAppsDrawer(
                    apps = allApps,
                    searchQuery = searchQuery,
                    isLoading = isLoadingApps,
                    activeEnv = activeEnvironment,
                    selectedFilterEnvId = selectedProfileFilter,
                    onOpenProfileSwitcher = { viewModel.openProfileSwitcher() },
                    onSearchQueryChange = viewModel::setSearchQuery,
                    onAppClick = { app ->
                        haptics.onCardPress()
                        viewModel.launchApp(context, app)
                    },
                    onCloseDrawer = { viewModel.setDrawerMode(false) },
                    onUninstallApp = { pkg, envId ->
                        viewModel.uninstallVirtualApp(context, pkg, envId)
                    },
                    onOpenCloneSheet = {
                        val env = activeEnvironment
                        if (env != null && !env.isSystem) {
                            viewModel.openCloneSheet(env)
                        } else {
                            val firstSandbox = environments.firstOrNull { !it.isSystem }
                            if (firstSandbox != null) {
                                viewModel.openCloneSheet(firstSandbox)
                            } else {
                                viewModel.openCreateDialog()
                            }
                        }
                    },
                    onVoiceSearch = {
                        haptics.onCardPress()
                        viewModel.openVoiceSearchForActiveProfile(context)
                    },
                    onCamera = {
                        haptics.onCardPress()
                        viewModel.openCameraForActiveProfile(context)
                    },
                    showAppLabels = launcherSettings.showDrawerLabels,
                    showSearchBar = launcherSettings.showDrawerSearchBar,
                    columnsCount = launcherSettings.drawerColumns,
                    iconShape = launcherSettings.iconShape,
                    isThemed = launcherSettings.themedIcons
                )
            }
        }

        // ── Ephemeral Notice Toast ────────────────────────────────────────────
        AnimatedVisibility(
            visible = statusNotice != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            statusNotice?.let { notice ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SandboxrTheme.colors.surface3.copy(alpha = 0.95f))
                        .border(1.dp, SandboxrTheme.colors.primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = notice,
                        fontFamily = SandboxrFontFamilies.JetBrainsMono,
                        fontSize = 12.sp,
                        color = SandboxrTheme.colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ── App Long-Press Context Menu Dialog ────────────────────────────────
        selectedAppForMenu?.let { app ->
            GrapheneAppContextDialog(
                app = app,
                isPinnedOnHome = homeApps.any { it.packageName == app.packageName && it.envId == app.envId },
                onDismiss = { selectedAppForMenu = null },
                onAppInfo = {
                    selectedAppForMenu = null
                    viewModel.openAppInfo(context, app)
                },
                onToggleHomePin = {
                    val isPinned = homeApps.any { it.packageName == app.packageName && it.envId == app.envId }
                    if (isPinned) {
                        viewModel.removeFromHome(app)
                    } else {
                        viewModel.addToHome(app)
                    }
                    selectedAppForMenu = null
                },
                onCloneToSandbox = {
                    selectedAppForMenu = null
                    val active = activeEnvironment
                    if (active != null && !active.isSystem) {
                        viewModel.cloneAppToEnvironment(context, app.packageName, active.id)
                    } else {
                        val firstSandbox = environments.find { !it.isSystem }
                        if (firstSandbox != null) {
                            viewModel.cloneAppToEnvironment(context, app.packageName, firstSandbox.id)
                        } else {
                            viewModel.openCreateDialog()
                        }
                    }
                },
                onUninstall = {
                    selectedAppForMenu = null
                    viewModel.uninstallVirtualApp(context, app.packageName, app.envId)
                }
            )
        }

        // ── GrapheneOS Long-Press Home Menu Dialog (matches 1.png) ───────────
        if (showHomeMenu) {
            GrapheneHomeMenuDialog(
                onDismissRequest = { viewModel.closeHomeMenu() },
                onOpenWallpaperStyle = { viewModel.openWallpaperStyle() },
                onOpenWidgets = { viewModel.openWidgetsDialog() },
                onOpenManageScreens = { viewModel.openManageScreensDialog() },
                onOpenHomeSettings = { viewModel.openHomeSettings() },
                onOpenProfileSwitcher = { viewModel.openProfileSwitcher() }
            )
        }

        // ── Wallpaper & Style Dialog (Wallpapers, AMOLED presets, shapes) ─────
        if (showWallpaperStyle) {
            com.ujwal.sandboxr.ui.settings.WallpaperStyleDialog(
                currentSettings = launcherSettings,
                onSaveSettings = { viewModel.updateLauncherSettings(context, it) },
                onDismissRequest = { viewModel.closeWallpaperStyle() }
            )
        }

        // ── Widgets Customization Dialog ──────────────────────────────────────
        if (showWidgetsDialog) {
            com.ujwal.sandboxr.ui.dialogs.WidgetsDialog(
                currentSettings = launcherSettings,
                onSaveSettings = { viewModel.updateLauncherSettings(context, it) },
                onDismissRequest = { viewModel.closeWidgetsDialog() }
            )
        }

        // ── Manage Workspace Screens Dialog (multi-screen carousel) ──────────
        if (showManageScreensDialog) {
            com.ujwal.sandboxr.ui.dialogs.ManageScreensDialog(
                pageCount = launcherSettings.pageCount,
                defaultPageIndex = launcherSettings.defaultPageIndex,
                currentPageIndex = pagerState.currentPage,
                onSetDefaultPage = { viewModel.setDefaultPage(context, it) },
                onAddPage = { viewModel.addWorkspacePage(context) },
                onRemovePage = { viewModel.removeWorkspacePage(context, it) },
                onSelectPage = { page ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                },
                onDismissRequest = { viewModel.closeManageScreensDialog() }
            )
        }

        // ── GrapheneOS Full Home Settings Dialog ─────────────────────────────
        if (showHomeSettings) {
            HomeSettingsDialog(
                currentSettings = launcherSettings,
                onSaveSettings = { viewModel.updateLauncherSettings(context, it) },
                onDismissRequest = { viewModel.closeHomeSettings() }
            )
        }

        // ── Profile Switcher Dialog (Triggered from App Drawer Count Pill) ───
        if (showProfileSwitcher) {
            ProfileSwitcherDialog(
                environments = environments,
                activeEnv = activeEnvironment,
                selectedFilterEnvId = selectedProfileFilter,
                appCounts = appCounts,
                onSelectProfile = { viewModel.setSelectedProfileFilter(context, it) },
                onCreateNewProfile = { viewModel.openCreateDialog() },
                onDismissRequest = { viewModel.closeProfileSwitcher() }
            )
        }

        // ── Subsystem Dialogs ────────────────────────────────────────────────
        if (showCreateDialog) {
            CreateEnvironmentDialog(
                onDismissRequest = { viewModel.closeCreateDialog() },
                onConfirm = { name, colorTag, iconType, netConfig, gms, clip ->
                    viewModel.createEnvironment(context, name, colorTag, iconType, netConfig, gms, clip)
                }
            )
        }

        cloneSheetEnv?.let { env ->
            CloneAppSheet(
                targetEnvironment = env,
                onDismissRequest = { viewModel.closeCloneSheet() },
                onCloneApp = { pkg ->
                    viewModel.cloneAppToEnvironment(context, pkg, env.id)
                }
            )
        }

        hardwareProfileEnv?.let { env ->
            HardwareProfileDialog(
                environment = env,
                onDismissRequest = { viewModel.closeHardwareProfile() },
                onCopyNotice = { viewModel.showNotice(it) }
            )
        }
    }
}

/**
 * Top At-a-Glance widget modeled after GrapheneOS / Pixel Launcher:
 * - Large, crisp digital clock (clicking opens Clock app)
 * - Date string (clicking opens Calendar app)
 * - Clean minimal indicator
 */
@Composable
private fun GrapheneAtAGlance(
    isDefaultLauncher: Boolean,
    onRequestDefaultLauncher: () -> Unit,
    onTimeClick: () -> Unit,
    onDateClick: () -> Unit
) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        if (currentTime.isNotEmpty()) {
            Text(
                text = currentTime,
                fontFamily = SandboxrFontFamilies.Inter,
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1.0).sp,
                color = SandboxrTheme.colors.textPrimary,
                modifier = Modifier.clickable(onClick = onTimeClick)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = currentDate,
                fontFamily = SandboxrFontFamilies.Inter,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = SandboxrTheme.colors.textSecondary,
                modifier = Modifier.clickable(onClick = onDateClick)
            )
        } else {
            Text(
                text = "SANDBOXR",
                fontFamily = SandboxrFontFamilies.Inter,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.0.sp,
                color = SandboxrTheme.colors.textPrimary
            )
        }

        if (!isDefaultLauncher) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SandboxrTheme.colors.surface2)
                    .border(1.dp, SandboxrTheme.colors.primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onRequestDefaultLauncher)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PhosphorIconView(
                    icon = PhosphorIcon.BOX,
                    color = SandboxrTheme.colors.primaryAccent,
                    size = 14.dp
                )
                Text(
                    text = "Tap to set as default Home launcher",
                    fontFamily = SandboxrFontFamilies.JetBrainsMono,
                    fontSize = 11.sp,
                    color = SandboxrTheme.colors.textPrimary
                )
            }
        }
    }
}

/**
 * Top OS Privacy & Sandbox Status Cards.
 * Completely local, zero AI, zero Google products.
 */
@Composable
private fun SandboxrQuickStatusCards(
    activeEnvName: String,
    onEnvClick: () -> Unit,
    onSecurityClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Active Sandbox Profile Card
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SandboxrTheme.colors.surface2.copy(alpha = 0.85f))
                .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(22.dp))
                .clickable(onClick = onEnvClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PhosphorIconView(
                icon = PhosphorIcon.BOX,
                color = SandboxrTheme.colors.primaryAccent,
                size = 18.dp
            )
            Column {
                Text(
                    text = activeEnvName,
                    fontFamily = SandboxrFontFamilies.Inter,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SandboxrTheme.colors.textPrimary
                )
                Text(
                    text = "Isolated Profile",
                    fontFamily = SandboxrFontFamilies.Inter,
                    fontSize = 10.sp,
                    color = SandboxrTheme.colors.textSecondary
                )
            }
        }

        // Local DNS Firewall / Privacy Shield Card
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SandboxrTheme.colors.surface2.copy(alpha = 0.85f))
                .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(22.dp))
                .clickable(onClick = onSecurityClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PhosphorIconView(
                icon = PhosphorIcon.SHIELD,
                color = SandboxrTheme.colors.primaryAccent,
                size = 18.dp
            )
            Column {
                Text(
                    text = "Firewall Active",
                    fontFamily = SandboxrFontFamilies.Inter,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SandboxrTheme.colors.textPrimary
                )
                Text(
                    text = "Local DNS Shield",
                    fontFamily = SandboxrFontFamilies.Inter,
                    fontSize = 10.sp,
                    color = SandboxrTheme.colors.textSecondary
                )
            }
        }
    }
}

/**
 * Workspace Page Indicator Dots with Home glyph for the default page.
 * Modeled after AOSP Launcher3 PageIndicatorDots.
 */
@Composable
private fun WorkspacePageIndicator(
    pageCount: Int,
    currentPage: Int,
    defaultPage: Int,
    onPageClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until pageCount) {
            val isCurrent = (i == currentPage)
            val isDefault = (i == defaultPage)

            if (isDefault) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isCurrent) SandboxrTheme.colors.primaryAccent else SandboxrTheme.colors.surface3)
                        .clickable { onPageClick(i) },
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorIconView(
                        icon = PhosphorIcon.HOME,
                        color = if (isCurrent) Color.Black else SandboxrTheme.colors.textSecondary,
                        size = 10.dp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (isCurrent) 16.dp else 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isCurrent) SandboxrTheme.colors.primaryAccent else SandboxrTheme.colors.surface3)
                        .clickable { onPageClick(i) }
                )
            }
        }
    }
}

/**
 * Bottom Hotseat Dock pinned with core apps and bottom Quick Search Bar.
 * Strictly NO Google "G" logo; uses privacy search glyph with mic & camera actions.
 */
@Composable
private fun GrapheneHotseat(
    dockApps: List<AppItem>,
    showDockSearchBar: Boolean,
    iconShape: String = "Circle",
    isThemed: Boolean = false,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onVoiceSearch: () -> Unit = {},
    onCamera: () -> Unit = {}
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hotseat Dock Container
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(SandboxrTheme.colors.surface1.copy(alpha = 0.85f))
                .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(32.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dockApps.forEach { app ->
                    AppIconItem(
                        app = app,
                        showLabel = false,
                        iconShape = iconShape,
                        isThemed = isThemed,
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) }
                    )
                }
            }
        }

        // Bottom Dock Quick Search Bar (Strictly NO Google "G" Logo)
        if (showDockSearchBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SandboxrTheme.colors.surface2.copy(alpha = 0.88f))
                    .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(24.dp))
                    .clickable(onClick = onOpenSearch)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Search Icon & Prompt
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PhosphorIconView(
                            icon = PhosphorIcon.SEARCH,
                            color = SandboxrTheme.colors.primaryAccent,
                            size = 18.dp
                        )
                        Text(
                            text = "Search apps, web, privacy...",
                            style = SandboxrTheme.typography.body.copy(fontSize = 13.sp),
                            color = SandboxrTheme.colors.textSecondary
                        )
                    }

                    // Right Mic & Camera Shortcuts (Scoped to active profile)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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

                        // Camera / Lens
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
        }
    }
}

/**
 * Contextual Action Popup Dialog on App Long-Press.
 */
@Composable
private fun GrapheneAppContextDialog(
    app: AppItem,
    isPinnedOnHome: Boolean,
    onDismiss: () -> Unit,
    onAppInfo: () -> Unit,
    onToggleHomePin: () -> Unit,
    onCloneToSandbox: () -> Unit,
    onUninstall: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(SandboxrTheme.colors.surface1)
                .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: App Label & Package
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SandboxrTheme.colors.surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = app.iconBitmap
                        if (icon != null) {
                            androidx.compose.foundation.Image(
                                bitmap = icon,
                                contentDescription = app.label,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Text(
                                text = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                fontFamily = SandboxrFontFamilies.Inter,
                                fontWeight = FontWeight.Bold,
                                color = SandboxrTheme.colors.textPrimary,
                                fontSize = 18.sp
                            )
                        }
                    }
                    Column {
                        Text(
                            text = app.label,
                            fontFamily = SandboxrFontFamilies.Inter,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SandboxrTheme.colors.textPrimary
                        )
                        Text(
                            text = app.packageName,
                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                            fontSize = 10.sp,
                            color = SandboxrTheme.colors.textSecondary
                        )
                    }
                }

                // Action: App Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SandboxrTheme.colors.surface2)
                        .clickable(onClick = onAppInfo)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PhosphorIconView(
                        icon = PhosphorIcon.TERMINAL,
                        color = SandboxrTheme.colors.primaryAccent,
                        size = 18.dp
                    )
                    Text(
                        text = "App info",
                        fontFamily = SandboxrFontFamilies.Inter,
                        fontSize = 14.sp,
                        color = SandboxrTheme.colors.textPrimary
                    )
                }

                // Action: Add / Remove from Home
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SandboxrTheme.colors.surface2)
                        .clickable(onClick = onToggleHomePin)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PhosphorIconView(
                        icon = if (isPinnedOnHome) PhosphorIcon.TRASH else PhosphorIcon.PLUS,
                        color = SandboxrTheme.colors.textPrimary,
                        size = 18.dp
                    )
                    Text(
                        text = if (isPinnedOnHome) "Remove from Home screen" else "Add to Home screen",
                        fontFamily = SandboxrFontFamilies.Inter,
                        fontSize = 14.sp,
                        color = SandboxrTheme.colors.textPrimary
                    )
                }

                // Action: Clone to Sandbox (if host app)
                if (app.isSystemApp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SandboxrTheme.colors.surface2)
                            .clickable(onClick = onCloneToSandbox)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PhosphorIconView(
                            icon = PhosphorIcon.BOX,
                            color = SandboxrTheme.colors.primaryAccent,
                            size = 18.dp
                        )
                        Text(
                            text = "Clone to Sandbox container",
                            fontFamily = SandboxrFontFamilies.Inter,
                            fontSize = 14.sp,
                            color = SandboxrTheme.colors.textPrimary
                        )
                    }
                } else {
                    // Action: Uninstall from Sandbox (if virtual app)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SandboxrTheme.colors.danger.copy(alpha = 0.15f))
                            .clickable(onClick = onUninstall)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PhosphorIconView(
                            icon = PhosphorIcon.TRASH,
                            color = SandboxrTheme.colors.danger,
                            size = 18.dp
                        )
                        Text(
                            text = "Uninstall from Sandbox",
                            fontFamily = SandboxrFontFamilies.Inter,
                            fontSize = 14.sp,
                            color = SandboxrTheme.colors.danger
                        )
                    }
                }
            }
        }
    }
}
