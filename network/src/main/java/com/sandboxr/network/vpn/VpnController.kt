package com.sandboxr.network.vpn

import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Auto-VPN Handoff State Machine for SANDBOXR Core Network Service.
 *
 * Manages the coordinated pause and resume of the local SANDBOXR tun0 interface
 * when an external system VPN (e.g., ProtonVPN, Mullvad, Tailscale) needs to
 * temporarily claim the Android VpnService slot.
 *
 * Android limitation: only ONE VpnService can be active at a time per UID group.
 * When a system-level VPN is activated (e.g., via Pro auto-VPN trigger), SANDBOXR
 * must cleanly yield control of its tun0 interface, and automatically re-establish
 * it when the external VPN disconnects.
 *
 * State machine transitions:
 *
 *   IDLE ──startVpn()──▶ ACTIVE
 *   ACTIVE ──pauseVpn()──▶ PAUSED
 *   PAUSED ──resumeVpn()──▶ ACTIVE
 *   ACTIVE / PAUSED ──stopVpn()──▶ IDLE
 *
 * Referenced by PRD Section 10.6 "Auto-VPN on Environment Open"
 * and PROJECT_EXECUTION_PLAN Phase 5, Task P05-T06.
 */
class VpnController(private val context: Context) {

    companion object {
        private const val TAG = "VpnController"

        /** Minimum cooldown between rapid pause/resume cycles (ms) */
        private const val HANDOFF_COOLDOWN_MS = 500L
    }

    enum class VpnState {
        IDLE,    // No VPN active
        ACTIVE,  // SANDBOXR tun0 is running
        PAUSED,  // tun0 yielded to external VPN
        ERROR    // Last operation failed
    }

    private val state = AtomicReference(VpnState.IDLE)
    private val lastStateChangeMs = AtomicLong(0)
    private val isTransitioning = AtomicBoolean(false)

    /**
     * Returns the current VPN state machine state.
     */
    fun getState(): VpnState = state.get()

    /**
     * Returns whether SANDBOXR's local tun0 is active.
     */
    fun isActive(): Boolean = state.get() == VpnState.ACTIVE

    /**
     * Returns whether the VPN is currently paused (yielded to external VPN).
     */
    fun isPaused(): Boolean = state.get() == VpnState.PAUSED

    /**
     * Starts the SANDBOXR local VpnService.
     * Transitions IDLE → ACTIVE.
     *
     * @return true if transition succeeded.
     */
    fun startVpn(): Boolean {
        if (!transitionAllowed()) return false

        val current = state.get()
        if (current != VpnState.IDLE) {
            Log.w(TAG, "startVpn() called in state $current — ignoring")
            return false
        }

        return try {
            val intent = Intent(context, SandboxrVpnService::class.java).apply {
                action = SandboxrVpnService.ACTION_START
            }
            context.startForegroundService(intent)
            state.set(VpnState.ACTIVE)
            lastStateChangeMs.set(System.currentTimeMillis())
            Log.i(TAG, "VPN started: IDLE → ACTIVE")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN service", e)
            state.set(VpnState.ERROR)
            false
        }
    }

    /**
     * Pauses the SANDBOXR tun0 interface to yield control to an external VPN.
     * Transitions ACTIVE → PAUSED.
     *
     * Called by Pro companion auto-VPN handoff protocol when a system VPN is
     * about to be activated.
     *
     * @return true if pause was accepted and executed.
     */
    fun pauseCoreDnsVpn(): Boolean {
        if (!transitionAllowed()) return false

        val current = state.get()
        if (current != VpnState.ACTIVE) {
            Log.w(TAG, "pauseCoreDnsVpn() called in state $current — ignoring")
            return false
        }

        return try {
            val intent = Intent(context, SandboxrVpnService::class.java).apply {
                action = SandboxrVpnService.ACTION_PAUSE
            }
            context.startService(intent)
            state.set(VpnState.PAUSED)
            lastStateChangeMs.set(System.currentTimeMillis())
            Log.i(TAG, "VPN paused: ACTIVE → PAUSED (yielding to external VPN)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause VPN service", e)
            state.set(VpnState.ERROR)
            false
        }
    }

    /**
     * Resumes the SANDBOXR tun0 interface after an external VPN has disconnected.
     * Transitions PAUSED → ACTIVE.
     *
     * Called automatically when Pro companion signals external VPN has terminated.
     *
     * @return true if resume succeeded.
     */
    fun resumeCoreDnsVpn(): Boolean {
        if (!transitionAllowed()) return false

        val current = state.get()
        if (current != VpnState.PAUSED) {
            Log.w(TAG, "resumeCoreDnsVpn() called in state $current — ignoring")
            return false
        }

        return try {
            val intent = Intent(context, SandboxrVpnService::class.java).apply {
                action = SandboxrVpnService.ACTION_RESUME
            }
            context.startService(intent)
            state.set(VpnState.ACTIVE)
            lastStateChangeMs.set(System.currentTimeMillis())
            Log.i(TAG, "VPN resumed: PAUSED → ACTIVE (SANDBOXR tun0 re-established)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume VPN service", e)
            state.set(VpnState.ERROR)
            false
        }
    }

    /**
     * Stops the SANDBOXR VPN service entirely.
     * Transitions ACTIVE/PAUSED → IDLE.
     */
    fun stopVpn(): Boolean {
        val current = state.get()
        if (current == VpnState.IDLE) return true

        return try {
            val intent = Intent(context, SandboxrVpnService::class.java).apply {
                action = SandboxrVpnService.ACTION_STOP
            }
            context.startService(intent)
            state.set(VpnState.IDLE)
            lastStateChangeMs.set(System.currentTimeMillis())
            Log.i(TAG, "VPN stopped: $current → IDLE")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop VPN service", e)
            state.set(VpnState.ERROR)
            false
        }
    }

    /**
     * Recovers from ERROR state by resetting to IDLE.
     */
    fun resetError() {
        if (state.compareAndSet(VpnState.ERROR, VpnState.IDLE)) {
            Log.i(TAG, "VPN controller error cleared: ERROR → IDLE")
        }
    }

    /**
     * Sets state directly for testing or service-driven sync (when service notifies controller).
     */
    internal fun syncState(newState: VpnState) {
        state.set(newState)
    }

    private fun transitionAllowed(): Boolean {
        val elapsed = System.currentTimeMillis() - lastStateChangeMs.get()
        if (elapsed < HANDOFF_COOLDOWN_MS) {
            Log.w(TAG, "State transition throttled (${elapsed}ms < ${HANDOFF_COOLDOWN_MS}ms cooldown)")
            return false
        }
        return true
    }
}
