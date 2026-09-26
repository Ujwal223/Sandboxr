package com.sandboxr.launcher.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sandboxr.launcher.theme.SandboxrColors

/**
 * Liquid Glass Material Modifier for SANDBOXR.
 *
 * Implements translucency, dynamic backdrop blur (RenderEffect on API 31+,
 * graceful fallback on API 29-30), and top specular glint.
 *
 * Strictly follows DESIGN.md Section 4 specifications without emojis or AI aesthetic traits.
 */
fun Modifier.glass(
    blurRadius: Dp = SandboxrColors.BlurRadiusStandard,
    tintAlpha: Float = 0.08f,
    borderAlpha: Float = 0.12f,
    cornerRadius: Dp = 20.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    isLight: Boolean = false,
    showSpecular: Boolean = true,
    tintColor: Color = Color.White
): Modifier {
    // API 29-30 fallback: when RenderEffect backdrop blur is unavailable,
    // boost tint opacity slightly to preserve contrast and legibility.
    val effectiveTintAlpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        tintAlpha
    } else {
        if (isLight) 0.85f else (tintAlpha + 0.16f).coerceAtMost(0.35f)
    }

    val topTint = if (isLight) 0.70f else (effectiveTintAlpha + 0.04f).coerceAtMost(1f)
    val bottomTint = if (isLight) 0.55f else effectiveTintAlpha

    val topBorder = if (isLight) 0.90f else (borderAlpha + 0.08f).coerceAtMost(1f)
    val bottomBorder = if (isLight) 0.70f else borderAlpha

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            tintColor.copy(alpha = topTint),
            tintColor.copy(alpha = bottomTint)
        )
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = topBorder),
            Color.White.copy(alpha = bottomBorder)
        )
    )

    val specularBrush = Brush.horizontalGradient(
        0.0f to Color.Transparent,
        0.3f to Color.White.copy(alpha = if (isLight) 0.60f else 0.35f),
        0.7f to Color.White.copy(alpha = if (isLight) 0.60f else 0.35f),
        1.0f to Color.Transparent
    )

    var modifier = this
        .graphicsLayer {
            clip = true
            this.shape = shape
        }
        .clip(shape)

    // Apply RenderEffect blur only on API 31+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.dp) {
        modifier = modifier.blur(blurRadius)
    }

    modifier = modifier
        .background(brush = backgroundBrush, shape = shape)
        .border(width = 1.dp, brush = borderBrush, shape = shape)

    if (showSpecular) {
        modifier = modifier.drawWithContent {
            drawContent()
            drawRect(
                brush = specularBrush,
                topLeft = Offset.Zero,
                size = Size(size.width, 1.dp.toPx())
            )
        }
    }

    return modifier
}

/**
 * Standard Glass preset (20dp blur radius).
 */
fun Modifier.glassStandard(
    cornerRadius: Dp = 20.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    isLight: Boolean = false,
    showSpecular: Boolean = true
): Modifier = glass(
    blurRadius = SandboxrColors.BlurRadiusStandard,
    tintAlpha = if (isLight) 0.60f else 0.08f,
    borderAlpha = if (isLight) 0.80f else 0.12f,
    cornerRadius = cornerRadius,
    shape = shape,
    isLight = isLight,
    showSpecular = showSpecular
)

/**
 * Heavy Glass preset (32dp blur radius) for lockscreens, modal sheets, and action sheets.
 */
fun Modifier.glassHeavy(
    cornerRadius: Dp = 28.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    isLight: Boolean = false,
    showSpecular: Boolean = true
): Modifier = glass(
    blurRadius = SandboxrColors.BlurRadiusHeavy,
    tintAlpha = if (isLight) 0.65f else 0.12f,
    borderAlpha = if (isLight) 0.85f else 0.16f,
    cornerRadius = cornerRadius,
    shape = shape,
    isLight = isLight,
    showSpecular = showSpecular
)

/**
 * Navigation Bar Glass preset (28dp blur radius).
 */
fun Modifier.glassNav(
    isLight: Boolean = false
): Modifier = glass(
    blurRadius = SandboxrColors.BlurRadiusNav,
    tintAlpha = if (isLight) 0.60f else 0.10f,
    borderAlpha = if (isLight) 0.80f else 0.12f,
    cornerRadius = 0.dp,
    shape = RoundedCornerShape(0.dp),
    isLight = isLight,
    showSpecular = true
)

/**
 * Card Glass preset with subtle environment color bleed.
 */
fun Modifier.glassCard(
    envColor: Color,
    cornerRadius: Dp = 24.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    isLight: Boolean = false,
    isActive: Boolean = false
): Modifier {
    val base = glass(
        blurRadius = SandboxrColors.BlurRadiusStandard,
        tintAlpha = if (isLight) 0.55f else 0.08f,
        borderAlpha = if (isLight) 0.80f else 0.12f,
        cornerRadius = cornerRadius,
        shape = shape,
        isLight = isLight,
        showSpecular = true,
        tintColor = envColor
    )

    return if (isActive) {
        base.border(
            width = 2.dp,
            color = SandboxrColors.PrimaryAccent,
            shape = shape
        )
    } else {
        base
    }
}

/**
 * Specular highlight glint composable bar.
 */
@Composable
fun SpecularGlint(
    modifier: Modifier = Modifier,
    isLight: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    0.0f to Color.Transparent,
                    0.3f to Color.White.copy(alpha = if (isLight) 0.60f else 0.35f),
                    0.7f to Color.White.copy(alpha = if (isLight) 0.60f else 0.35f),
                    1.0f to Color.Transparent
                )
            )
    )
}

/**
 * Reusable GlassSurface container.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    blurRadius: Dp = SandboxrColors.BlurRadiusStandard,
    cornerRadius: Dp = 20.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    isLight: Boolean = false,
    showSpecular: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.glass(
            blurRadius = blurRadius,
            cornerRadius = cornerRadius,
            shape = shape,
            isLight = isLight,
            showSpecular = showSpecular
        )
    ) {
        content()
    }
}
