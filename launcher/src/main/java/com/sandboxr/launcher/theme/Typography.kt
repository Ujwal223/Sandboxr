package com.sandboxr.launcher.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typeface definitions for SANDBOXR.
 * Inter for clean geometric UI hierarchy.
 * JetBrains Mono for security parameters, identifiers, and telemetry.
 */
object SandboxrFontFamilies {
    val Inter = FontFamily.SansSerif
    val JetBrainsMono = FontFamily.Monospace
}

/**
 * Immutable typography system for SANDBOXR conforming to DESIGN.md Section 3.
 */
@Immutable
data class SandboxrTypography(
    val displayLarge: TextStyle = TextStyle(
        fontFamily = SandboxrFontFamilies.Inter,
        fontWeight = FontWeight.Light,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp
    ),
    val display: TextStyle = TextStyle(
        fontFamily = SandboxrFontFamilies.Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 35.2.sp,
        letterSpacing = (-0.5).sp
    ),
    val titleLarge: TextStyle = TextStyle(
        fontFamily = SandboxrFontFamilies.Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.8.sp,
        letterSpacing = (-0.3).sp
    ),
    val title: TextStyle = TextStyle(
        fontFamily = SandboxrFontFamilies.Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.3).sp
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = SandboxrFontFamilies.Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 25.5.sp,
        letterSpacing = 0.sp
    ),
    val body: TextStyle = TextStyle(
        fontFamily = SandboxrFontFamilies.Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.5.sp,
        letterSpacing = 0.sp
    ),
    val labelLarge: TextStyle = TextStyle(
        fontFamily = SandboxrFontFamilies.Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.9.sp,
        letterSpacing = 0.3.sp
    ),
    val label: TextStyle = TextStyle(
        fontFamily = SandboxrFontFamilies.Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.3.sp,
        letterSpacing = 0.3.sp
    ),
    val monoLarge: TextStyle = TextStyle(
        fontFamily = SandboxrFontFamilies.JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.5.sp,
        letterSpacing = 0.sp
    ),
    val mono: TextStyle = TextStyle(
        fontFamily = SandboxrFontFamilies.JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.5.sp,
        letterSpacing = 0.sp
    )
)

val LocalSandboxrTypography = staticCompositionLocalOf { SandboxrTypography() }
