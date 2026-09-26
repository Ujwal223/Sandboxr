package com.sandboxr.network.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VpnControllerTest {

    private lateinit var controller: VpnController

    @Before
    fun setUp() {
        controller = VpnController(RuntimeEnvironment.getApplication())
        // Sync to IDLE cleanly for tests that bypass service intents
        controller.syncState(VpnController.VpnState.IDLE)
    }

    @Test
    fun testInitialStateIsIdle() {
        assertEquals(VpnController.VpnState.IDLE, controller.getState())
        assertFalse(controller.isActive())
        assertFalse(controller.isPaused())
    }

    @Test
    fun testStateMachineTransitions() {
        // IDLE → ACTIVE
        controller.syncState(VpnController.VpnState.ACTIVE)
        assertEquals(VpnController.VpnState.ACTIVE, controller.getState())
        assertTrue(controller.isActive())
        assertFalse(controller.isPaused())

        // ACTIVE → PAUSED
        controller.syncState(VpnController.VpnState.PAUSED)
        assertEquals(VpnController.VpnState.PAUSED, controller.getState())
        assertFalse(controller.isActive())
        assertTrue(controller.isPaused())

        // PAUSED → ACTIVE (resume)
        controller.syncState(VpnController.VpnState.ACTIVE)
        assertEquals(VpnController.VpnState.ACTIVE, controller.getState())
        assertTrue(controller.isActive())

        // ACTIVE → IDLE (stop)
        controller.syncState(VpnController.VpnState.IDLE)
        assertEquals(VpnController.VpnState.IDLE, controller.getState())
        assertFalse(controller.isActive())
        assertFalse(controller.isPaused())
    }

    @Test
    fun testPauseRequiresActiveState() {
        // Cannot pause from IDLE
        controller.syncState(VpnController.VpnState.IDLE)
        val paused = controller.pauseCoreDnsVpn()
        assertFalse("Pause from IDLE should fail", paused)
        assertEquals(VpnController.VpnState.IDLE, controller.getState())

        // Cannot pause from PAUSED (already paused)
        controller.syncState(VpnController.VpnState.PAUSED)
        val pausedAgain = controller.pauseCoreDnsVpn()
        assertFalse("Pause from PAUSED should fail", pausedAgain)
    }

    @Test
    fun testResumeRequiresPausedState() {
        // Cannot resume from IDLE
        controller.syncState(VpnController.VpnState.IDLE)
        val resumed = controller.resumeCoreDnsVpn()
        assertFalse("Resume from IDLE should fail", resumed)

        // Cannot resume from ACTIVE (not paused)
        controller.syncState(VpnController.VpnState.ACTIVE)
        val resumedFromActive = controller.resumeCoreDnsVpn()
        assertFalse("Resume from ACTIVE should fail", resumedFromActive)
    }

    @Test
    fun testStopFromAnyNonIdleState() {
        // Stop from ACTIVE
        controller.syncState(VpnController.VpnState.ACTIVE)
        val stopped1 = controller.stopVpn()
        assertTrue(stopped1)
        assertEquals(VpnController.VpnState.IDLE, controller.getState())

        // Stop from PAUSED
        controller.syncState(VpnController.VpnState.PAUSED)
        val stopped2 = controller.stopVpn()
        assertTrue(stopped2)
        assertEquals(VpnController.VpnState.IDLE, controller.getState())
    }

    @Test
    fun testStopFromIdleIsNoOp() {
        controller.syncState(VpnController.VpnState.IDLE)
        val result = controller.stopVpn()
        assertTrue("Stop from IDLE should be safe no-op", result)
        assertEquals(VpnController.VpnState.IDLE, controller.getState())
    }

    @Test
    fun testErrorRecovery() {
        controller.syncState(VpnController.VpnState.ERROR)
        assertEquals(VpnController.VpnState.ERROR, controller.getState())
        assertFalse(controller.isActive())

        controller.resetError()
        assertEquals(VpnController.VpnState.IDLE, controller.getState())
    }

    @Test
    fun testFullAutoVpnHandoffSequence() {
        // Simulate complete Pro auto-VPN handoff lifecycle:
        // 1. SANDBOXR starts normally
        controller.syncState(VpnController.VpnState.ACTIVE)
        assertTrue(controller.isActive())

        // 2. Pro signals external VPN about to activate → pause SANDBOXR tun0
        controller.syncState(VpnController.VpnState.PAUSED)
        assertFalse(controller.isActive())
        assertTrue(controller.isPaused())

        // 3. External VPN (ProtonVPN) is active (SANDBOXR not involved)

        // 4. User switches environment back → external VPN terminates → SANDBOXR resumes
        controller.syncState(VpnController.VpnState.ACTIVE)
        assertTrue(controller.isActive())
        assertFalse(controller.isPaused())

        // 5. App exit
        controller.syncState(VpnController.VpnState.IDLE)
        assertFalse(controller.isActive())
    }
}
