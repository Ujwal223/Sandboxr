package com.sandboxr.launcher.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp

/**
 * Spring physics animation presets for SANDBOXR.
 *
 * Adheres strictly to DESIGN.md Section 6:
 * - Springs everywhere instead of Bezier curves for physical tactile weight.
 * - Under 350ms duration response target.
 * - One motion per action without animation chaining.
 * - Accessibility-aware: automatically downgrades to snap() when reduce motion is enabled.
 *
 * Zero emojis or AI traits.
 */
object SandboxrSprings {

    // Snappy — button presses, icon scale, toggles (<150ms effective)
    val Snappy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy, // 0.65f
        stiffness = Spring.StiffnessHigh                // 1000f
    )

    // Smooth — screen transitions, card expand, bottom sheet appearance (~250-300ms)
    val Smooth: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,     // 1.0f
        stiffness = Spring.StiffnessMedium              // 400f
    )

    // Gentle — environment card idle transitions, badge pulses (~350ms)
    val Gentle: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,     // 1.0f
        stiffness = Spring.StiffnessLow                 // 200f
    )

    // Bouncy — successful unlocks, new environment creation (~350-400ms)
    val Bouncy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,    // 0.5f
        stiffness = Spring.StiffnessMedium              // 400f
    )

    /**
     * Generic typed spring factories for custom types (Dp, IntOffset, Size, etc.)
     */
    fun <T> snappy(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> smooth(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> gentle(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> bouncy(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = visibilityThreshold
    )

    /**
     * Checks if the device has reduce-motion or disabled animation scale.
     */
    fun isReduceMotionEnabled(context: Context): Boolean {
        return try {
            val resolver = context.contentResolver
            val animatorScale = Settings.Global.getFloat(
                resolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            val transitionScale = Settings.Global.getFloat(
                resolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            animatorScale == 0f || transitionScale == 0f
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Resolves the appropriate animation spec based on reduce-motion state.
     * When reduce motion is enabled, returns snap() for instantaneous state change.
     */
    fun <T> resolveSpec(
        springSpec: SpringSpec<T>,
        isReduceMotion: Boolean
    ): AnimationSpec<T> {
        return if (isReduceMotion) snap() else springSpec
    }
}

/**
 * Composable helper returning true if the device environment requests reduced motion.
 */
@Composable
@ReadOnlyComposable
fun rememberIsReduceMotion(): Boolean {
    val context = LocalContext.current
    return SandboxrSprings.isReduceMotionEnabled(context)
}

/**
 * Composable resolving a spring preset or snap() if reduced motion is requested.
 */
@Composable
fun <T> springOrSnap(preset: SpringSpec<T>): AnimationSpec<T> {
    val isReduceMotion = rememberIsReduceMotion()
    return SandboxrSprings.resolveSpec(preset, isReduceMotion)
}

/**
 * Accessibility-aware animated float state wrapper using Sandboxr spring physics.
 */
@Composable
fun sandboxrAnimateFloatAsState(
    targetValue: Float,
    preset: SpringSpec<Float> = SandboxrSprings.Smooth,
    label: String = "SandboxrFloatAnimation"
): State<Float> {
    val spec = springOrSnap(preset)
    return animateFloatAsState(
        targetValue = targetValue,
        animationSpec = spec,
        label = label
    )
}

/**
 * Accessibility-aware animated Dp state wrapper using Sandboxr spring physics.
 */
@Composable
fun sandboxrAnimateDpAsState(
    targetValue: Dp,
    preset: SpringSpec<Dp> = SandboxrSprings.smooth(),
    label: String = "SandboxrDpAnimation"
): State<Dp> {
    val spec = springOrSnap(preset)
    return animateDpAsState(
        targetValue = targetValue,
        animationSpec = spec,
        label = label
    )
}

/**
 * Accessibility-aware animated Color state wrapper using Sandboxr spring physics.
 */
@Composable
fun sandboxrAnimateColorAsState(
    targetValue: Color,
    preset: SpringSpec<Color> = SandboxrSprings.smooth(),
    label: String = "SandboxrColorAnimation"
): State<Color> {
    val spec = springOrSnap(preset)
    return animateColorAsState(
        targetValue = targetValue,
        animationSpec = spec,
        label = label
    )
}
