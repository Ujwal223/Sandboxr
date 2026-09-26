package com.sandboxr.launcher.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sandboxr.launcher.theme.SandboxrColors
import com.sandboxr.launcher.theme.SandboxrSpacingTokens
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.network.model.NetworkMode

/**
 * Supported geometric icon glyphs for environments.
 * Strictly vector-based; zero emojis.
 */
enum class EnvironmentIconType {
    LOCK,
    SHIELD,
    TERMINAL,
    BOX,
    CPU,
    GLOBE,
    KEY,
    DATABASE
}

/**
 * UI State representation for an Environment Card.
 */
data class EnvironmentCardData(
    val id: String,
    val name: String,
    val color: Color,
    val isSystem: Boolean = false,
    val appCount: Int = 0,
    val networkMode: NetworkMode = NetworkMode.DIRECT,
    val hasNotification: Boolean = false,
    val notificationCount: Int = 0,
    val isActive: Boolean = false,
    val iconType: EnvironmentIconType = if (isSystem) EnvironmentIconType.LOCK else EnvironmentIconType.SHIELD
)

/**
 * EnvironmentCard component adhering strictly to DESIGN.md Section 5.1.
 *
 * - Size: 160dp x 220dp
 * - Corner radius: 24dp
 * - Subtle environment color bleed at 40% opacity with Liquid Glass overlay
 * - Physics spring scaling on press (0.97) and active state (1.02)
 * - Vector-only iconography (no emojis)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnvironmentCard(
    data: EnvironmentCardData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Physics scale animation: pressed (0.97) -> active (1.02) -> default (1.0)
    val targetScale = when {
        isPressed -> 0.97f
        data.isActive -> 1.02f
        else -> 1.0f
    }

    val animatedScale by sandboxrAnimateFloatAsState(
        targetValue = targetScale,
        preset = SandboxrSprings.Snappy,
        label = "EnvironmentCardScale"
    )

    val cardColor = if (data.isSystem) Color(0xFF2A2A35) else data.color
    val shape = RoundedCornerShape(SandboxrSpacingTokens.CardRadius)

    Box(
        modifier = modifier
            .width(SandboxrSpacingTokens.CardWidth)
            .height(SandboxrSpacingTokens.CardHeight)
            .scale(animatedScale)
            .clip(shape)
            .glassCard(
                envColor = cardColor.copy(alpha = 0.40f),
                cornerRadius = SandboxrSpacingTokens.CardRadius,
                shape = shape,
                isActive = data.isActive
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(SandboxrSpacingTokens.Normal)
    ) {
        // Card Layout (Top to Bottom)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Icon (Top-Left) + Network & Notification Status Dots (Top-Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Environment Icon Glyph (24dp inside touch target)
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EnvironmentVectorGlyph(
                        type = data.iconType,
                        color = if (data.isSystem) SandboxrColors.TextSecondaryDark else cardColor
                    )
                }

                // Status Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Network Mode Dot
                    NetworkStatusDot(mode = data.networkMode)

                    // Notification Badge Dot
                    if (data.hasNotification) {
                        NotificationBadgeDot()
                    }
                }
            }

            // Bottom Area: Name + App Counter + Active Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (data.isSystem) "System" else data.name,
                        style = SandboxrTheme.typography.title,
                        color = SandboxrColors.TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${data.appCount} apps",
                        style = SandboxrTheme.typography.label,
                        color = SandboxrColors.TextSecondaryDark
                    )
                }

                // Active Indicator Dot
                if (data.isActive) {
                    ActiveIndicatorDot()
                }
            }
        }
    }
}

/**
 * 8dp Network Mode Status Dot.
 */
@Composable
fun NetworkStatusDot(
    mode: NetworkMode,
    modifier: Modifier = Modifier
) {
    val dotColor = when (mode) {
        NetworkMode.DIRECT -> SandboxrColors.Success
        NetworkMode.WIREGUARD -> SandboxrColors.PrimaryAccent
        NetworkMode.SOCKS5 -> SandboxrColors.Warning
        NetworkMode.BLOCKED -> SandboxrColors.Danger
    }

    Box(
        modifier = modifier
            .size(SandboxrSpacingTokens.NetworkDotSize)
            .background(color = dotColor, shape = CircleShape)
    )
}

/**
 * 8dp Notification Badge Dot.
 */
@Composable
fun NotificationBadgeDot(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(SandboxrSpacingTokens.BadgeDotSize)
            .background(color = SandboxrColors.Warning, shape = CircleShape)
    )
}

