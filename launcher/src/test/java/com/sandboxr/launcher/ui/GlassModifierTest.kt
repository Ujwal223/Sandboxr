package com.sandboxr.launcher.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sandboxr.launcher.theme.SandboxrColors
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GlassModifierTest {

    @Test
    fun testGlassModifierDark() {
        val modifier = Modifier.glass(
            blurRadius = 20.dp,
            tintAlpha = 0.08f,
            borderAlpha = 0.12f,
            cornerRadius = 20.dp,
            isLight = false,
            showSpecular = true
        )
        assertNotNull(modifier)
    }

    @Test
    fun testGlassModifierLight() {
        val modifier = Modifier.glass(
            blurRadius = 20.dp,
            tintAlpha = 0.60f,
            borderAlpha = 0.80f,
            cornerRadius = 20.dp,
            isLight = true,
            showSpecular = true
        )
        assertNotNull(modifier)
    }

    @Test
    fun testGlassStandardPreset() {
        val modifier = Modifier.glassStandard(
            cornerRadius = 20.dp,
            isLight = false
        )
        assertNotNull(modifier)
    }

    @Test
    fun testGlassHeavyPreset() {
        val modifier = Modifier.glassHeavy(
            cornerRadius = 28.dp,
            isLight = false
        )
        assertNotNull(modifier)
    }

    @Test
    fun testGlassNavPreset() {
        val modifier = Modifier.glassNav(isLight = false)
        assertNotNull(modifier)
    }

    @Test
    fun testGlassCardPresetActiveAndInactive() {
        val inactiveCard = Modifier.glassCard(
            envColor = SandboxrColors.EnvSlateDark,
            cornerRadius = 24.dp,
            isActive = false
        )
        val activeCard = Modifier.glassCard(
            envColor = SandboxrColors.EnvSlateDark,
            cornerRadius = 24.dp,
            isActive = true
        )
        assertNotNull(inactiveCard)
        assertNotNull(activeCard)
    }

    @Test
    @Config(sdk = [29])
    fun testGlassModifierPreApi31Fallback() {
        // Verifies fallback execution on Android 10 (API 29) without RenderEffect crash
        val modifier = Modifier.glass(
            blurRadius = 20.dp,
            tintAlpha = 0.08f,
            borderAlpha = 0.12f,
            cornerRadius = 20.dp,
            isLight = false
        )
        assertNotNull(modifier)
    }
}
