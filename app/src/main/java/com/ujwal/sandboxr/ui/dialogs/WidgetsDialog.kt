package com.ujwal.sandboxr.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.window.Dialog
import com.sandboxr.launcher.theme.SandboxrFontFamilies
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.launcher.ui.PhosphorIcon
import com.sandboxr.launcher.ui.PhosphorIconView
import com.ujwal.sandboxr.ui.settings.LauncherSettings

/**
 * Widgets Picker & Configuration Dialog matching Android 14/15 Pixel & AOSP widget options.
 */
@Composable
fun WidgetsDialog(
    currentSettings: LauncherSettings,
    onSaveSettings: (LauncherSettings) -> Unit,
    onDismissRequest: () -> Unit
) {
    var settings by remember { mutableStateOf(currentSettings) }

    Dialog(onDismissRequest = {
        onSaveSettings(settings)
        onDismissRequest()
    }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SandboxrTheme.colors.surface1)
                .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WIDGETS",
                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = SandboxrTheme.colors.primaryAccent
                        )
                        Text(
                            text = "Configure desktop widgets & At-a-Glance",
                            fontFamily = SandboxrFontFamilies.Inter,
                            fontSize = 12.sp,
                            color = SandboxrTheme.colors.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SandboxrTheme.colors.surface2)
                            .clickable {
                                onSaveSettings(settings)
                                onDismissRequest()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorIconView(
                            icon = PhosphorIcon.X,
                            color = SandboxrTheme.colors.textSecondary,
                            size = 14.dp
                        )
                    }
                }

                // Widget Toggles List
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 1. At-a-Glance
                    WidgetToggleRow(
                        title = "At-a-Glance Header",
                        subtitle = "Displays current day, date, time & calendar shortcut",
                        icon = PhosphorIcon.CLOUD_SUN,
                        checked = settings.showAtAGlance,
                        onCheckedChange = { settings = settings.copy(showAtAGlance = it) }
                    )

                    // 2. Sandboxr Status Cards
                    WidgetToggleRow(
                        title = "Sandbox & Privacy Widgets",
                        subtitle = "Active isolated profile & DNS firewall cards on Desktop",
                        icon = PhosphorIcon.SHIELD,
                        checked = settings.showWidgets,
                        onCheckedChange = { settings = settings.copy(showWidgets = it) }
                    )

                    // 3. Bottom Dock Search Bar
                    WidgetToggleRow(
                        title = "Dock Quick Search Bar",
                        subtitle = "Privacy search pill below hotseat dock with Voice & Camera shortcuts",
                        icon = PhosphorIcon.SEARCH,
                        checked = settings.showDockSearchBar,
                        onCheckedChange = { settings = settings.copy(showDockSearchBar = it) }
                    )
                }

                // Done Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SandboxrTheme.colors.primaryAccent)
                        .clickable {
                            onSaveSettings(settings)
                            onDismissRequest()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save & Apply Widgets",
                        fontFamily = SandboxrFontFamilies.Inter,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SandboxrTheme.colors.surface1
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetToggleRow(
    title: String,
    subtitle: String,
    icon: PhosphorIcon,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SandboxrTheme.colors.surface2)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    fontSize = 13.sp,
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

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SandboxrTheme.colors.surface1,
                checkedTrackColor = SandboxrTheme.colors.primaryAccent,
                uncheckedThumbColor = SandboxrTheme.colors.textSecondary,
                uncheckedTrackColor = SandboxrTheme.colors.surface1
            )
        )
    }
}
