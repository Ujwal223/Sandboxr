package com.sandboxr.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandboxr.launcher.theme.SandboxrColors
import com.sandboxr.launcher.theme.SandboxrSpacingTokens
import com.sandboxr.launcher.theme.SandboxrTheme

/**
 * Primary navigation destinations for SANDBOXR.
 */
enum class NavTab(val label: String) {
    ENVIRONMENTS("Environments"),
    FIREWALL("Firewall"),
    VAULT("Vault"),
    SETTINGS("Settings")
}

/**
 * Floating Glass Navigation Bar adhering to DESIGN.md Section 5.3:
 *
 * - Height: 72dp (+ safe area navigation bar insets)
 * - Heavy glass blur (28dp radius) floating over content
 * - 1dp subtle top border (White 12%)
 * - 56dp minimum touch target per tab item
 * - Active tab: 2dp accent indicator line above icon + accent colored icon & label
 * - Clean vector iconography; zero emojis
 */
@Composable
fun SandboxrNavBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
    isLight: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassNav(isLight = isLight)
            .navigationBarsPadding()
            .height(SandboxrSpacingTokens.NavBarHeight)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(SandboxrSpacingTokens.NavBarHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                NavTabItem(
                    tab = tab,
                    isSelected = isSelected,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Individual navigation tab item (minimum 56dp touch target).
 */
@Composable
fun NavTabItem(
    tab: NavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val contentColor = if (isSelected) {
        SandboxrColors.PrimaryAccent
    } else {
        SandboxrColors.TextSecondaryDark
    }

    Column(
        modifier = modifier
            .height(SandboxrSpacingTokens.NavBarHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Active Indicator (2dp line above icon)
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(2.dp)
                .background(
                    color = if (isSelected) SandboxrColors.PrimaryAccent else Color.Transparent,
                    shape = RoundedCornerShape(1.dp)
                )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Vector Icon Glyph (24dp)
        NavTabGlyph(
            tab = tab,
            color = contentColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Label (10sp)
        Text(
            text = tab.label,
            color = contentColor,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Geometric vector glyphs for navigation tabs.
 */
@Composable
fun NavTabGlyph(
    tab: NavTab,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.6f.dp.toPx()

        when (tab) {
            NavTab.ENVIRONMENTS -> drawEnvironmentsGlyph(color, strokeWidth, w, h)
            NavTab.FIREWALL -> drawFirewallGlyph(color, strokeWidth, w, h)
            NavTab.VAULT -> drawVaultGlyph(color, strokeWidth, w, h)
            NavTab.SETTINGS -> drawSettingsGlyph(color, strokeWidth, w, h)
        }
    }
}

private fun DrawScope.drawEnvironmentsGlyph(color: Color, strokeWidth: Float, w: Float, h: Float) {
    // 2x2 grid representing modular containers
    val gap = w * 0.16f
    val boxW = (w - gap) / 2f
    val boxH = (h - gap) / 2f
    val radius = CornerRadius(2.5f.dp.toPx())

    drawRoundRect(color, Offset(0f, 0f), Size(boxW, boxH), radius, Stroke(strokeWidth))
    drawRoundRect(color, Offset(boxW + gap, 0f), Size(boxW, boxH), radius, Stroke(strokeWidth))
    drawRoundRect(color, Offset(0f, boxH + gap), Size(boxW, boxH), radius, Stroke(strokeWidth))
    drawRoundRect(color, Offset(boxW + gap, boxH + gap), Size(boxW, boxH), radius, Stroke(strokeWidth))
}

private fun DrawScope.drawFirewallGlyph(color: Color, strokeWidth: Float, w: Float, h: Float) {
    // Shield glyph
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.10f)
        lineTo(w * 0.85f, h * 0.25f)
        lineTo(w * 0.85f, h * 0.55f)
        cubicTo(w * 0.85f, h * 0.80f, w * 0.5f, h * 0.92f, w * 0.5f, h * 0.92f)
        cubicTo(w * 0.5f, h * 0.92f, w * 0.15f, h * 0.80f, w * 0.15f, h * 0.55f)
        lineTo(w * 0.15f, h * 0.25f)
        close()
    }
    drawPath(path, color, style = Stroke(strokeWidth))
}

private fun DrawScope.drawVaultGlyph(color: Color, strokeWidth: Float, w: Float, h: Float) {
    // Vault safe outline with center dial
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.12f, h * 0.12f),
        size = Size(w * 0.76f, h * 0.76f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(strokeWidth)
    )
    drawCircle(color, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(strokeWidth))
    drawCircle(color, radius = 2.dp.toPx(), center = Offset(w * 0.5f, h * 0.5f))
}

private fun DrawScope.drawSettingsGlyph(color: Color, strokeWidth: Float, w: Float, h: Float) {
    // Clean sliders icon
    drawLine(color, Offset(w * 0.15f, h * 0.30f), Offset(w * 0.85f, h * 0.30f), strokeWidth)
    drawCircle(color, radius = 3.dp.toPx(), center = Offset(w * 0.38f, h * 0.30f))

    drawLine(color, Offset(w * 0.15f, h * 0.70f), Offset(w * 0.85f, h * 0.70f), strokeWidth)
    drawCircle(color, radius = 3.dp.toPx(), center = Offset(w * 0.65f, h * 0.70f))
}

/**
 * Action Sheet Item definition.
 */
data class ActionSheetItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Environment Action Sheet adhering to DESIGN.md Section 5.5:
 *
 * - Corner radius top: 28dp
 * - Heavy glass material (blurRadius=32dp, tintAlpha=0.12f)
 * - Handle: 32dp x 4dp rounded, White 30%, centered, 12dp from top
 * - Clean vertical action item stack
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentActionSheet(
    environmentName: String,
    items: List<ActionSheetItem>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = SandboxrSpacingTokens.BottomSheetRadius,
            topEnd = SandboxrSpacingTokens.BottomSheetRadius
        ),
        containerColor = SandboxrColors.SurfaceLevel1Dark.copy(alpha = 0.95f),
        dragHandle = {
            // Drag handle: 32dp x 4dp, White 30%, centered, 12dp from top
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(SandboxrSpacingTokens.BottomSheetHandleWidth)
                    .height(SandboxrSpacingTokens.BottomSheetHandleHeight)
                    .background(
                        color = Color.White.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = SandboxrSpacingTokens.Normal, vertical = 8.dp)
        ) {
            // Header
            Text(
                text = environmentName,
                style = SandboxrTheme.typography.title,
                color = SandboxrColors.TextPrimaryDark,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Items
            items.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = {
                            action.onClick()
                            onDismissRequest()
                        })
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = action.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (action.isDestructive) {
                                SandboxrColors.TextDestructive
                            } else {
                                SandboxrColors.TextPrimaryDark
                            }
                        )
                        if (action.subtitle != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = action.subtitle,
                                fontSize = 12.sp,
                                color = SandboxrColors.TextSecondaryDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
