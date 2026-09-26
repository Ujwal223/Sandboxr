package com.sandboxr.network

import com.sandboxr.network.model.NetworkMode
import com.sandboxr.network.model.NetworkRoute
import com.sandboxr.network.vpn.SandboxrVpnService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirestackEngineTest {

    @Before
    fun setUp() {
        FirestackEngine.resetForTesting()
    }

    @After
    fun tearDown() {
        FirestackEngine.resetForTesting()
    }

    @Test
    fun testEngineLifecycle() {
        assertFalse(FirestackEngine.isRunning())
        assertEquals(-1, FirestackEngine.getTunFd())

        // Invalid FD fails
        val startedInvalid = FirestackEngine.start(-1)
        assertFalse(startedInvalid)
        assertFalse(FirestackEngine.isRunning())

        // Valid mock FD succeeds
        val started = FirestackEngine.start(42, 1500)
        assertTrue(started)
        assertTrue(FirestackEngine.isRunning())
        assertEquals(42, FirestackEngine.getTunFd())

        // Stop succeeds
        val stopped = FirestackEngine.stop()
        assertTrue(stopped)
        assertFalse(FirestackEngine.isRunning())
        assertEquals(-1, FirestackEngine.getTunFd())
    }

    @Test
    fun testBlocklistSubMillisecondMatching() {
        val testDomains = listOf(
            "ads.google.com",
            "telemetry.facebook.com",
            "tracker.analytics.io",
            "doubleclick.net"
        )
        FirestackEngine.loadBlocklist(testDomains)

        // Warm up JIT/classloader
        repeat(50) {
            FirestackEngine.isDomainBlocked("subdomain.doubleclick.net")
        }

        val iterations = 100
        val startTime = System.nanoTime()
        var blocked = false
        repeat(iterations) {
            blocked = FirestackEngine.isDomainBlocked("subdomain.doubleclick.net")
        }
        val elapsedTotal = System.nanoTime() - startTime
        val avgNanos = elapsedTotal / iterations

        assertTrue("Subdomain of doubleclick.net should be blocked", blocked)
        assertTrue("Average lookup should be under 1ms (< 1,000,000 ns, actual: ${avgNanos}ns)", avgNanos < 1_000_000)

        // Non-blocked domain
        assertFalse(FirestackEngine.isDomainBlocked("wikipedia.org"))
        assertFalse(FirestackEngine.isDomainBlocked("github.com"))

        val stats = FirestackEngine.getStats()
        assertTrue(stats.dnsQueriesTotal >= 3)
        assertTrue(stats.dnsQueriesBlocked >= 1)
    }

    @Test
    fun testRoutingConfiguration() {
        val routeDirect = NetworkRoute(
            envId = "env-direct",
            mode = NetworkMode.DIRECT,
            adBlockEnabled = true
        )
        val routeSocks = NetworkRoute(
            envId = "env-socks",
            mode = NetworkMode.SOCKS5,
            proxyHost = "127.0.0.1",
            proxyPort = 1080
        )
        val routeWg = NetworkRoute(
            envId = "env-wg",
            mode = NetworkMode.WIREGUARD,
            wgConfig = "[Interface]\nPrivateKey = abc=\n"
        )
        val routeBlocked = NetworkRoute(
            envId = "env-blocked",
            mode = NetworkMode.BLOCKED
        )

        FirestackEngine.setRoute(routeDirect)
        FirestackEngine.setRoute(routeSocks)
        FirestackEngine.setRoute(routeWg)
        FirestackEngine.setRoute(routeBlocked)

        assertEquals(NetworkMode.DIRECT, FirestackEngine.getRoute("env-direct")?.mode)
        assertEquals("127.0.0.1", FirestackEngine.getRoute("env-socks")?.proxyHost)
        assertEquals(1080, FirestackEngine.getRoute("env-socks")?.proxyPort)
        assertEquals(NetworkMode.WIREGUARD, FirestackEngine.getRoute("env-wg")?.mode)
        assertEquals(NetworkMode.BLOCKED, FirestackEngine.getRoute("env-blocked")?.mode)

        assertEquals(4, FirestackEngine.getStats().activeRoutesCount)

        FirestackEngine.removeRoute("env-direct")
        assertEquals(3, FirestackEngine.getStats().activeRoutesCount)
        assertEquals(null, FirestackEngine.getRoute("env-direct"))
    }

    @Test
    fun testVpnServiceConstantsAndConfig() {
        assertEquals("10.111.222.1", SandboxrVpnService.LOCAL_IP_V4)
        assertEquals("fd00::1", SandboxrVpnService.LOCAL_IP_V6)
        assertEquals(1500, SandboxrVpnService.VPN_MTU)
        assertEquals("Sandboxr Local Firewall", SandboxrVpnService.SESSION_NAME)
    }
}
