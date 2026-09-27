package com.sandboxr.launcher.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Corner radius tokens for SANDBOXR.
 */
object SandboxrRadiusTokens {
    val XSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Button: Dp = 14.dp
    val Icon: Dp = 16.dp
    val Glass: Dp = 20.dp
    val Card: Dp = 24.dp
    val BottomSheet: Dp = 28.dp
    val Pill: Dp = 36.dp
}

/**
 * Parametric Superellipse (Lamé curve) Shape.
 * Implements continuous curvature squircle using |x/a|^n + |y/b|^n = 1 (n=4).
 * Eliminates standard circular corner tangent discontinuities.
 */
class SuperellipseShape(
    val exponent: Float = 4.0f,
    private val steps: Int = 72
) : Shape {

    // High-performance MRU Outline cache to prevent re-computing transcendentals at 120Hz
    private val cacheLock = Any()
    private var cachedWidth = -1f
    private var cachedHeight = -1f
    private var cachedOutline: Outline? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Generic(Path())
        }

        synchronized(cacheLock) {
            if (size.width == cachedWidth && size.height == cachedHeight && cachedOutline != null) {
                return cachedOutline!!
            }
        }

        val path = Path()
        val a = size.width / 2f
        val b = size.height / 2f
        val cx = a
        val cy = b
        val p = 2.0f / exponent

        val dt = (2.0 * PI) / steps
        for (i in 0..steps) {
            val t = i * dt
            val cosT = cos(t).toFloat()
            val sinT = sin(t).toFloat()

            val x = cx + a * sign(cosT) * abs(cosT).pow(p)
            val y = cy + b * sign(sinT) * abs(sinT).pow(p)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        val outline = Outline.Generic(path)

        synchronized(cacheLock) {
            cachedWidth = size.width
            cachedHeight = size.height
            cachedOutline = outline
        }

        return outline
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuperellipseShape) return false
        return exponent == other.exponent && steps == other.steps
    }

    override fun hashCode(): Int {
        var result = exponent.hashCode()
        result = 31 * result + steps
        return result
    }
}

@Immutable
data class SandboxrShapes(
    val small: Shape = RoundedCornerShape(SandboxrRadiusTokens.Small),
    val medium: Shape = RoundedCornerShape(SandboxrRadiusTokens.Medium),
    val button: Shape = RoundedCornerShape(SandboxrRadiusTokens.Button),
    val icon: Shape = SuperellipseShape(exponent = 4.0f),
    val glass: Shape = RoundedCornerShape(SandboxrRadiusTokens.Glass),
    val card: Shape = RoundedCornerShape(SandboxrRadiusTokens.Card),
    val bottomSheet: Shape = RoundedCornerShape(
        topStart = SandboxrRadiusTokens.BottomSheet,
        topEnd = SandboxrRadiusTokens.BottomSheet,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    ),
    val pill: Shape = RoundedCornerShape(SandboxrRadiusTokens.Pill)
)

val LocalSandboxrShapes = staticCompositionLocalOf { SandboxrShapes() }

/**
 * Resolves the Compose [Shape] corresponding to user's launcher icon shape preference.
 * Matches Android OS skin customizations (Pixel, OneUI, GrapheneOS).
 */
fun getLauncherIconShape(shapeName: String): Shape {
    return when (shapeName.lowercase().trim()) {
        "circle" -> CircleShape
        "rounded square" -> RoundedCornerShape(16.dp)
        "teardrop" -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 6.dp)
        "pebble", "egg" -> RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 28.dp)
        "squircle" -> SuperellipseShape(exponent = 4.0f)
        else -> RoundedCornerShape(20.dp)
    }
}
