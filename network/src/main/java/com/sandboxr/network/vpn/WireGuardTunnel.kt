package com.sandboxr.network.vpn

import android.util.Log
import java.io.BufferedReader
import java.io.StringReader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WireGuard Tunnel Manager for per-environment embedded tunneling.
 *
 * Parses standard wg0.conf format blocks, validates keys and endpoints,
 * and manages tunnel lifecycle per virtual environment.
 *
 * Integration note: On Android, actual WireGuard packet forwarding uses the
 * WireGuard-Android library (com.wireguard.android.backend) or the kernel
 * wireguard module via VpnService. This class provides the config management
 * and lifecycle layer; the packet path hooks into [SandboxrVpnService] which
 * routes environment-tagged sockets through the tunnel interface.
 */
class WireGuardTunnel(val envId: String) {

    companion object {
        private const val TAG = "WireGuardTunnel"

        /**
         * Parses a standard wg0.conf block into a [WireGuardConfig].
         * Supports [Interface] and [Peer] sections.
         * All key names are case-insensitive.
         *
         * @throws IllegalArgumentException if required fields are missing.
         */
        fun parseConfig(configContent: String): WireGuardConfig {
            val iface = mutableMapOf<String, String>()
            val peer = mutableMapOf<String, String>()
            var section = ""

            BufferedReader(StringReader(configContent)).forEachLine { rawLine ->
                val line = rawLine.trim()
                if (line.isBlank() || line.startsWith("#")) return@forEachLine

                if (line.startsWith("[") && line.endsWith("]")) {
                    section = line.lowercase()
                    return@forEachLine
                }

                val eqIdx = line.indexOf('=')
                if (eqIdx < 1) return@forEachLine
                val key = line.substring(0, eqIdx).trim().lowercase()
                val value = line.substring(eqIdx + 1).trim()

                when (section) {
                    "[interface]" -> iface[key] = value
                    "[peer]" -> peer[key] = value
                }
            }

            val privateKey = iface["privatekey"]
                ?: throw IllegalArgumentException("Missing [Interface] PrivateKey")
            val address = iface["address"]
                ?: throw IllegalArgumentException("Missing [Interface] Address")
            val publicKey = peer["publickey"]
                ?: throw IllegalArgumentException("Missing [Peer] PublicKey")
            val endpoint = peer["endpoint"]
                ?: throw IllegalArgumentException("Missing [Peer] Endpoint")

            val endpointParts = endpoint.split(":")
            if (endpointParts.size < 2) {
                throw IllegalArgumentException("Invalid Endpoint format (expected host:port): $endpoint")
            }

            return WireGuardConfig(
                envId = "",
                privateKey = privateKey,
                publicKey = publicKey,
                addresses = address.split(",").map { it.trim() },
                endpoint = endpoint,
                endpointHost = endpointParts.dropLast(1).joinToString(":"),
                endpointPort = endpointParts.last().toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid endpoint port in: $endpoint"),
                allowedIPs = peer["allowedips"]?.split(",")?.map { it.trim() } ?: listOf("0.0.0.0/0", "::/0"),
                dns = iface["dns"],
                mtu = iface["mtu"]?.toIntOrNull() ?: 1420,
                presharedKey = peer["presharedkey"],
                persistentKeepalive = peer["persistentkeepalive"]?.toIntOrNull() ?: 0
            )
        }
    }

    @Volatile
    private var config: WireGuardConfig? = null
    private val active = AtomicBoolean(false)

    /**
     * Applies and activates a WireGuard configuration block.
     * Returns true if config is valid and parsed successfully.
     */
    fun setConfig(configContent: String): Boolean {
        return try {
            val parsed = parseConfig(configContent)
            config = parsed.copy(envId = envId)
            Log.i(TAG, "WireGuard config applied for env=$envId endpoint=${parsed.endpoint}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Invalid WireGuard config for env=$envId: ${e.message}")
            false
        }
    }

    /**
     * Returns the currently applied WireGuard configuration.
     */
    fun getConfig(): WireGuardConfig? = config

    /**
     * Marks the tunnel as active (called when VpnService establishes the WG interface).
     */
    fun activate(): Boolean {
        val cfg = config ?: run {
            Log.w(TAG, "Cannot activate tunnel for env=$envId: no config loaded")
            return false
        }
        active.set(true)
        Log.i(TAG, "WireGuard tunnel activated for env=$envId via ${cfg.endpoint}")
        return true
    }

    /**
     * Marks the tunnel as inactive (called on environment switch or termination).
     */
    fun deactivate() {
        active.set(false)
        Log.i(TAG, "WireGuard tunnel deactivated for env=$envId")
    }

    /**
     * Returns whether the tunnel is currently active.
     */
    fun isActive(): Boolean = active.get()

    /**
     * Validates a config block without applying it.
     * Returns error message string or null if valid.
     */
    fun validateConfig(configContent: String): String? {
        return try {
            parseConfig(configContent)
            null
        } catch (e: Exception) {
            e.message
        }
    }
}

/**
 * Parsed and validated WireGuard configuration.
 */
data class WireGuardConfig(
    val envId: String,
    val privateKey: String,
    val publicKey: String,
    val addresses: List<String>,
    val endpoint: String,
    val endpointHost: String,
    val endpointPort: Int,
    val allowedIPs: List<String>,
    val dns: String? = null,
    val mtu: Int = 1420,
    val presharedKey: String? = null,
    val persistentKeepalive: Int = 0
) {
    /**
     * Regenerates a wg-quick compatible .conf string for export or handoff.
     */
    fun toConfString(): String = buildString {
        appendLine("[Interface]")
        appendLine("PrivateKey = $privateKey")
        appendLine("Address = ${addresses.joinToString(", ")}")
        if (!dns.isNullOrBlank()) appendLine("DNS = $dns")
        appendLine("MTU = $mtu")
        appendLine()
        appendLine("[Peer]")
        appendLine("PublicKey = $publicKey")
        appendLine("Endpoint = $endpoint")
        appendLine("AllowedIPs = ${allowedIPs.joinToString(", ")}")
        if (!presharedKey.isNullOrBlank()) appendLine("PresharedKey = $presharedKey")
        if (persistentKeepalive > 0) appendLine("PersistentKeepalive = $persistentKeepalive")
    }
}
