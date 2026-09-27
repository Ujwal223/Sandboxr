package com.sandboxr.launcher.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

/**
 * Global Theme Wrapper for SANDBOXR Launcher.
 *
 * Implements pure Google Material You (Material 3 Dynamic Theming) matching GrapheneOS Launcher3.
 * Dynamically adapts color scheme from device wallpaper on Android 12+ (API 31+).
 */
@Composable
fun SandboxrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val m3Colors = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme(
            primary = SandboxrColors.PrimaryAccent,
            background = SandboxrColors.BackgroundDark,
            surface = SandboxrColors.SurfaceLevel1Dark,
            surfaceVariant = SandboxrColors.SurfaceLevel2Dark
        )
        else -> lightColorScheme(
            primary = SandboxrColors.PrimaryAccent,
            background = SandboxrColors.BackgroundLight,
            surface = SandboxrColors.SurfaceLevel1Light,
            surfaceVariant = SandboxrColors.SurfaceLevel2Light
        )
    }

    val colors = SandboxrColorScheme(
        isDark = darkTheme,
        background = m3Colors.background,
        surface1 = m3Colors.surface,
        surface2 = m3Colors.surfaceVariant,
        surface3 = m3Colors.surfaceContainerHigh,
        glassTint = m3Colors.surface.copy(alpha = 0.88f),
        glassBorder = m3Colors.outlineVariant.copy(alpha = 0.4f),
        primaryAccent = m3Colors.primary,
        accentPressed = m3Colors.primary.copy(alpha = 0.8f),
        accentGlow = m3Colors.primary.copy(alpha = 0.15f),
        success = SandboxrColors.Success,
        warning = SandboxrColors.Warning,
        danger = m3Colors.error,
        textPrimary = m3Colors.onSurface,
        textSecondary = m3Colors.onSurfaceVariant,
        textTertiary = m3Colors.outline,
        textMonospace = m3Colors.onSurface
    )

    val typography = SandboxrTypography()
    val spacing = SandboxrSpacing()
    val shapes = SandboxrShapes()

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
