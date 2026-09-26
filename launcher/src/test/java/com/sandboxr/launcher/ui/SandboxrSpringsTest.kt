package com.sandboxr.launcher.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SandboxrSpringsTest {

    @Test
    fun testSpringPresetParameters() {
        // Snappy: dampingRatio = 0.65f, stiffness = 1000f
        assertEquals(Spring.DampingRatioMediumBouncy, SandboxrSprings.Snappy.dampingRatio, 0.001f)
        assertEquals(Spring.StiffnessHigh, SandboxrSprings.Snappy.stiffness, 0.001f)

        // Smooth: dampingRatio = 1.0f, stiffness = 400f
        assertEquals(Spring.DampingRatioNoBouncy, SandboxrSprings.Smooth.dampingRatio, 0.001f)
        assertEquals(Spring.StiffnessMedium, SandboxrSprings.Smooth.stiffness, 0.001f)

        // Gentle: dampingRatio = 1.0f, stiffness = 200f
        assertEquals(Spring.DampingRatioNoBouncy, SandboxrSprings.Gentle.dampingRatio, 0.001f)
        assertEquals(Spring.StiffnessLow, SandboxrSprings.Gentle.stiffness, 0.001f)

        // Bouncy: dampingRatio = 0.5f, stiffness = 400f
        assertEquals(Spring.DampingRatioLowBouncy, SandboxrSprings.Bouncy.dampingRatio, 0.001f)
        assertEquals(Spring.StiffnessMedium, SandboxrSprings.Bouncy.stiffness, 0.001f)
    }

    @Test
    fun testGenericSpringFactories() {
        val dpSpring: SpringSpec<Dp> = SandboxrSprings.snappy(visibilityThreshold = 0.1.dp)
        assertEquals(Spring.DampingRatioMediumBouncy, dpSpring.dampingRatio, 0.001f)
        assertEquals(Spring.StiffnessHigh, dpSpring.stiffness, 0.001f)
        assertEquals(0.1.dp, dpSpring.visibilityThreshold)

        val smoothFloat = SandboxrSprings.smooth<Float>()
        assertEquals(Spring.DampingRatioNoBouncy, smoothFloat.dampingRatio, 0.001f)
        assertEquals(Spring.StiffnessMedium, smoothFloat.stiffness, 0.001f)
    }

    @Test
    fun testResolveSpecWithReduceMotion() {
        val normalSpec = SandboxrSprings.resolveSpec(
            springSpec = SandboxrSprings.Snappy,
            isReduceMotion = false
        )
        assertTrue(normalSpec is SpringSpec)
        assertEquals(SandboxrSprings.Snappy, normalSpec)

        val reducedSpec = SandboxrSprings.resolveSpec(
            springSpec = SandboxrSprings.Snappy,
            isReduceMotion = true
        )
        assertTrue(reducedSpec is SnapSpec)
    }

    @Test
    fun testIsReduceMotionEnabledDetection() {
        val context = RuntimeEnvironment.getApplication()
        val resolver = context.contentResolver

        // Default state
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        Settings.Global.putFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1.0f)
        assertFalse(SandboxrSprings.isReduceMotionEnabled(context))

        // Animator duration scale disabled
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0.0f)
        assertTrue(SandboxrSprings.isReduceMotionEnabled(context))

        // Reset and test transition animation scale disabled
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        Settings.Global.putFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0.0f)
        assertTrue(SandboxrSprings.isReduceMotionEnabled(context))
    }
}
