package com.sandboxr.launcher.ui

import androidx.compose.ui.unit.dp
import com.sandboxr.launcher.theme.SandboxrSpacingTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SandboxrNavBarTest {

    @Test
    fun testNavTabEnumAndLabels() {
        assertEquals(4, NavTab.entries.size)
        assertEquals("Environments", NavTab.ENVIRONMENTS.label)
        assertEquals("Firewall", NavTab.FIREWALL.label)
        assertEquals("Vault", NavTab.VAULT.label)
        assertEquals("Settings", NavTab.SETTINGS.label)
    }

    @Test
    fun testNavBarAndBottomSheetDimensions() {
        assertEquals(72.dp, SandboxrSpacingTokens.NavBarHeight)
        assertEquals(28.dp, SandboxrSpacingTokens.BottomSheetRadius)
        assertEquals(32.dp, SandboxrSpacingTokens.BottomSheetHandleWidth)
        assertEquals(4.dp, SandboxrSpacingTokens.BottomSheetHandleHeight)
    }

    @Test
    fun testActionSheetItemExecution() {
        var clicked = false
        val item = ActionSheetItem(
            id = "delete",
            title = "Delete Container",
            subtitle = "Irreversible action",
            isDestructive = true,
            onClick = { clicked = true }
        )

        assertEquals("delete", item.id)
        assertEquals("Delete Container", item.title)
        assertEquals("Irreversible action", item.subtitle)
        assertTrue(item.isDestructive)

        item.onClick()
        assertTrue(clicked)
    }

    @Test
    fun testStandardActionSheetItem() {
        val item = ActionSheetItem(
            id = "settings",
            title = "Container Settings",
            onClick = {}
        )
        assertFalse(item.isDestructive)
    }
}
