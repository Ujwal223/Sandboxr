package com.sandboxr.network.router

import android.util.Log
import com.sandboxr.network.FirestackEngine
import com.sandboxr.network.model.NetworkMode
import com.sandboxr.network.model.NetworkRoute
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Per-Environment Network Routing Dispatcher.
 *
 * Implements socket-level and packet-level dispatching according to each
 * container environment's configured [NetworkMode] (DIRECT, SOCKS5, WIREGUARD, BLOCKED).
 *
 * - BLOCKED: Instantly drops all outbound traffic.
 * - SOCKS5: Tunnels TCP connections through user-specified SOCKS5 proxy endpoint.
 * - WIREGUARD: Routes traffic into embedded WireGuard tunnel interface.
 * - DIRECT: Normal zero-latency direct routing to host network interfaces.
 */
object NetworkRouter {

    private const val TAG = "NetworkRouter"

    enum class RouteAction {
        PASS_DIRECT,
        PROXY_SOCKS5,
        TUNNEL_WIREGUARD,
        DROP
    }

    data class RoutingDecision(
        val action: RouteAction,
        val envId: String,
        val targetHost: String,
        val targetPort: Int,
        val proxyHost: String? = null,
        val proxyPort: Int? = null,
        val wgConfig: String? = null,
        val reason: String = ""
    )

    private val routes = ConcurrentHashMap<String, NetworkRoute>()

    @Volatile
    private var activeEnvId: String = ""

    // Telemetry metrics
    val droppedPackets = AtomicLong(0)
    val directPackets = AtomicLong(0)
    val socks5Packets = AtomicLong(0)
    val wireguardPackets = AtomicLong(0)

    /**
     * Registers or updates an environment's network routing configuration.
     */
    fun registerRoute(route: NetworkRoute) {
        routes[route.envId] = route
        FirestackEngine.setRoute(route)
        Log.d(TAG, "Registered route for env ${route.envId} with mode ${route.mode}")
    }

    /**
     * Removes routing configuration for a deleted environment.
     */
    fun unregisterRoute(envId: String) {
        routes.remove(envId)
        FirestackEngine.removeRoute(envId)
        Log.d(TAG, "Unregistered route for env $envId")
    }

    /**
     * Sets the currently active foreground environment ID.
     */
    fun setActiveEnvironment(envId: String) {
        activeEnvId = envId
        Log.d(TAG, "Active environment set to: $envId")
    }

    /**
     * Returns the currently active environment ID.
     */
    fun getActiveEnvironment(): String = activeEnvId

    /**
     * Retrieves routing configuration for an environment.
     */
    fun getRoute(envId: String): NetworkRoute? = routes[envId]

    /**
     * Evaluates packet routing action for an environment targeting a destination.
     */
    fun evaluateRouting(envId: String, targetHost: String, targetPort: Int): RoutingDecision {
        val route = routes[envId] ?: NetworkRoute(envId = envId, mode = NetworkMode.DIRECT)

        return when (route.mode) {
            NetworkMode.BLOCKED -> {
                droppedPackets.incrementAndGet()
                RoutingDecision(
                    action = RouteAction.DROP,
                    envId = envId,
                    targetHost = targetHost,
                    targetPort = targetPort,
                    reason = "Environment $envId network mode is BLOCKED: dropping outbound packet"
                )
            }

            NetworkMode.SOCKS5 -> {
                if (route.proxyHost.isNullOrBlank() || route.proxyPort == null || route.proxyPort <= 0) {
                    droppedPackets.incrementAndGet()
                    RoutingDecision(
                        action = RouteAction.DROP,
                        envId = envId,
                        targetHost = targetHost,
                        targetPort = targetPort,
                        reason = "SOCKS5 proxy host or port invalid: dropping packet"
                    )
                } else {
                    socks5Packets.incrementAndGet()
                    RoutingDecision(
                        action = RouteAction.PROXY_SOCKS5,
                        envId = envId,
                        targetHost = targetHost,
                        targetPort = targetPort,
                        proxyHost = route.proxyHost,
                        proxyPort = route.proxyPort,
                        reason = "Routing via SOCKS5 proxy ${route.proxyHost}:${route.proxyPort}"
                    )
                }
            }

            NetworkMode.WIREGUARD -> {
                if (route.wgConfig.isNullOrBlank()) {
                    droppedPackets.incrementAndGet()
                    RoutingDecision(
                        action = RouteAction.DROP,
                        envId = envId,
                        targetHost = targetHost,
                        targetPort = targetPort,
                        reason = "WireGuard configuration missing: dropping packet"
                    )
                } else {
                    wireguardPackets.incrementAndGet()
                    RoutingDecision(
                        action = RouteAction.TUNNEL_WIREGUARD,
                        envId = envId,
                        targetHost = targetHost,
                        targetPort = targetPort,
                        wgConfig = route.wgConfig,
                        reason = "Routing via embedded WireGuard tunnel"
                    )
                }
            }

            NetworkMode.DIRECT -> {
                directPackets.incrementAndGet()
                RoutingDecision(
                    action = RouteAction.PASS_DIRECT,
                    envId = envId,
                    targetHost = targetHost,
                    targetPort = targetPort,
                    reason = "Routing direct to host interface"
                )
            }
        }
    }

