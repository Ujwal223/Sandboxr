package com.sandboxr.network.dns

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

class DnsBlockerTest {

    private val blocker = DnsBlocker.instance

    @Before
    fun setUp() {
        blocker.clear()
    }

    @After
    fun tearDown() {
        blocker.clear()
    }

    @Test
    fun testHostsStreamParsing() {
        val hostsContent = """
            # Steven Black hosts test snippet
            127.0.0.1 localhost
            ::1 localhost
            0.0.0.0 0.0.0.0
            
            0.0.0.0 doubleclick.net
            0.0.0.0 adservice.google.com
            0.0.0.0 analytics.tiktok.com
            0.0.0.0 telemetry.facebook.com
            tracker.single.entry.com
        """.trimIndent()

        val added = blocker.loadFromStream(ByteArrayInputStream(hostsContent.toByteArray()))
        assertEquals(5, added)
        assertEquals(5, blocker.domainCount)

        assertTrue(blocker.isBlocked("doubleclick.net"))
        assertTrue(blocker.isBlocked("adservice.google.com"))
        assertTrue(blocker.isBlocked("analytics.tiktok.com"))
        assertTrue(blocker.isBlocked("telemetry.facebook.com"))
        assertTrue(blocker.isBlocked("tracker.single.entry.com"))

        // Excluded hosts
        assertFalse(blocker.isBlocked("localhost"))
        assertFalse(blocker.isBlocked("0.0.0.0"))
        assertFalse(blocker.isBlocked("wikipedia.org"))
    }

    @Test
    fun testSubdomainHierarchicalBlocking() {
        blocker.addDomains(listOf("doubleclick.net", "facebook.com"))

        // Direct match
        assertTrue(blocker.isBlocked("doubleclick.net"))
        // Subdomains
        assertTrue(blocker.isBlocked("ad.doubleclick.net"))
        assertTrue(blocker.isBlocked("deep.nested.subdomain.doubleclick.net"))
        assertTrue(blocker.isBlocked("pixel.facebook.com"))

        // Non-blocked
        assertFalse(blocker.isBlocked("google.com"))
        assertFalse(blocker.isBlocked("notdoubleclick.net"))
    }

    @Test
    fun testSubMillisecondLookupBenchmark() {
        val testDomains = (1..500).map { "tracker-$it.advertising-network.org" } + listOf("doubleclick.net")
        blocker.addDomains(testDomains)

        // Warm up JIT
        repeat(50) {
            blocker.isBlocked("ad.sub.doubleclick.net")
        }

        // Benchmark 100 queries
        val iterations = 100
        val startTime = System.nanoTime()
        var blockedCount = 0
        repeat(iterations) {
            if (blocker.isBlocked("ad.sub.doubleclick.net")) {
                blockedCount++
            }
        }
        val elapsedTotal = System.nanoTime() - startTime
        val avgNanos = elapsedTotal / iterations

        assertEquals(iterations, blockedCount)
        assertTrue(
            "Query matching must be sub-millisecond (< 1,000,000 ns, actual: ${avgNanos}ns)",
            avgNanos < 1_000_000
        )
    }

    @Test
    fun testSyntheticDnsResponseForBlockedDomain() {
        blocker.addDomains(listOf("blocked-tracker.com"))

        val queryPacket = buildRawDnsQuery("blocked-tracker.com", DnsBlocker.TYPE_A, 0x55AA.toShort())

        val response = blocker.processDnsQuery(queryPacket, returnNxDomain = false)
        assertNotNull("Blocked domain query must return synthetic response", response)

        val respBuffer = ByteBuffer.wrap(response!!)
        val id = respBuffer.short
        val flags = respBuffer.short.toInt() and 0xFFFF
        val qdCount = respBuffer.short.toInt() and 0xFFFF
        val anCount = respBuffer.short.toInt() and 0xFFFF

        assertEquals(0x55AA.toShort(), id)
        assertEquals("Flags must indicate response (0x8180)", 0x8180, flags)
        assertEquals(1, qdCount)
        assertEquals(1, anCount)

        // Verify last 4 bytes are 0.0.0.0
        val answerIp = response.copyOfRange(response.size - 4, response.size)
        assertEquals(0, answerIp[0].toInt())
        assertEquals(0, answerIp[1].toInt())
        assertEquals(0, answerIp[2].toInt())
        assertEquals(0, answerIp[3].toInt())
    }

    @Test
    fun testNxDomainResponse() {
        blocker.addDomains(listOf("blocked-tracker.com"))

        val queryPacket = buildRawDnsQuery("blocked-tracker.com", DnsBlocker.TYPE_A, 0x77BB.toShort())

        val response = blocker.processDnsQuery(queryPacket, returnNxDomain = true)
        assertNotNull(response)

        val respBuffer = ByteBuffer.wrap(response!!)
        respBuffer.position(2) // Skip ID
        val flags = respBuffer.short.toInt() and 0xFFFF
        val qdCount = respBuffer.short.toInt() and 0xFFFF
        val anCount = respBuffer.short.toInt() and 0xFFFF

        // 0x8183 = NXDOMAIN
        assertEquals(0x8183, flags)
        assertEquals(1, qdCount)
        assertEquals(0, anCount)
    }

    @Test
    fun testAllowedDomainReturnsNull() {
        blocker.addDomains(listOf("adserver.com"))

        val queryPacket = buildRawDnsQuery("open-source-project.org", DnsBlocker.TYPE_A, 0x1234.toShort())
        val response = blocker.processDnsQuery(queryPacket)

        assertNull("Allowed domain must return null so it can be forwarded to upstream DNS", response)
    }

    @Test
    fun testTelemetryTracking() {
        blocker.addDomains(listOf("telemetry-domain.com"))

        blocker.isBlocked("telemetry-domain.com")
        blocker.isBlocked("allowed.org")
        blocker.isBlocked("another-allowed.org")

        assertEquals(3, blocker.queriesProcessed)
        assertEquals(1, blocker.queriesBlocked)
    }

    private fun buildRawDnsQuery(domain: String, qType: Int, queryId: Short): ByteArray {
        val buffer = ByteBuffer.allocate(512)
        // Header
        buffer.putShort(queryId)
        buffer.putShort(0x0100.toShort()) // Standard query, RD=1
        buffer.putShort(1.toShort())      // QDCOUNT=1
        buffer.putShort(0.toShort())      // ANCOUNT=0
        buffer.putShort(0.toShort())      // NSCOUNT=0
        buffer.putShort(0.toShort())      // ARCOUNT=0

        // Question: QNAME
        for (part in domain.split(".")) {
            val bytes = part.toByteArray(Charsets.US_ASCII)
            buffer.put(bytes.size.toByte())
            buffer.put(bytes)
        }
        buffer.put(0.toByte()) // Terminal null label

        // QTYPE & QCLASS
        buffer.putShort(qType.toShort())
        buffer.putShort(DnsBlocker.CLASS_IN.toShort())

        val result = ByteArray(buffer.position())
        buffer.flip()
        buffer.get(result)
        return result
    }
}
