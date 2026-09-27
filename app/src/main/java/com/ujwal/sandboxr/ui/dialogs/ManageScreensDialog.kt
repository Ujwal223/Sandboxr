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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sandboxr.launcher.theme.SandboxrFontFamilies
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.launcher.ui.PhosphorIcon
import com.sandboxr.launcher.ui.PhosphorIconView

/**
 * Manage Workspace Screens Dialog modeled after AOSP Launcher3 / GrapheneOS Workspace management.
 * Allows adding screens, removing screens, and setting the default Home Screen.
 */
@Composable
fun ManageScreensDialog(
    pageCount: Int,
    defaultPageIndex: Int,
    currentPageIndex: Int,
    onSetDefaultPage: (Int) -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: (Int) -> Unit,
    onSelectPage: (Int) -> Unit,
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
                            text = "MANAGE SCREENS",
                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = SandboxrTheme.colors.primaryAccent
                        )
                        Text(
                            text = "Add, remove, or set default Home screen",
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
                            .clickable(onClick = onDismissRequest),
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorIconView(
                            icon = PhosphorIcon.X,
                            color = SandboxrTheme.colors.textSecondary,
                            size = 14.dp
                        )
                    }
                }

                // Screens Horizontal Carousel
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pageCount) { pageIndex ->
                        val isDefault = pageIndex == defaultPageIndex
                        val isCurrent = pageIndex == currentPageIndex

                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(190.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isCurrent) SandboxrTheme.colors.surface3 else SandboxrTheme.colors.surface2)
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isDefault) SandboxrTheme.colors.primaryAccent else SandboxrTheme.colors.glassBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    onSelectPage(pageIndex)
                                    onDismissRequest()
                                }
                                .padding(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top row: Page number & Home default badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Screen ${pageIndex + 1}",
                                        fontFamily = SandboxrFontFamilies.JetBrainsMono,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SandboxrTheme.colors.textPrimary
                                    )

                                    // Default Home Icon Toggle
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (isDefault) SandboxrTheme.colors.primaryAccent else SandboxrTheme.colors.surface1)
                                            .clickable { onSetDefaultPage(pageIndex) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        PhosphorIconView(
                                            icon = PhosphorIcon.HOME,
                                            color = if (isDefault) SandboxrTheme.colors.surface1 else SandboxrTheme.colors.textSecondary,
                                            size = 14.dp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Bottom Actions: Set Default or Remove
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (isDefault) {
                                        Text(
                                            text = "Default Screen",
                                            fontFamily = SandboxrFontFamilies.Inter,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SandboxrTheme.colors.primaryAccent
                                        )
                                    } else {
                                        Text(
                                            text = "Tap Home icon to set default",
                                            fontFamily = SandboxrFontFamilies.Inter,
                                            fontSize = 9.sp,
                                            color = SandboxrTheme.colors.textSecondary
                                        )
                                    }

                                    if (pageCount > 1) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SandboxrTheme.colors.danger.copy(alpha = 0.12f))
                                                .clickable { onRemovePage(pageIndex) }
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PhosphorIconView(
                                                icon = PhosphorIcon.TRASH,
                                                color = SandboxrTheme.colors.danger,
                                                size = 12.dp
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Delete",
                                                fontFamily = SandboxrFontFamilies.Inter,
                                                fontSize = 10.sp,
                                                color = SandboxrTheme.colors.danger,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add Screen Card
                    item {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(190.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SandboxrTheme.colors.surface2)
                                .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(16.dp))
                                .clickable(onClick = onAddPage)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(SandboxrTheme.colors.primaryAccent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PhosphorIconView(
                                        icon = PhosphorIcon.PLUS,
                                        color = SandboxrTheme.colors.primaryAccent,
                                        size = 18.dp
                                    )
                                }
                                Text(
                                    text = "Add Screen",
                                    fontFamily = SandboxrFontFamilies.Inter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SandboxrTheme.colors.primaryAccent
                                )
                            }
                        }
                    }
                }

                // Done Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SandboxrTheme.colors.primaryAccent)
                        .clickable(onClick = onDismissRequest)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Done",
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
