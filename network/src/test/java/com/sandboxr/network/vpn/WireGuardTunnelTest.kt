package com.sandboxr.network.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WireGuardTunnelTest {

    private val validConf = """
        [Interface]
        PrivateKey = aaaaaabbbbbbccccccddddddeeeeeeffffff1111=
        Address = 10.0.0.2/32, fd00::2/128
        DNS = 1.1.1.1
        MTU = 1420

        [Peer]
        PublicKey = 1111112222223333334444445555556666667777=
        Endpoint = 198.51.100.1:51820
        AllowedIPs = 0.0.0.0/0, ::/0
        PresharedKey = psk123==
        PersistentKeepalive = 25
    """.trimIndent()

    @Test
    fun testParseValidConfig() {
        val cfg = WireGuardTunnel.parseConfig(validConf)

        assertEquals("aaaaaabbbbbbccccccddddddeeeeeeffffff1111=", cfg.privateKey)
        assertEquals("1111112222223333334444445555556666667777=", cfg.publicKey)
        assertEquals("198.51.100.1:51820", cfg.endpoint)
        assertEquals("198.51.100.1", cfg.endpointHost)
        assertEquals(51820, cfg.endpointPort)
        assertEquals(listOf("10.0.0.2/32", "fd00::2/128"), cfg.addresses)
        assertEquals(listOf("0.0.0.0/0", "::/0"), cfg.allowedIPs)
        assertEquals("1.1.1.1", cfg.dns)
        assertEquals(1420, cfg.mtu)
        assertEquals("psk123==", cfg.presharedKey)
        assertEquals(25, cfg.persistentKeepalive)
    }

    @Test
    fun testParseMinimalConfig() {
        val minConf = """
            [Interface]
            PrivateKey = minkey=
            Address = 10.0.0.5/32

            [Peer]
            PublicKey = peerkey=
            Endpoint = 203.0.113.50:51820
        """.trimIndent()

        val cfg = WireGuardTunnel.parseConfig(minConf)
        assertEquals("minkey=", cfg.privateKey)
        assertEquals("peerkey=", cfg.publicKey)
        assertEquals(1420, cfg.mtu) // Default
        assertEquals(0, cfg.persistentKeepalive) // Default
        assertEquals(listOf("0.0.0.0/0", "::/0"), cfg.allowedIPs) // Default
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseFailsMissingPrivateKey() {
        WireGuardTunnel.parseConfig("""
            [Interface]
            Address = 10.0.0.2/32
            [Peer]
            PublicKey = abc=
            Endpoint = 1.2.3.4:51820
        """.trimIndent())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseFailsInvalidEndpointPort() {
        WireGuardTunnel.parseConfig("""
            [Interface]
            PrivateKey = abc=
            Address = 10.0.0.2/32
            [Peer]
            PublicKey = def=
            Endpoint = 1.2.3.4:notaport
        """.trimIndent())
    }

    @Test
    fun testTunnelLifecycle() {
        val tunnel = WireGuardTunnel("env-wg-001")

        assertFalse(tunnel.isActive())
        assertNull(tunnel.getConfig())

        val applied = tunnel.setConfig(validConf)
        assertTrue(applied)
        assertNotNull(tunnel.getConfig())
        assertEquals("env-wg-001", tunnel.getConfig()!!.envId)

        tunnel.activate()
        assertTrue(tunnel.isActive())

        tunnel.deactivate()
        assertFalse(tunnel.isActive())
    }

    @Test
    fun testValidateConfigErrors() {
        val tunnel = WireGuardTunnel("env-wg-002")

        val missingPeerKey = """
            [Interface]
            PrivateKey = abc=
            Address = 10.0.0.2/32
            [Peer]
            Endpoint = 1.2.3.4:51820
        """.trimIndent()

        val error = tunnel.validateConfig(missingPeerKey)
        assertNotNull(error)
        assertTrue(error!!.contains("PublicKey"))

        val validError = tunnel.validateConfig(validConf)
        assertNull(validError)
    }

    @Test
    fun testToConfStringRoundTrip() {
        val cfg = WireGuardTunnel.parseConfig(validConf)
        val serialized = cfg.toConfString()

        // Re-parse the serialized string
        val reparsed = WireGuardTunnel.parseConfig(serialized)

        assertEquals(cfg.privateKey, reparsed.privateKey)
        assertEquals(cfg.publicKey, reparsed.publicKey)
        assertEquals(cfg.endpoint, reparsed.endpoint)
        assertEquals(cfg.addresses, reparsed.addresses)
        assertEquals(cfg.allowedIPs, reparsed.allowedIPs)
        assertEquals(cfg.mtu, reparsed.mtu)
        assertEquals(cfg.persistentKeepalive, reparsed.persistentKeepalive)
        assertEquals(cfg.presharedKey, reparsed.presharedKey)
    }

    @Test
    fun testSetInvalidConfigDoesNotCrash() {
        val tunnel = WireGuardTunnel("env-wg-003")
        val result = tunnel.setConfig("garbage config with no valid sections")
        assertFalse(result)
        assertNull(tunnel.getConfig())
    }

    @Test
    fun testActivateWithoutConfigFails() {
        val tunnel = WireGuardTunnel("env-wg-004")
        val activated = tunnel.activate()
        assertFalse(activated)
        assertFalse(tunnel.isActive())
    }
}
