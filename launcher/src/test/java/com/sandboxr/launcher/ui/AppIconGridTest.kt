package com.sandboxr.launcher.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.sandboxr.launcher.theme.SandboxrColors
import com.sandboxr.launcher.theme.SuperellipseShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppIconGridTest {

    @Test
    fun testAppItemDataProperties() {
        val systemApp = AppItem(
            packageName = "com.android.settings",
            label = "Settings",
            envId = "system",
            isSystemApp = true
        )
        assertTrue(systemApp.isSystemApp)
        assertEquals("com.android.settings", systemApp.packageName)
        assertEquals("Settings", systemApp.label)

        val virtualApp = AppItem(
            packageName = "org.thoughtcrime.securesms",
            label = "Signal",
            envId = "privacy-uuid",
            envColor = SandboxrColors.EnvSlateLight,
            isSystemApp = false,
            notificationBadgeCount = 5
        )
        assertFalse(virtualApp.isSystemApp)
        assertEquals(SandboxrColors.EnvSlateLight, virtualApp.envColor)
        assertEquals(5, virtualApp.notificationBadgeCount)
    }

    @Test
    fun testAppIconSquircleGeometry() {
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