    /**
     * Executes RFC 1928 SOCKS5 client handshake on an established socket.
     * Negotiates NO AUTH (0x00) and sends CONNECT command for targetHost:targetPort.
     */
    fun performSocks5Handshake(
        socket: Socket,
        targetHost: String,
        targetPort: Int
    ): Boolean {
        try {
            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            // 1. Version negotiation: [VER = 0x05, NMETHODS = 1, METHOD = 0x00]
            output.write(byteArrayOf(0x05, 0x01, 0x00))
            output.flush()

            val authResp = ByteArray(2)
            readExact(input, authResp)
            if (authResp[0].toInt() != 0x05 || authResp[1].toInt() != 0x00) {
                Log.e(TAG, "SOCKS5 proxy rejected authentication: ${authResp.contentToString()}")
                return false
            }

            // 2. Request: [VER = 5, CMD = 1 (CONNECT), RSV = 0, ATYP = 3 (Domain), LEN, NAME..., PORT_HI, PORT_LO]
            val hostBytes = targetHost.toByteArray(Charsets.US_ASCII)
            val req = ByteArray(4 + 1 + hostBytes.size + 2)
            req[0] = 0x05
            req[1] = 0x01
            req[2] = 0x00
            req[3] = 0x03 // Domain name
            req[4] = hostBytes.size.toByte()
            System.arraycopy(hostBytes, 0, req, 5, hostBytes.size)
            req[req.size - 2] = (targetPort ushr 8).toByte()
            req[req.size - 1] = (targetPort and 0xFF).toByte()

            output.write(req)
            output.flush()

            // 3. Response: [VER, REP, RSV, ATYP, BND.ADDR..., BND.PORT]
            val respHeader = ByteArray(4)
            readExact(input, respHeader)
            if (respHeader[1].toInt() != 0x00) {
                Log.e(TAG, "SOCKS5 proxy connection failed with REP: 0x${respHeader[1].toString(16)}")
                return false
            }

            // Drain bound address based on ATYP
            when (respHeader[3].toInt()) {
                0x01 -> readExact(input, ByteArray(4 + 2)) // IPv4 + Port
                0x04 -> readExact(input, ByteArray(16 + 2)) // IPv6 + Port
                0x03 -> {
                    val len = input.read()
                    if (len > 0) readExact(input, ByteArray(len + 2))
                }
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error executing SOCKS5 handshake", e)
            return false
        }
    }

    /**
     * Dials and establishes a SOCKS5 proxied connection.
     */
    fun openSocks5Socket(
        proxyHost: String,
        proxyPort: Int,
        targetHost: String,
        targetPort: Int,
        timeoutMs: Int = 10000
    ): Socket? {
        val socket = Socket()
        return try {
            socket.connect(InetSocketAddress(proxyHost, proxyPort), timeoutMs)
            val success = performSocks5Handshake(socket, targetHost, targetPort)
            if (success) {
                socket
            } else {
                socket.close()
                null
            }
        } catch (e: Exception) {
            try { socket.close() } catch (_: Exception) {}
            null
        }
    }

    private fun readExact(stream: InputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = stream.read(buffer, offset, buffer.size - offset)
            if (read == -1) throw java.io.EOFException("Premature EOF during SOCKS5 handshake")
            offset += read
        }
    }

    /**
     * Resets routes and counters (for testing).
     */
    fun resetForTesting() {
        routes.clear()
        activeEnvId = ""
        droppedPackets.set(0)
        directPackets.set(0)
        socks5Packets.set(0)
        wireguardPackets.set(0)
    }
}
