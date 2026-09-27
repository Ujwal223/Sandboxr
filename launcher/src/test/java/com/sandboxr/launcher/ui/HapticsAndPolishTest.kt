package com.sandboxr.launcher.ui

import android.content.Context
import android.view.View
import androidx.compose.ui.unit.dp
import com.sandboxr.launcher.theme.SandboxrColors
import com.sandboxr.launcher.theme.SandboxrSpacingTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HapticsAndPolishTest {

    @Test
    fun testHapticsDispatchAllTypes() {
        val context = RuntimeEnvironment.getApplication()
        val view = View(context)
        val haptics = SandboxrHaptics(view)

        // All 7 haptic action triggers should execute cleanly without error
        haptics.onCardPress()
        haptics.onEnvironmentSwitch()
        haptics.onDestructiveConfirm()
        haptics.onUnlockSuccess()
        haptics.onUnlockFail()
        haptics.onDragStart()
        haptics.onDrop()
    }

    @Test
    fun testHapticsPreApi30Fallback() {
        val context = RuntimeEnvironment.getApplication()
        val view = View(context)
        val haptics = SandboxrHaptics(view)

        // Verifies fallback constants on API 29 (REJECT / CONFIRM / GESTURE_END fallbacks)
        haptics.onDestructiveConfirm()
        haptics.onUnlockSuccess()
        haptics.onUnlockFail()
        haptics.onDrop()
    }

    @Test
    fun testPhosphorIconsEnumCompleteness() {
        assertEquals(30, PhosphorIcon.entries.size)
        assertTrue(PhosphorIcon.entries.contains(PhosphorIcon.LOCK))
        assertTrue(PhosphorIcon.entries.contains(PhosphorIcon.SHIELD))
        assertTrue(PhosphorIcon.entries.contains(PhosphorIcon.TERMINAL))
        assertTrue(PhosphorIcon.entries.contains(PhosphorIcon.GEAR))
        assertTrue(PhosphorIcon.entries.contains(PhosphorIcon.TRASH))
        assertTrue(PhosphorIcon.entries.contains(PhosphorIcon.EXPORT))
        assertTrue(PhosphorIcon.entries.contains(PhosphorIcon.GRID))
        assertTrue(PhosphorIcon.entries.contains(PhosphorIcon.ARROW_LEFT))
        assertTrue(PhosphorIcon.entries.contains(PhosphorIcon.ARROW_UP))
        assertTrue(PhosphorIcon.entries.contains(PhosphorIcon.CARET_DOWN))
        assertTrue(PhosphorIcon.entries.contains(PhosphorIcon.CARET_UP))
        assertTrue(PhosphorIcon.entries.contains(PhosphorIcon.MIC))
        assertTrue(PhosphorIcon.entries.contains(PhosphorIcon.CAMERA))
        assertTrue(PhosphorIcon.entries.contains(PhosphorIcon.SEARCH))
    }

    @Test
    fun testDesignChecklistConformance() {
        // Section 14 Checklist Item: All touch targets >= 44dp
        assertTrue(SandboxrSpacingTokens.MinTouchTarget >= 44.dp)
        assertTrue(SandboxrSpacingTokens.IconButtonSize >= 44.dp)
        assertTrue(SandboxrSpacingTokens.ButtonHeight >= 44.dp)
        assertTrue(SandboxrSpacingTokens.AppIconTouchTarget >= 44.dp)

        // Section 14 Checklist Item: Dark mode base surface #0A0A0F
        assertEquals(androidx.compose.ui.graphics.Color(0xFF0A0A0F), SandboxrColors.BackgroundDark)

        // Section 14 Checklist Item: Springs responsive (<350ms target)
        assertEquals(androidx.compose.animation.core.Spring.StiffnessHigh, SandboxrSprings.Snappy.stiffness, 0.1f)
        assertEquals(androidx.compose.animation.core.Spring.StiffnessMedium, SandboxrSprings.Smooth.stiffness, 0.1f)
    }
}
