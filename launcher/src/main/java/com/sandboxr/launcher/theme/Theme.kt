package com.sandboxr.launcher.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Global Theme Wrapper for SANDBOXR Launcher.
 *
 * Provides typed design tokens (Colors, Typography, Spacing, Shapes)
 * and harmonizes with Material3 primitives.
 */
@Composable
fun SandboxrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) darkSandboxrColorScheme() else lightSandboxrColorScheme()
    val typography = SandboxrTypography()
    val spacing = SandboxrSpacing()
    val shapes = SandboxrShapes()

    val m3Colors = if (darkTheme) {
        darkColorScheme(
            primary = colors.primaryAccent,
            onPrimary = SandboxrColors.BackgroundDark,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface1,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.textSecondary,
            error = colors.danger,
            onError = SandboxrColors.BackgroundDark
        )
    } else {
        lightColorScheme(
            primary = colors.primaryAccent,
            onPrimary = SandboxrColors.BackgroundLight,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface1,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.textSecondary,
            error = colors.danger,
            onError = SandboxrColors.BackgroundLight
        )
    }

    CompositionLocalProvider(
        LocalSandboxrColors provides colors,
        LocalSandboxrTypography provides typography,
        LocalSandboxrSpacing provides spacing,
        LocalSandboxrShapes provides shapes
    ) {
        MaterialTheme(
            colorScheme = m3Colors,
            content = content
        )
    }
}

/**
 * Accessor object for current composition tokens.
 */
object SandboxrTheme {
    val colors: SandboxrColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalSandboxrColors.current

    val typography: SandboxrTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalSandboxrTypography.current

    val spacing: SandboxrSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSandboxrSpacing.current

    val shapes: SandboxrShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalSandboxrShapes.current
}
