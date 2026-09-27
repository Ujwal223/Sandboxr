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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ujwal.sandboxr.data.db.EnvironmentEntity
import com.sandboxr.launcher.theme.SandboxrColors
import com.sandboxr.launcher.theme.SandboxrFontFamilies
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.launcher.ui.PhosphorIcon
import com.sandboxr.launcher.ui.PhosphorIconView

/**
 * Compact Profile Switcher Pill placed in the App Drawer header
 * (in place of the static app count).
 */
@Composable
fun DrawerProfileSwitcherPill(
    activeEnv: EnvironmentEntity?,
    selectedFilterEnvId: String?,
    appCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMerged = selectedFilterEnvId == null
    val isWork = activeEnv?.id == EnvironmentEntity.WORK_PROFILE_ENV_ID
    val isPersonal = (activeEnv?.isSystem == true || activeEnv?.id == EnvironmentEntity.SYSTEM_ENV_ID) && !isWork
    val badgeColor = when {
        isMerged -> SandboxrTheme.colors.primaryAccent
        isWork -> Color(0xFF2E7D32)
        isPersonal -> SandboxrTheme.colors.primaryAccent
        activeEnv != null -> Color(activeEnv.colorTag)
        else -> SandboxrTheme.colors.primaryAccent
    }

    val icon = when {
        isMerged -> PhosphorIcon.GRID
        isWork -> PhosphorIcon.BOX
        isPersonal -> PhosphorIcon.USER
        else -> PhosphorIcon.SHIELD
    }

    val initials = when {
        isMerged -> "ALL"
        isWork -> "W"
        isPersonal -> "P"
        activeEnv != null -> activeEnv.displayName.take(2).uppercase()
        else -> "P"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = SandboxrTheme.colors.surface2,
        border = BorderStroke(1.5.dp, badgeColor.copy(alpha = 0.85f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                PhosphorIconView(
                    icon = icon,
                    color = badgeColor,
                    size = 14.dp
                )
            }
            Text(
                text = initials,
                fontFamily = SandboxrFontFamilies.JetBrainsMono,
                fontSize = 11.sp,
                color = SandboxrTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            PhosphorIconView(
                icon = PhosphorIcon.CARET_DOWN,
                color = SandboxrTheme.colors.textSecondary,
                size = 10.dp
            )
        }
    }
}

/**
 * Modal Sheet for switching active user / sandbox profile.
 */
@Composable
fun ProfileSwitcherDialog(
    environments: List<EnvironmentEntity>,
    activeEnv: EnvironmentEntity?,
    selectedFilterEnvId: String?,
    appCounts: Map<String, Int>,
    onSelectProfile: (String?) -> Unit,
    onCreateNewProfile: () -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
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
                            text = "Profiles",
                            style = SandboxrTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = SandboxrTheme.colors.textPrimary
                        )
                        Text(
                            text = "Filter applications by isolated profile",
                            style = SandboxrTheme.typography.body,
                            color = SandboxrTheme.colors.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SandboxrTheme.colors.surface2)
                            .clickable(onClick = onDismissRequest),
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorIconView(
                            icon = PhosphorIcon.X,
                            color = SandboxrTheme.colors.textPrimary,
                            size = 16.dp
                        )
                    }
                }

                // Profile Items List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: All Apps (Merged view)
                    item {
                        ProfileSelectCard(
                            title = "All Applications",
                            subtitle = "Unified view across personal & all sandboxes",
                            color = SandboxrTheme.colors.primaryAccent,
                            icon = PhosphorIcon.GRID,
                            isSelected = selectedFilterEnvId == null,
                            count = appCounts.values.sum(),
                            onClick = {
                                onSelectProfile(null)
                                onDismissRequest()
                            }
                        )
                    }

                    // Option 2..N: Specific Environments (Personal + Work Profile + Sandboxes)
                    items(environments) { env ->
                        val isSelected = selectedFilterEnvId == env.id
                        val count = appCounts[env.id] ?: 0
                        val isWork = env.id == EnvironmentEntity.WORK_PROFILE_ENV_ID
                        val isPersonal = (env.isSystem || env.id == EnvironmentEntity.SYSTEM_ENV_ID) && !isWork
                        val title = when {
                            isWork -> "Work Profile"
                            isPersonal -> "Personal / System"
                            else -> env.displayName
                        }
                        val subtitle = when {
                            isWork -> "Managed Android Work Profile"
                            isPersonal -> "Host device environment"
                            else -> "Sandbox • ${env.networkConfig.name}"
                        }
                        val cardColor = when {
                            isWork -> Color(0xFF2E7D32)
                            isPersonal -> SandboxrTheme.colors.primaryAccent
                            else -> Color(env.colorTag)
                        }
                        val cardIcon = when {
                            isWork -> PhosphorIcon.BOX
                            isPersonal -> PhosphorIcon.USER
                            else -> PhosphorIcon.LOCK
                        }

                        ProfileSelectCard(
                            title = title,
                            subtitle = subtitle,
                            color = cardColor,
                            icon = cardIcon,
                            isSelected = isSelected,
                            count = count,
                            onClick = {
                                onSelectProfile(env.id)
                                onDismissRequest()
                            }
                        )
                    }
                }

                // Create New Profile Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SandboxrTheme.colors.surface2)
                        .border(1.dp, SandboxrTheme.colors.primaryAccent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable {
                            onDismissRequest()
                            onCreateNewProfile()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    PhosphorIconView(
                        icon = PhosphorIcon.PLUS,
                        color = SandboxrTheme.colors.primaryAccent,
                        size = 16.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Create New Sandbox Profile",
                        fontFamily = SandboxrFontFamilies.Inter,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SandboxrTheme.colors.primaryAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSelectCard(
    title: String,
    subtitle: String,
    color: Color,
    icon: PhosphorIcon,
    isSelected: Boolean,
    count: Int,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) color else SandboxrTheme.colors.glassBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) color.copy(alpha = 0.12f) else SandboxrTheme.colors.surface2)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                PhosphorIconView(
                    icon = icon,
                    color = color,
                    size = 18.dp
                )
            }
            Column {
                Text(
                    text = title,
                    style = SandboxrTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = SandboxrTheme.colors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = SandboxrTheme.typography.label,
                    color = SandboxrTheme.colors.textSecondary
                )
            }
        }

        // Count Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(SandboxrTheme.colors.surface1)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "$count apps",
                style = SandboxrTheme.typography.label,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) color else SandboxrTheme.colors.textSecondary
            )
        }
    }
}
