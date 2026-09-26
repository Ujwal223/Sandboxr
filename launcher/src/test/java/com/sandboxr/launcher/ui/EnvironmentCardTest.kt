package com.sandboxr.launcher.ui

import androidx.compose.ui.graphics.Color
import com.sandboxr.launcher.theme.SandboxrColors
import com.sandboxr.network.model.NetworkMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EnvironmentCardTest {

    @Test
    fun testPinSystemCardFirst() {
        val virtual1 = EnvironmentCardData(id = "v1", name = "Banking", color = SandboxrColors.EnvSlateDark, isSystem = false)
        val virtual2 = EnvironmentCardData(id = "v2", name = "Social", color = SandboxrColors.EnvVioletDark, isSystem = false)
        val systemCard = EnvironmentCardData(id = "sys", name = "System", color = Color.DarkGray, isSystem = true)

        // System card is at index 2 initially
        val mixedList = listOf(virtual1, virtual2, systemCard)
        val sorted = pinSystemCardFirst(mixedList)

        assertEquals(3, sorted.size)
        assertTrue(sorted[0].isSystem)
        assertEquals("sys", sorted[0].id)
        assertEquals("v1", sorted[1].id)
        assertEquals("v2", sorted[2].id)
    }

    @Test
    fun testReorderVirtualEnvironmentsPreservesSystemAtFirst() {
        val systemCard = EnvironmentCardData(id = "sys", name = "System", color = Color.DarkGray, isSystem = true)
        val virtual1 = EnvironmentCardData(id = "v1", name = "Banking", color = SandboxrColors.EnvSlateDark, isSystem = false)
        val virtual2 = EnvironmentCardData(id = "v2", name = "Social", color = SandboxrColors.EnvVioletDark, isSystem = false)
        val virtual3 = EnvironmentCardData(id = "v3", name = "Work", color = SandboxrColors.EnvTealDark, isSystem = false)

        val initialList = listOf(systemCard, virtual1, virtual2, virtual3)

        // Reorder virtual index 0 ("Banking") to virtual index 2 (after "Work")
        val reordered = reorderVirtualEnvironments(initialList, fromVirtualIndex = 0, toVirtualIndex = 2)

        assertEquals(4, reordered.size)
        // System card MUST remain at index 0
        assertTrue(reordered[0].isSystem)
        assertEquals("sys", reordered[0].id)

        // Virtual ordering should now be: Social (v2), Work (v3), Banking (v1)
        assertEquals("v2", reordered[1].id)
        assertEquals("v3", reordered[2].id)
        assertEquals("v1", reordered[3].id)
    }

    @Test
    fun testEnvironmentCardDataDefaults() {
        val sysCard = EnvironmentCardData(id = "system", name = "System", color = Color.Gray, isSystem = true)
        assertTrue(sysCard.isSystem)
        assertEquals(EnvironmentIconType.LOCK, sysCard.iconType)
        assertEquals(NetworkMode.DIRECT, sysCard.networkMode)
        assertFalse(sysCard.hasNotification)
        assertFalse(sysCard.isActive)

        val virtCard = EnvironmentCardData(
            id = "work-uuid",
            name = "Work Container",
            color = SandboxrColors.EnvAmberDark,
            isSystem = false,
            appCount = 7,
            networkMode = NetworkMode.WIREGUARD,
            hasNotification = true,
            notificationCount = 3,
            isActive = true,
            iconType = EnvironmentIconType.TERMINAL
        )
        assertFalse(virtCard.isSystem)
        assertEquals(7, virtCard.appCount)
        assertEquals(NetworkMode.WIREGUARD, virtCard.networkMode)
        assertTrue(virtCard.hasNotification)
        assertEquals(3, virtCard.notificationCount)
        assertTrue(virtCard.isActive)
        assertEquals(EnvironmentIconType.TERMINAL, virtCard.iconType)
    }
}
