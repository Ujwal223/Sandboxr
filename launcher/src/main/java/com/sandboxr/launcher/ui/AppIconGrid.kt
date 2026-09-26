package com.sandboxr.launcher.ui

import android.graphics.drawable.Drawable
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

/**
 * Data model representing an installed application in a specific environment.
 */
data class AppItem(
    val packageName: String,
    val label: String,
    val iconBitmap: ImageBitmap? = null,
    val envId: String,
    val envColor: Color = SandboxrColors.PrimaryAccent,
    val isSystemApp: Boolean = false,
    val notificationBadgeCount: Int = 0
)

/**
 * Single App Icon Component adhering strictly to DESIGN.md Section 5.2:
 *
 * - Icon size: 60dp x 60dp
 * - Touch target: 72dp x 72dp
 * - Shape: SuperellipseShape (n=4 exponent squircle)
 * - Environment badge: 8dp dot bottom-right of icon with container color
 * - Spring physics: Snappy scale (0.94) on press
 * - Label: 13sp centered, max 1 line, ellipsized
 * - Zero emojis, clean typography
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconItem(
    app: AppItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetScale = if (isPressed) 0.94f else 1.0f
    val animatedScale by sandboxrAnimateFloatAsState(
        targetValue = targetScale,
        preset = SandboxrSprings.Snappy,
        label = "AppIconScale"
    )

    val squircleShape = remember { SuperellipseShape(exponent = 4.0f) }

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
            // App Icon Surface (Superellipse Squircle)
            Box(
                modifier = Modifier
                    .size(SandboxrSpacingTokens.AppIconSize)
                    .clip(squircleShape)
                    .background(SandboxrColors.SurfaceLevel2Dark)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = squircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (app.iconBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = app.iconBitmap,
                        contentDescription = app.label,
                        modifier = Modifier.size(SandboxrSpacingTokens.AppIconSize)
                    )
                } else {
                    // Minimalist monospace initial placeholder (zero emojis)
                    val initial = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    Text(
                        text = initial,
                        color = SandboxrColors.TextPrimaryDark,
                        fontSize = 22.sp,
                        fontFamily = SandboxrFontFamilies.Inter,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Environment Color Badge Dot (8dp at bottom-right)
            if (!app.isSystemApp) {
                Box(
                    modifier = Modifier
                        .size(SandboxrSpacingTokens.BadgeDotSize + 2.dp)
                        .align(Alignment.BottomEnd)
                        .border(
                            width = 1.dp,
                            color = SandboxrColors.BackgroundDark,
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
                            color = SandboxrColors.BackgroundDark,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(SandboxrSpacingTokens.BadgeDotSize)
                            .background(color = SandboxrColors.PrimaryAccent, shape = CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // App Label: 13sp, centered, max 1 line, ellipsized
        Text(
            text = app.label,
            style = SandboxrTheme.typography.mono.copy(fontSize = 13.sp),
            color = SandboxrColors.TextPrimaryDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * AppIconGrid displaying applications in an environment using standard launcher spacing.
 *
 * Spacing: 16dp horizontal, 20dp vertical
 */
@Composable
fun AppIconGrid(
    apps: List<AppItem>,
    modifier: Modifier = Modifier,
    columns: GridCells = GridCells.Fixed(4),
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
            key = { "${it.envId}_${it.packageName}" }
        ) { app ->
            AppIconItem(
                app = app,
                onClick = { onAppClick(app) },
                onLongClick = { onAppLongClick(app) }
            )
        }
    }
}
