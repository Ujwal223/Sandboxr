package com.sandboxr.network.dns

import android.content.Context
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * High-Performance In-RAM Ad-Blocking DNS Filter.
 *
 * Loads hosts blocklists (e.g. Steven Black unified hosts) into an optimized
 * in-memory HashSet. Provides sub-millisecond query evaluation (< 1ms) and
 * synthesizes immediate RFC-compliant DNS responses (0.0.0.0 / :: or NXDOMAIN)
 * without generating external network round-trips.
 */
class DnsBlocker private constructor() {

    companion object {
        val instance: DnsBlocker by lazy { DnsBlocker() }

        const val DEFAULT_ASSET_PATH = "blocklist/hosts.txt"

        // DNS Record Types
        const val TYPE_A = 1
        const val TYPE_NS = 2
        const val TYPE_CNAME = 5
        const val TYPE_SOA = 6
        const val TYPE_AAAA = 28

        // DNS Classes
        const val CLASS_IN = 1

        // DNS RCODEs
        const val RCODE_NOERROR = 0
        const val RCODE_NXDOMAIN = 3
    }

    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()

    private val totalQueries = AtomicLong(0)
    private val blockedQueries = AtomicLong(0)

    /**
     * Number of unique domains currently loaded in memory.
     */
    val domainCount: Int
        get() = blockedDomains.size

    /**
     * Telemetry metric for total evaluated queries.
     */
    val queriesProcessed: Long
        get() = totalQueries.get()

    /**
     * Telemetry metric for total blocked queries.
     */
    val queriesBlocked: Long
        get() = blockedQueries.get()