/**
 * 8dp Active Environment Indicator Dot with subtle ambient glow ring.
 */
@Composable
fun ActiveIndicatorDot(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(SandboxrSpacingTokens.BadgeDotSize + 4.dp)
            .border(
                width = 1.dp,
                color = SandboxrColors.PrimaryAccent.copy(alpha = 0.40f),
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

/**
 * Canvas vector renderer for environment glyphs.
 * Free of emojis and external icon assets.
 */
@Composable
fun EnvironmentVectorGlyph(
    type: EnvironmentIconType,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.8f.dp.toPx()

        when (type) {
            EnvironmentIconType.LOCK -> drawLockGlyph(color, strokeWidth, w, h)
            EnvironmentIconType.SHIELD -> drawShieldGlyph(color, strokeWidth, w, h)
            EnvironmentIconType.TERMINAL -> drawTerminalGlyph(color, strokeWidth, w, h)
            EnvironmentIconType.BOX -> drawBoxGlyph(color, strokeWidth, w, h)
            EnvironmentIconType.CPU -> drawCpuGlyph(color, strokeWidth, w, h)
            EnvironmentIconType.GLOBE -> drawGlobeGlyph(color, strokeWidth, w, h)
            EnvironmentIconType.KEY -> drawKeyGlyph(color, strokeWidth, w, h)
            EnvironmentIconType.DATABASE -> drawDatabaseGlyph(color, strokeWidth, w, h)
        }
    }
}

private fun DrawScope.drawLockGlyph(color: Color, strokeWidth: Float, w: Float, h: Float) {
    // Shackle
    val shacklePath = Path().apply {
        moveTo(w * 0.30f, h * 0.45f)
        lineTo(w * 0.30f, h * 0.28f)
        cubicTo(w * 0.30f, h * 0.10f, w * 0.70f, h * 0.10f, w * 0.70f, h * 0.28f)
        lineTo(w * 0.70f, h * 0.45f)
    }
    drawPath(shacklePath, color, style = Stroke(width = strokeWidth))
    // Body
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.20f, h * 0.45f),
        size = Size(w * 0.60f, h * 0.45f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(width = strokeWidth)
    )
}

private fun DrawScope.drawShieldGlyph(color: Color, strokeWidth: Float, w: Float, h: Float) {
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.10f)
        lineTo(w * 0.85f, h * 0.25f)
        lineTo(w * 0.85f, h * 0.55f)
        cubicTo(w * 0.85f, h * 0.80f, w * 0.5f, h * 0.92f, w * 0.5f, h * 0.92f)
        cubicTo(w * 0.5f, h * 0.92f, w * 0.15f, h * 0.80f, w * 0.15f, h * 0.55f)
        lineTo(w * 0.15f, h * 0.25f)
        close()
    }
    drawPath(path, color, style = Stroke(width = strokeWidth))
}

private fun DrawScope.drawTerminalGlyph(color: Color, strokeWidth: Float, w: Float, h: Float) {
    // Prompt >
    val prompt = Path().apply {
        moveTo(w * 0.20f, h * 0.30f)
        lineTo(w * 0.45f, h * 0.50f)
        lineTo(w * 0.20f, h * 0.70f)
    }
    drawPath(prompt, color, style = Stroke(width = strokeWidth))
    // Cursor _
    drawLine(
        color = color,
        start = Offset(w * 0.55f, h * 0.70f),
        end = Offset(w * 0.80f, h * 0.70f),
        strokeWidth = strokeWidth
    )
}

private fun DrawScope.drawBoxGlyph(color: Color, strokeWidth: Float, w: Float, h: Float) {
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.18f, h * 0.18f),
        size = Size(w * 0.64f, h * 0.64f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(width = strokeWidth)
    )
    drawLine(color, Offset(w * 0.18f, h * 0.42f), Offset(w * 0.82f, h * 0.42f), strokeWidth)
}

private fun DrawScope.drawCpuGlyph(color: Color, strokeWidth: Float, w: Float, h: Float) {
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.25f, h * 0.25f),
        size = Size(w * 0.50f, h * 0.50f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(width = strokeWidth)
    )
    // Pins
    drawLine(color, Offset(w * 0.38f, h * 0.10f), Offset(w * 0.38f, h * 0.25f), strokeWidth)
    drawLine(color, Offset(w * 0.62f, h * 0.10f), Offset(w * 0.62f, h * 0.25f), strokeWidth)
    drawLine(color, Offset(w * 0.38f, h * 0.75f), Offset(w * 0.38f, h * 0.90f), strokeWidth)
    drawLine(color, Offset(w * 0.62f, h * 0.75f), Offset(w * 0.62f, h * 0.90f), strokeWidth)
}

