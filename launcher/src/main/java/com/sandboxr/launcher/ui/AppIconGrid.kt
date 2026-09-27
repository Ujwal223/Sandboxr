package com.sandboxr.launcher.ui

import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandboxr.launcher.theme.SandboxrColors
import com.sandboxr.launcher.theme.SandboxrFontFamilies
import com.sandboxr.launcher.theme.SandboxrSpacingTokens
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.launcher.theme.SuperellipseShape

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ColorFilter
import com.sandboxr.launcher.theme.getLauncherIconShape

/**
 * Data model representing an installed application in a specific environment.
 * Marked @Immutable for maximum Compose LazyVerticalGrid recycling efficiency.
 */
@Immutable
data class AppItem(
    val packageName: String,
    val label: String,
    val iconBitmap: ImageBitmap? = null,
    val envId: String,
    val envColor: Color = SandboxrColors.PrimaryAccent,
    val isSystemApp: Boolean = false,
    val notificationBadgeCount: Int = 0,
    val userHandle: UserHandle? = null
)

/**
 * Single App Icon Component adhering strictly to DESIGN.md Section 5.2 and Pixel Launcher aesthetics:
 *
 * - Icon size: 60dp x 60dp
 * - Touch target: 72dp x 72dp
 * - Shape: Dynamically adapts to user setting (Circle, Squircle, Rounded Square, Teardrop, Pebble)
 * - Themed Icons: Monochrome Material You accent mode support
 * - Environment badge: 8dp dot bottom-right of icon with container color
 * - Snappy press scale
 * - Label: 12sp centered, max 1 line, ellipsized
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconItem(
    app: AppItem,
    modifier: Modifier = Modifier,
    iconShape: String = "Squircle",
    isThemed: Boolean = false,
    showLabel: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = tween(durationMillis = 80),
        label = "AppIconScale"
    )

    val shape = remember(iconShape) { getLauncherIconShape(iconShape) }

    Column(
        modifier = modifier
            .width(SandboxrSpacingTokens.AppIconTouchTarget)
            .scale(animatedScale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon Box (60dp x 60dp) with bottom-right environment badge
        Box(
            modifier = Modifier.size(SandboxrSpacingTokens.AppIconSize),
            contentAlignment = Alignment.Center
        ) {
            // App Icon Surface
            Box(
                modifier = Modifier
                    .size(SandboxrSpacingTokens.AppIconSize)
                    .clip(shape)
                    .background(
                        if (isThemed) SandboxrTheme.colors.surface3 else SandboxrTheme.colors.surface2
                    )
                    .border(
                        width = 1.dp,
                        color = SandboxrTheme.colors.glassBorder,
                        shape = shape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (app.iconBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = app.iconBitmap,
                        contentDescription = app.label,
                        colorFilter = if (isThemed) ColorFilter.tint(SandboxrTheme.colors.primaryAccent) else null,
                        modifier = Modifier.size(SandboxrSpacingTokens.AppIconSize)
                    )
                } else {
                    // Monospace initial placeholder
                    val initial = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    Text(
                        text = initial,
                        color = if (isThemed) SandboxrTheme.colors.primaryAccent else SandboxrTheme.colors.textPrimary,
                        fontSize = 22.sp,
                        fontFamily = SandboxrFontFamilies.Inter,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Environment Color Badge Dot (8dp at bottom-right)
            if (!app.isSystemApp || app.envId == "work_profile_environment") {
                Box(
                    modifier = Modifier
                        .size(SandboxrSpacingTokens.BadgeDotSize + 2.dp)
                        .align(Alignment.BottomEnd)
                        .border(
                            width = 1.dp,
                            color = SandboxrTheme.colors.background,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(SandboxrSpacingTokens.BadgeDotSize)
                            .background(color = app.envColor, shape = CircleShape)
                    )
                }
            }

            // Notification Badge Dot (top-right if notifications pending)
            if (app.notificationBadgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(SandboxrSpacingTokens.BadgeDotSize + 2.dp)
                        .align(Alignment.TopEnd)
                        .border(
                            width = 1.dp,
                            color = SandboxrTheme.colors.background,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(SandboxrSpacingTokens.BadgeDotSize)
                            .background(color = SandboxrTheme.colors.primaryAccent, shape = CircleShape)
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))

            // App Label: 12sp, centered, max 1 line, ellipsized
            Text(
                text = app.label,
                style = SandboxrTheme.typography.body.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = SandboxrTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * High-performance AppIconGrid displaying applications in an environment or drawer.
 * Uses stable keys and contentType to ensure butter-smooth 120Hz scrolling without recomposition lag.
 */
@Composable
fun AppIconGrid(
    apps: List<AppItem>,
    modifier: Modifier = Modifier,
    columns: GridCells = GridCells.Fixed(4),
    iconShape: String = "Squircle",
    isThemed: Boolean = false,
    showLabel: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = SandboxrSpacingTokens.AppGridHorizontalSpacing,
        vertical = SandboxrSpacingTokens.AppGridVerticalSpacing
    ),
    onAppClick: (AppItem) -> Unit = {},
    onAppLongClick: (AppItem) -> Unit = {}
) {
    LazyVerticalGrid(
        columns = columns,
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(
            SandboxrSpacingTokens.AppGridHorizontalSpacing,
            Alignment.CenterHorizontally
        ),
        verticalArrangement = Arrangement.spacedBy(SandboxrSpacingTokens.AppGridVerticalSpacing)
    ) {
        items(
            items = apps,
            key = { "${it.envId}_${it.packageName}" },
            contentType = { "app_item" }
        ) { app ->
            AppIconItem(
                app = app,
                iconShape = iconShape,
                isThemed = isThemed,
                showLabel = showLabel,
                onClick = { onAppClick(app) },
                onLongClick = { onAppLongClick(app) }
            )
        }
    }
}