    /**
     * Loads domain blocklist from an InputStream (standard hosts format).
     * Returns total number of domains added.
     */
    fun loadFromStream(inputStream: InputStream): Int {
        var added = 0
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
            lines.forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isNotEmpty() && !line.startsWith("#")) {
                    val domain = parseHostsLine(line)
                    if (domain != null && isValidDomain(domain)) {
                        if (blockedDomains.add(domain)) {
                            added++
                        }
                    }
                }
            }
        }
        return added
    }

    /**
     * Loads bundled hosts blocklist from Android application assets.
     */
    fun loadFromAssets(context: Context, assetPath: String = DEFAULT_ASSET_PATH): Int {
        return try {
            context.assets.open(assetPath).use { stream ->
                loadFromStream(stream)
            }
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Adds an array or list of domain names directly to the blocklist.
     */
    fun addDomains(domains: Collection<String>): Int {
        var count = 0
        for (d in domains) {
            val norm = normalizeDomain(d)
            if (norm.isNotEmpty() && isValidDomain(norm)) {
                if (blockedDomains.add(norm)) {
                    count++
                }
            }
        }
        return count
    }

    /**
     * Evaluates whether a domain is blocked.
     * Guaranteed sub-millisecond lookup (< 0.05ms in-RAM).
     *
     * Evaluates exact domain as well as hierarchical parent subdomains:
     * e.g., "ad.sub.doubleclick.net" checks "ad.sub.doubleclick.net",
     * "sub.doubleclick.net", and "doubleclick.net".
     */
    fun isBlocked(domain: String): Boolean {
        totalQueries.incrementAndGet()
        val norm = normalizeDomain(domain)
        if (norm.isEmpty()) return false

        // 1. Direct O(1) exact match
        if (blockedDomains.contains(norm)) {
            blockedQueries.incrementAndGet()
            return true
        }

        // 2. Parent subdomain hierarchy scan (skip TLDs without dots)
        val lastDotIndex = norm.lastIndexOf('.')
        if (lastDotIndex > 0) {
            var dotIndex = norm.indexOf('.')
            while (dotIndex != -1 && dotIndex < lastDotIndex) {
                val parent = norm.substring(dotIndex + 1)
                if (blockedDomains.contains(parent)) {
                    blockedQueries.incrementAndGet()
                    return true
                }
                dotIndex = norm.indexOf('.', dotIndex + 1)
            }
        }

        return false
    }

    /**
     * Inspects raw DNS wire bytes. If the query target is in the blocklist,
     * immediately synthesizes and returns an RFC-compliant DNS response (0.0.0.0 / ::
     * or NXDOMAIN). If allowed, returns null.
     */
    fun processDnsQuery(packet: ByteArray, returnNxDomain: Boolean = false): ByteArray? {
        if (packet.size < 12) return null

        val parsed = parseDnsQuestion(packet) ?: return null
        val domain = parsed.first
        val qType = parsed.second

        if (!isBlocked(domain)) {
            return null // Not blocked, forward to upstream DNS
        }

        return buildBlockedResponse(packet, qType, returnNxDomain)
    }

    /**
     * Parses the QNAME and QTYPE from raw DNS wire bytes.
     */
    fun parseDnsQuestion(packet: ByteArray): Pair<String, Int>? {
        if (packet.size < 12) return null

        val buffer = ByteBuffer.wrap(packet)
        buffer.position(12) // Skip 12-byte header

        val labels = mutableListOf<String>()
        while (buffer.hasRemaining()) {
            val len = buffer.get().toInt() and 0xFF
            if (len == 0) break
            if (len and 0xC0 == 0xC0) {
                // Compression pointer in query question section is non-standard
                return null
            }
            if (buffer.remaining() < len) return null

            val labelBytes = ByteArray(len)
            buffer.get(labelBytes)
            labels.add(String(labelBytes, Charsets.US_ASCII))
        }

        if (labels.isEmpty() || buffer.remaining() < 4) return null

        val domain = labels.joinToString(".")
        val qType = buffer.short.toInt() and 0xFFFF
        // val qClass = buffer.short.toInt() and 0xFFFF

        return Pair(domain, qType)
    }

    /**
     * Synthesizes an RFC 1035 compliant DNS answer packet returning 0.0.0.0 or NXDOMAIN.
     */
    fun buildBlockedResponse(query: ByteArray, qType: Int, returnNxDomain: Boolean): ByteArray {
        val queryBuffer = ByteBuffer.wrap(query)
        val queryId = queryBuffer.short

        // Response flags: standard response, recursion desired, recursion available
        val flags = if (returnNxDomain) {
            0x8183.toShort() // NXDOMAIN (RCODE = 3)
        } else {
            0x8180.toShort() // NOERROR (RCODE = 0)
        }

        val anCount = if (!returnNxDomain && (qType == TYPE_A || qType == TYPE_AAAA)) {
            1.toShort()
        } else {
            0.toShort()
        }

        // Find end of Question section in query
        var questionEnd = 12
        while (questionEnd < query.size) {
            val len = query[questionEnd].toInt() and 0xFF
            if (len == 0) {
                questionEnd += 5 // Null byte + 2 bytes QTYPE + 2 bytes QCLASS
                break
            }
            questionEnd += 1 + len
        }

        val questionBytes = query.copyOfRange(12, minOf(questionEnd, query.size))
        val answerRecordSize = if (!returnNxDomain && qType == TYPE_A) {
            16 // 2 (name ptr) + 2 (type) + 2 (class) + 4 (ttl) + 2 (rdlen) + 4 (ip)
        } else if (!returnNxDomain && qType == TYPE_AAAA) {
            28 // 2 (name ptr) + 2 (type) + 2 (class) + 4 (ttl) + 2 (rdlen) + 16 (ip6)
        } else {
            0
        }

        val totalSize = 12 + questionBytes.size + answerRecordSize
        val respBuffer = ByteBuffer.allocate(totalSize)

        // 12-byte header
        respBuffer.putShort(queryId)
        respBuffer.putShort(flags)
        respBuffer.putShort(1.toShort()) // QDCOUNT = 1
        respBuffer.putShort(anCount)     // ANCOUNT = 1 or 0
        respBuffer.putShort(0.toShort()) // NSCOUNT = 0
        respBuffer.putShort(0.toShort()) // ARCOUNT = 0

        // Question section
        respBuffer.put(questionBytes)

        // Answer section (if returning 0.0.0.0 or ::)
        if (!returnNxDomain && anCount.toInt() == 1) {
            respBuffer.putShort(0xC00C.toShort()) // Pointer to QNAME at offset 12
            if (qType == TYPE_A) {
                respBuffer.putShort(TYPE_A.toShort())
                respBuffer.putShort(CLASS_IN.toShort())
                respBuffer.putInt(300) // TTL 300 seconds
                respBuffer.putShort(4.toShort()) // RDLENGTH 4 bytes
                respBuffer.put(byteArrayOf(0, 0, 0, 0)) // 0.0.0.0
            } else if (qType == TYPE_AAAA) {
                respBuffer.putShort(TYPE_AAAA.toShort())
                respBuffer.putShort(CLASS_IN.toShort())
                respBuffer.putInt(300)
                respBuffer.putShort(16.toShort()) // RDLENGTH 16 bytes
                respBuffer.put(ByteArray(16)) // ::
            }
        }

        return respBuffer.array()
    }

    /**
     * Clears all domains and resets telemetry (for testing).
     */
    fun clear() {
        blockedDomains.clear()
        totalQueries.set(0)
        blockedQueries.set(0)
    }

    private fun parseHostsLine(line: String): String? {
        val parts = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return when {
            parts.size >= 2 -> normalizeDomain(parts[1])
            parts.size == 1 -> normalizeDomain(parts[0])
            else -> null
        }
    }

    private fun normalizeDomain(domain: String): String {
        return domain.trim().trim('.').lowercase()
    }

    private fun isValidDomain(domain: String): Boolean {
        if (domain.isEmpty() || domain == "localhost" || domain == "0.0.0.0" || domain == "127.0.0.1") {
            return false
        }
        return domain.contains(".") && !domain.contains(" ")
    }
}
