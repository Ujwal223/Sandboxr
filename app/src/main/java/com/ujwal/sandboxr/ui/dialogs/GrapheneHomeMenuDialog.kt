package com.ujwal.sandboxr.ui.dialogs

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sandboxr.launcher.theme.SandboxrFontFamilies
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.launcher.ui.PhosphorIcon
import com.sandboxr.launcher.ui.PhosphorIconView

/**
 * GrapheneOS / AOSP Launcher3 Home Workspace Long-Press Context Menu.
 * Displayed when user long-presses wallpaper / empty space on Desktop.
 */
@Composable
fun GrapheneHomeMenuDialog(
    onDismissRequest: () -> Unit,
    onOpenWallpaperStyle: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenManageScreens: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onOpenProfileSwitcher: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SandboxrTheme.colors.surface1)
                .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Item 1: Wallpaper & style (from 1.png)
                HomeMenuItem(
                    title = "Wallpaper & style",
                    subtitle = "Wallpapers, icon shapes, and themed accents",
                    icon = PhosphorIcon.PALETTE,
                    onClick = {
                        onDismissRequest()
                        onOpenWallpaperStyle()
                    }
                )

                // Item 2: Widgets (from 1.png)
                HomeMenuItem(
                    title = "Widgets",
                    subtitle = "Weather, AI Assist, and At-a-Glance widgets",
                    icon = PhosphorIcon.WIDGETS,
                    onClick = {
                        onDismissRequest()
                        onOpenWidgets()
                    }
                )

                // Item 3: Manage Screens (from Launcher3 / PagedView)
                HomeMenuItem(
                    title = "Manage screens",
                    subtitle = "Add, remove, or set default Home screen",
                    icon = PhosphorIcon.GRID,
                    onClick = {
                        onDismissRequest()
                        onOpenManageScreens()
                    }
                )

                // Item 4: Home settings (from 1.png)
                HomeMenuItem(
                    title = "Home settings",
                    subtitle = "Grid layout, drawer, gestures, and privacy",
                    icon = PhosphorIcon.GEAR,
                    onClick = {
                        onDismissRequest()
                        onOpenHomeSettings()
                    }
                )

                // Item 5: Sandbox Profiles
                HomeMenuItem(
                    title = "Sandbox Profiles",
                    subtitle = "Switch or create container environments",
                    icon = PhosphorIcon.SHIELD,
                    onClick = {
                        onDismissRequest()
                        onOpenProfileSwitcher()
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeMenuItem(
    title: String,
    subtitle: String,
    icon: PhosphorIcon,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SandboxrTheme.colors.surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SandboxrTheme.colors.surface1),
            contentAlignment = Alignment.Center
        ) {
            PhosphorIconView(
                icon = icon,
                color = SandboxrTheme.colors.primaryAccent,
                size = 18.dp
            )
        }
        Column {
            Text(
                text = title,
                fontFamily = SandboxrFontFamilies.Inter,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SandboxrTheme.colors.textPrimary
            )
            Text(
                text = subtitle,
                fontFamily = SandboxrFontFamilies.Inter,
                fontSize = 11.sp,
                color = SandboxrTheme.colors.textSecondary
            )
        }
    }
}
