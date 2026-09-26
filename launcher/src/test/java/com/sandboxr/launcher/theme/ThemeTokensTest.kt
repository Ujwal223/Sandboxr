package com.sandboxr.launcher.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeTokensTest {

    @Test
    fun testDarkColorSchemeValues() {
        val dark = darkSandboxrColorScheme()
        assertTrue(dark.isDark)
        assertEquals(Color(0xFF0A0A0F), dark.background)
        assertEquals(Color(0xFF12121A), dark.surface1)
        assertEquals(Color(0xFF1A1A26), dark.surface2)
        assertEquals(Color(0xFF22223A), dark.surface3)
        assertEquals(Color(0xFF5B8AF5), dark.primaryAccent)
        assertEquals(Color(0xFFF0F0F5), dark.textPrimary)
        assertEquals(Color(0xFF8C8CA0), dark.textSecondary)
        assertEquals(Color(0xFFC8C8D0), dark.textMonospace)
        assertEquals(Color(0x14FFFFFF), dark.glassTint)
        assertEquals(Color(0x1FFFFFFF), dark.glassBorder)
    }

    @Test
    fun testLightColorSchemeValues() {
        val light = lightSandboxrColorScheme()
        assertEquals(false, light.isDark)
        assertEquals(Color(0xFFF2F2F7), light.background)
        assertEquals(Color(0xFFFFFFFF), light.surface1)
        assertEquals(Color(0xFFF0F0F5), light.surface2)
        assertEquals(Color(0xFFE8E8F0), light.surface3)
        assertEquals(Color(0xFF0A0A0F), light.textPrimary)
        assertEquals(Color(0xFF6C6C80), light.textSecondary)
        assertEquals(Color(0xFF2A2A3A), light.textMonospace)
        assertEquals(Color(0x99FFFFFF), light.glassTint)
        assertEquals(Color(0xCCFFFFFF), light.glassBorder)
    }

    @Test
    fun testDefaultEnvironmentColors() {
        assertEquals(Color(0xFF3A4A6B), SandboxrColors.EnvSlateDark)
        assertEquals(Color(0xFF7B92CC), SandboxrColors.EnvSlateLight)
        assertEquals(Color(0xFF4A3A6B), SandboxrColors.EnvVioletDark)
        assertEquals(Color(0xFF9B7BCC), SandboxrColors.EnvVioletLight)
        assertEquals(Color(0xFF6B3A4A), SandboxrColors.EnvRoseDark)
        assertEquals(Color(0xFFCC7B92), SandboxrColors.EnvRoseLight)
        assertEquals(Color(0xFF6B5A2A), SandboxrColors.EnvAmberDark)
        assertEquals(Color(0xFFCCA85B), SandboxrColors.EnvAmberLight)
        assertEquals(Color(0xFF2A5A5A), SandboxrColors.EnvTealDark)
        assertEquals(Color(0xFF5BCCCC), SandboxrColors.EnvTealLight)
        assertEquals(Color(0xFF3A5A2A), SandboxrColors.EnvMossDark)
        assertEquals(Color(0xFF7BCC5B), SandboxrColors.EnvMossLight)
        assertEquals(Color(0xFF6B2A2A), SandboxrColors.EnvCrimsonDark)
        assertEquals(Color(0xFFCC5B5B), SandboxrColors.EnvCrimsonLight)
        assertEquals(Color(0xFF3A3A3A), SandboxrColors.EnvGraphiteDark)
        assertEquals(Color(0xFF8C8C8C), SandboxrColors.EnvGraphiteLight)
    }

    @Test
    fun testTypographyTokens() {
        val typography = SandboxrTypography()
        assertEquals(FontFamily.SansSerif, SandboxrFontFamilies.Inter)
        assertEquals(FontFamily.Monospace, SandboxrFontFamilies.JetBrainsMono)

        assertEquals(40.sp, typography.displayLarge.fontSize)
        assertEquals(32.sp, typography.display.fontSize)
        assertEquals(24.sp, typography.titleLarge.fontSize)
        assertEquals(20.sp, typography.title.fontSize)
        assertEquals(17.sp, typography.bodyLarge.fontSize)
        assertEquals(15.sp, typography.body.fontSize)
        assertEquals(13.sp, typography.labelLarge.fontSize)
        assertEquals(11.sp, typography.label.fontSize)

        assertEquals(SandboxrFontFamilies.JetBrainsMono, typography.monoLarge.fontFamily)
        assertEquals(15.sp, typography.monoLarge.fontSize)
        assertEquals(SandboxrFontFamilies.JetBrainsMono, typography.mono.fontFamily)
        assertEquals(13.sp, typography.mono.fontSize)
    }

    @Test
    fun testSpacingTokens() {
        val spacing = SandboxrSpacing()
        assertEquals(160.dp, spacing.cardWidth)
        assertEquals(220.dp, spacing.cardHeight)
        assertEquals(60.dp, spacing.iconSize)
        assertEquals(72.dp, spacing.navBarHeight)
        assertEquals(44.dp, spacing.minTouchTarget)
        assertEquals(52.dp, spacing.buttonHeight)
    }

    @Test
    fun testSuperellipseShapeOutline() {
        val shape = SuperellipseShape(exponent = 4.0f)
        val outline = shape.createOutline(
            size = Size(60f, 60f),
            layoutDirection = LayoutDirection.Ltr,
            density = Density(1f)
        )
        assertNotNull(outline)
        assertTrue(outline is Outline.Generic)
    }
}
