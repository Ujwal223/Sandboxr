package com.sandboxr.launcher.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing and sizing tokens for SANDBOXR.
 */
object SandboxrSpacingTokens {
    val None: Dp = 0.dp
    val XSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Normal: Dp = 16.dp
    val Large: Dp = 20.dp
    val XLarge: Dp = 24.dp
    val XXLarge: Dp = 32.dp
    val XXXLarge: Dp = 48.dp

    // Component Dimensions
    val CardWidth: Dp = 160.dp
    val CardHeight: Dp = 220.dp
    val CardRadius: Dp = 24.dp

    val AppIconSize: Dp = 60.dp
    val AppIconTouchTarget: Dp = 72.dp
    val AppGridHorizontalSpacing: Dp = 16.dp
    val AppGridVerticalSpacing: Dp = 20.dp

    val NavBarHeight: Dp = 72.dp
    val ButtonHeight: Dp = 52.dp
    val IconButtonSize: Dp = 44.dp
    val MinTouchTarget: Dp = 44.dp

    val BadgeDotSize: Dp = 8.dp
    val NetworkDotSize: Dp = 8.dp
    val ActiveRingWidth: Dp = 2.dp
    val GlassBorderWidth: Dp = 1.dp

    val BottomSheetRadius: Dp = 28.dp
    val BottomSheetHandleWidth: Dp = 32.dp
    val BottomSheetHandleHeight: Dp = 4.dp
}

@Immutable
data class SandboxrSpacing(
    val none: Dp = SandboxrSpacingTokens.None,
    val xSmall: Dp = SandboxrSpacingTokens.XSmall,
    val small: Dp = SandboxrSpacingTokens.Small,
    val medium: Dp = SandboxrSpacingTokens.Medium,
    val normal: Dp = SandboxrSpacingTokens.Normal,
    val large: Dp = SandboxrSpacingTokens.Large,
    val xLarge: Dp = SandboxrSpacingTokens.XLarge,
    val xxLarge: Dp = SandboxrSpacingTokens.XXLarge,
    val xxxLarge: Dp = SandboxrSpacingTokens.XXXLarge,
    val cardWidth: Dp = SandboxrSpacingTokens.CardWidth,
    val cardHeight: Dp = SandboxrSpacingTokens.CardHeight,
    val iconSize: Dp = SandboxrSpacingTokens.AppIconSize,
    val navBarHeight: Dp = SandboxrSpacingTokens.NavBarHeight,
    val minTouchTarget: Dp = SandboxrSpacingTokens.MinTouchTarget,
    val buttonHeight: Dp = SandboxrSpacingTokens.ButtonHeight
)

val LocalSandboxrSpacing = staticCompositionLocalOf { SandboxrSpacing() }
