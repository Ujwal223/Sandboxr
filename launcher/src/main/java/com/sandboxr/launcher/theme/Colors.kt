package com.sandboxr.launcher.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * SANDBOXR Design System Color Tokens.
 *
 * Adheres strictly to GrapheneOS zero-decoration security hierarchy and Apple
 * Liquid Glass physics. Dark-first architecture: #0A0A0F base surface with
 * translucent glass overlays and high-contrast monospace security legibility.
 *
 * Zero emoji or psychedelic AI traits. Professional, high-precision engineering palette.
 */
object SandboxrColors {
    // Dark Surfaces
    val BackgroundDark = Color(0xFF0A0A0F)
    val SurfaceLevel1Dark = Color(0xFF12121A)
    val SurfaceLevel2Dark = Color(0xFF1A1A26)
    val SurfaceLevel3Dark = Color(0xFF22223A)

    // Light Surfaces
    val BackgroundLight = Color(0xFFF2F2F7)
    val SurfaceLevel1Light = Color(0xFFFFFFFF)
    val SurfaceLevel2Light = Color(0xFFF0F0F5)
    val SurfaceLevel3Light = Color(0xFFE8E8F0)

    // Glass Material Constants
    val GlassTintDark = Color(0x14FFFFFF)      // 8% white over blur
    val GlassBorderDark = Color(0x1FFFFFFF)    // 12% white 1dp stroke
    val GlassTintLight = Color(0x99FFFFFF)     // 60% white over blur
    val GlassBorderLight = Color(0xCCFFFFFF)   // 80% white 1dp stroke

    val BlurRadiusStandard: Dp = 20.dp
    val BlurRadiusHeavy: Dp = 32.dp
    val BlurRadiusNav: Dp = 28.dp

    // Accent Colors
    val PrimaryAccent = Color(0xFF5B8AF5)       // Blue-violet for active states & CTAs
    val AccentPressed = Color(0xFF4070E0)       // Darkened on tap
    val AccentGlow = Color(0x1F5B8AF5)          // 12% opacity ambient glow
    val Success = Color(0xFF34C97A)
    val Warning = Color(0xFFF5A623)
    val Danger = Color(0xFFFF453A)

    // Text Dark
    val TextPrimaryDark = Color(0xFFF0F0F5)
    val TextSecondaryDark = Color(0xFF8C8CA0)
    val TextTertiaryDark = Color(0xFF5C5C70)
    val TextMonospaceDark = Color(0xFFC8C8D0)

    // Text Light
    val TextPrimaryLight = Color(0xFF0A0A0F)
    val TextSecondaryLight = Color(0xFF6C6C80)
    val TextTertiaryLight = Color(0xFF9C9CB0)
    val TextMonospaceLight = Color(0xFF2A2A3A)

    // Common Text
    val TextDestructive = Color(0xFFFF453A)
    val TextLink = Color(0xFF5B8AF5)

    /**
     * Predefined 8 environment palette pairings (Dark fill, Light badge).
     */
    val EnvSlateDark = Color(0xFF3A4A6B)
    val EnvSlateLight = Color(0xFF7B92CC)

    val EnvVioletDark = Color(0xFF4A3A6B)
    val EnvVioletLight = Color(0xFF9B7BCC)

    val EnvRoseDark = Color(0xFF6B3A4A)
    val EnvRoseLight = Color(0xFFCC7B92)

    val EnvAmberDark = Color(0xFF6B5A2A)
    val EnvAmberLight = Color(0xFFCCA85B)

    val EnvTealDark = Color(0xFF2A5A5A)
    val EnvTealLight = Color(0xFF5BCCCC)

    val EnvMossDark = Color(0xFF3A5A2A)
    val EnvMossLight = Color(0xFF7BCC5B)

    val EnvCrimsonDark = Color(0xFF6B2A2A)
    val EnvCrimsonLight = Color(0xFFCC5B5B)

    val EnvGraphiteDark = Color(0xFF3A3A3A)
    val EnvGraphiteLight = Color(0xFF8C8C8C)
}

/**
 * Immutable color scheme container consumed throughout the launcher UI.
 */
@Immutable
data class SandboxrColorScheme(
    val isDark: Boolean,
    val background: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val glassTint: Color,
    val glassBorder: Color,
    val primaryAccent: Color,
    val accentPressed: Color,
    val accentGlow: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textMonospace: Color,
    val textDestructive: Color = SandboxrColors.TextDestructive,
    val textLink: Color = SandboxrColors.TextLink
)

fun darkSandboxrColorScheme(): SandboxrColorScheme = SandboxrColorScheme(
    isDark = true,
    background = SandboxrColors.BackgroundDark,
    surface1 = SandboxrColors.SurfaceLevel1Dark,
    surface2 = SandboxrColors.SurfaceLevel2Dark,
    surface3 = SandboxrColors.SurfaceLevel3Dark,
    glassTint = SandboxrColors.GlassTintDark,
    glassBorder = SandboxrColors.GlassBorderDark,
    primaryAccent = SandboxrColors.PrimaryAccent,
    accentPressed = SandboxrColors.AccentPressed,
    accentGlow = SandboxrColors.AccentGlow,
    success = SandboxrColors.Success,
    warning = SandboxrColors.Warning,
    danger = SandboxrColors.Danger,
    textPrimary = SandboxrColors.TextPrimaryDark,
    textSecondary = SandboxrColors.TextSecondaryDark,
    textTertiary = SandboxrColors.TextTertiaryDark,
    textMonospace = SandboxrColors.TextMonospaceDark
)

fun lightSandboxrColorScheme(): SandboxrColorScheme = SandboxrColorScheme(
    isDark = false,
    background = SandboxrColors.BackgroundLight,
    surface1 = SandboxrColors.SurfaceLevel1Light,
    surface2 = SandboxrColors.SurfaceLevel2Light,
    surface3 = SandboxrColors.SurfaceLevel3Light,
    glassTint = SandboxrColors.GlassTintLight,
    glassBorder = SandboxrColors.GlassBorderLight,
    primaryAccent = SandboxrColors.PrimaryAccent,
    accentPressed = SandboxrColors.AccentPressed,
    accentGlow = SandboxrColors.AccentGlow,
    success = SandboxrColors.Success,
    warning = SandboxrColors.Warning,
    danger = SandboxrColors.Danger,
    textPrimary = SandboxrColors.TextPrimaryLight,
    textSecondary = SandboxrColors.TextSecondaryLight,
    textTertiary = SandboxrColors.TextTertiaryLight,
    textMonospace = SandboxrColors.TextMonospaceLight
)

val LocalSandboxrColors = staticCompositionLocalOf { darkSandboxrColorScheme() }
