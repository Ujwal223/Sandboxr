package com.sandboxr.network.router

import com.sandboxr.network.model.NetworkMode
import com.sandboxr.network.model.NetworkRoute
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class NetworkRouterTest {

    @Before
    fun setUp() {
        NetworkRouter.resetForTesting()
    }

    @After
    fun tearDown() {
        NetworkRouter.resetForTesting()
    }

    @Test
    fun testBlockedModeDropsPackets() {
        val envBlocked = "env-blocked-uuid"
        NetworkRouter.registerRoute(
            NetworkRoute(
                envId = envBlocked,
                mode = NetworkMode.BLOCKED
            )
        )

        val decision = NetworkRouter.evaluateRouting(envBlocked, "api.example.com", 443)
        assertEquals(NetworkRouter.RouteAction.DROP, decision.action)
        assertEquals(1, NetworkRouter.droppedPackets.get())

        val decision2 = NetworkRouter.evaluateRouting(envBlocked, "8.8.8.8", 53)
        assertEquals(NetworkRouter.RouteAction.DROP, decision2.action)
        assertEquals(2, NetworkRouter.droppedPackets.get())
    }

    @Test
    fun testDirectModePassesPackets() {
        val envDirect = "env-direct-uuid"
        NetworkRouter.registerRoute(
            NetworkRoute(
                envId = envDirect,
                mode = NetworkMode.DIRECT
            )
        )

        val decision = NetworkRouter.evaluateRouting(envDirect, "github.com", 443)
        assertEquals(NetworkRouter.RouteAction.PASS_DIRECT, decision.action)
        assertEquals(1, NetworkRouter.directPackets.get())
    }

    @Test
    fun testSocks5ModeConfigAndValidation() {
        val envSocks = "env-socks-uuid"
        NetworkRouter.registerRoute(
            NetworkRoute(
                envId = envSocks,
                mode = NetworkMode.SOCKS5,
                proxyHost = "10.0.0.1",
                proxyPort = 1080
            )
        )

        val decision = NetworkRouter.evaluateRouting(envSocks, "example.org", 80)
        assertEquals(NetworkRouter.RouteAction.PROXY_SOCKS5, decision.action)
        assertEquals("10.0.0.1", decision.proxyHost)
        assertEquals(1080, decision.proxyPort)
        assertEquals(1, NetworkRouter.socks5Packets.get())

        // Invalid config should drop
        val envSocksBad = "env-socks-bad"
        NetworkRouter.registerRoute(
            NetworkRoute(
                envId = envSocksBad,
                mode = NetworkMode.SOCKS5,
                proxyHost = null,
                proxyPort = 0
            )
        )
        val badDecision = NetworkRouter.evaluateRouting(envSocksBad, "example.org", 80)
        assertEquals(NetworkRouter.RouteAction.DROP, badDecision.action)
    }

    @Test
    fun testWireGuardModeRouting() {
        val envWg = "env-wg-uuid"
        val sampleWgConfig = """
            [Interface]
            PrivateKey = aabbcc==
            Address = 10.0.0.2/32
            [Peer]
            PublicKey = ddeeff==
            Endpoint = 203.0.113.1:51820
        """.trimIndent()

        NetworkRouter.registerRoute(
            NetworkRoute(
                envId = envWg,
                mode = NetworkMode.WIREGUARD,
                wgConfig = sampleWgConfig
            )
        )

        val decision = NetworkRouter.evaluateRouting(envWg, "secure.bank.com", 443)
        assertEquals(NetworkRouter.RouteAction.TUNNEL_WIREGUARD, decision.action)
        assertEquals(sampleWgConfig, decision.wgConfig)
        assertEquals(1, NetworkRouter.wireguardPackets.get())
    }

    @Test
    fun testSocks5HandshakeFlow() {
        // Start a mock SOCKS5 server on a local ephemeral port
        val server = ServerSocket(0)
        val port = server.localPort

        val serverThread = thread {
            try {
                val client = server.accept()
                val input = client.getInputStream()
                val output = client.getOutputStream()

                // Step 1: Read client handshake [0x05, 0x01, 0x00]
                val verReq = ByteArray(3)
                input.read(verReq)
                assertEquals(0x05, verReq[0].toInt())

                // Send auth response: [0x05, 0x00]
                output.write(byteArrayOf(0x05, 0x00))
                output.flush()

                // Step 2: Read CONNECT request [0x05, 0x01, 0x00, 0x03, len, domain..., port]
                val reqHdr = ByteArray(4)
                input.read(reqHdr)
                assertEquals(0x05, reqHdr[0].toInt())
                assertEquals(0x01, reqHdr[1].toInt()) // CONNECT

                val atyp = reqHdr[3].toInt()
                if (atyp == 0x03) {
                    val len = input.read()
                    val hostBuf = ByteArray(len)
                    input.read(hostBuf)
                    val portBuf = ByteArray(2)
                    input.read(portBuf)
                }

                // Send success response: [0x05, 0x00, 0x00, 0x01, 0,0,0,0, 0,0]
                output.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                output.flush()

                client.close()
            } catch (_: Exception) {}
        }

        // Test client handshake
        val clientSocket = Socket("127.0.0.1", port)
        val handshakeOk = NetworkRouter.performSocks5Handshake(clientSocket, "target.domain.com", 443)
        assertTrue("SOCKS5 handshake should succeed with mock server", handshakeOk)

        clientSocket.close()
        server.close()
        serverThread.join(2000)
    }

    @Test
    fun testUnregisteredEnvDefaultsToDirect() {
        val decision = NetworkRouter.evaluateRouting("unknown-env-999", "kernel.org", 80)
        assertEquals(NetworkRouter.RouteAction.PASS_DIRECT, decision.action)
    }

    @Test
    fun testUnregisterRoute() {
        NetworkRouter.registerRoute(
            NetworkRoute(
                envId = "temp-env",
                mode = NetworkMode.BLOCKED
            )
        )
        assertNotNull(NetworkRouter.getRoute("temp-env"))

        NetworkRouter.unregisterRoute("temp-env")
        assertEquals(null, NetworkRouter.getRoute("temp-env"))

        val decision = NetworkRouter.evaluateRouting("temp-env", "example.com", 80)
        assertEquals(NetworkRouter.RouteAction.PASS_DIRECT, decision.action)
    }
}