private fun DrawScope.drawGlobeGlyph(color: Color, strokeWidth: Float, w: Float, h: Float) {
    drawCircle(color, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(strokeWidth))
    drawLine(color, Offset(w * 0.12f, h * 0.5f), Offset(w * 0.88f, h * 0.5f), strokeWidth)
    drawOval(
        color = color,
        topLeft = Offset(w * 0.32f, h * 0.12f),
        size = Size(w * 0.36f, h * 0.76f),
        style = Stroke(strokeWidth)
    )
}

private fun DrawScope.drawKeyGlyph(color: Color, strokeWidth: Float, w: Float, h: Float) {
    drawCircle(color, radius = w * 0.20f, center = Offset(w * 0.35f, h * 0.40f), style = Stroke(strokeWidth))
    drawLine(color, Offset(w * 0.50f, h * 0.48f), Offset(w * 0.80f, h * 0.78f), strokeWidth)
    drawLine(color, Offset(w * 0.68f, h * 0.66f), Offset(w * 0.78f, h * 0.56f), strokeWidth)
}

private fun DrawScope.drawDatabaseGlyph(color: Color, strokeWidth: Float, w: Float, h: Float) {
    drawOval(color, topLeft = Offset(w * 0.20f, h * 0.15f), size = Size(w * 0.60f, h * 0.22f), style = Stroke(strokeWidth))
    drawOval(color, topLeft = Offset(w * 0.20f, h * 0.42f), size = Size(w * 0.60f, h * 0.22f), style = Stroke(strokeWidth))
    drawOval(color, topLeft = Offset(w * 0.20f, h * 0.68f), size = Size(w * 0.60f, h * 0.22f), style = Stroke(strokeWidth))
    drawLine(color, Offset(w * 0.20f, h * 0.26f), Offset(w * 0.20f, h * 0.79f), strokeWidth)
    drawLine(color, Offset(w * 0.80f, h * 0.26f), Offset(w * 0.80f, h * 0.79f), strokeWidth)
}

/**
 * LazyRow container for Environment Cards.
 * Ensures the System card is always pinned first (index 0).
 * Supports card selection, long-press action sheets, and reordering.
 */
@Composable
fun EnvironmentLazyRow(
    environments: List<EnvironmentCardData>,
    modifier: Modifier = Modifier,
    onEnvironmentSelected: (EnvironmentCardData) -> Unit = {},
    onEnvironmentLongClick: (EnvironmentCardData) -> Unit = {}
) {
    val listState = rememberLazyListState()

    // Ensure System card is pinned first
    val sortedEnvironments = remember(environments) {
        pinSystemCardFirst(environments)
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = SandboxrSpacingTokens.Normal),
        horizontalArrangement = Arrangement.spacedBy(SandboxrSpacingTokens.Medium)
    ) {
        itemsIndexed(
            items = sortedEnvironments,
            key = { _, item -> item.id }
        ) { _, item ->
            EnvironmentCard(
                data = item,
                onClick = { onEnvironmentSelected(item) },
                onLongClick = { onEnvironmentLongClick(item) }
            )
        }
    }
}

/**
 * Enforces System card pinned at index 0.
 */
fun pinSystemCardFirst(list: List<EnvironmentCardData>): List<EnvironmentCardData> {
    val systemCards = list.filter { it.isSystem }
    val virtualCards = list.filter { !it.isSystem }
    return systemCards + virtualCards
}

/**
 * Reorders virtual environment cards while guaranteeing System card remains at index 0.
 */
fun reorderVirtualEnvironments(
    list: List<EnvironmentCardData>,
    fromVirtualIndex: Int,
    toVirtualIndex: Int
): List<EnvironmentCardData> {
    val pinned = pinSystemCardFirst(list)
    val systemCards = pinned.filter { it.isSystem }
    val virtualCards = pinned.filter { !it.isSystem }.toMutableList()

    if (fromVirtualIndex in virtualCards.indices && toVirtualIndex in virtualCards.indices) {
        val item = virtualCards.removeAt(fromVirtualIndex)
        virtualCards.add(toVirtualIndex, item)
    }

    return systemCards + virtualCards
}
