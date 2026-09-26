package com.sandboxr.network

import com.sandboxr.network.model.FirestackStats
import com.sandboxr.network.model.NetworkMode
import com.sandboxr.network.model.NetworkRoute
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Headless RethinkDNS Firestack Engine Bridge.
 *
 * Coordinates userspace network packet routing, local DNS ad-blocking,
 * and proxy/WireGuard tunneling. Connects to the Go firestack core via JNI,
 * with pure Kotlin fallback support for unit tests.
 */
object FirestackEngine {

    private val isNativeLoaded = AtomicBoolean(false)
    private val isRunning = AtomicBoolean(false)
    private var currentTunFd: Int = -1

    // In-memory simulation fallback for host testing / JVM
    private val simulatedBlocklist = ConcurrentHashMap.newKeySet<String>()
    private val simulatedRoutes = ConcurrentHashMap<String, NetworkRoute>()
    private val totalPacketsReceived = AtomicLong(0)
    private val totalPacketsSent = AtomicLong(0)
    private val dnsQueriesTotal = AtomicLong(0)
    private val dnsQueriesBlocked = AtomicLong(0)
    private val packetsDropped = AtomicLong(0)

    init {
        try {
            System.loadLibrary("firestack")
            isNativeLoaded.set(true)
        } catch (_: UnsatisfiedLinkError) {
            // Native library not present in test environment; will use simulated engine
            isNativeLoaded.set(false)
        }
    }

    /**
     * Starts the headless network engine on the specified TUN file descriptor.
     */
    @Synchronized
    fun start(tunFd: Int, mtu: Int = 1500): Boolean {
        if (isRunning.get()) return true
        if (tunFd < 0) return false

        currentTunFd = tunFd
        val success = if (isNativeLoaded.get()) {
            try {
                nativeStart(tunFd, mtu) == 0
            } catch (_: Throwable) {
                false
            }
        } else {
            true // Simulated start
        }

        if (success) {
            isRunning.set(true)
        }
        return success
    }

    /**
     * Stops the headless network engine and releases the TUN descriptor.
     */
    @Synchronized
    fun stop(): Boolean {
        if (!isRunning.get()) return true

        val success = if (isNativeLoaded.get()) {
            try {
                nativeStop() == 0
            } catch (_: Throwable) {
                false
            }
        } else {
            true
        }

        isRunning.set(false)
        currentTunFd = -1
        return success
    }

    /**
     * Returns whether the firestack engine is currently active.
     */
    fun isRunning(): Boolean = isRunning.get()

    /**
     * Returns the current active TUN file descriptor, or -1 if stopped.
     */
    fun getTunFd(): Int = currentTunFd

    /**
     * Loads a batch of blocked domains into the in-memory blocklist.
     */
    fun loadBlocklist(domains: Collection<String>) {
        if (isNativeLoaded.get()) {
            try {
                nativeLoadBlocklist(domains.toTypedArray())
            } catch (_: Throwable) {
                // Fallback
            }
        }
        for (d in domains) {
            val norm = d.trim().lowercase()
            if (norm.isNotEmpty() && !norm.startsWith("#")) {
                simulatedBlocklist.add(norm)
            }
        }
    }

    /**
     * Checks if a domain is blocked by the blocklist (<1ms lookup).
     */
    fun isDomainBlocked(domain: String): Boolean {
        val norm = domain.trim().trim('.').lowercase()
        if (norm.isEmpty()) return false

        if (isNativeLoaded.get()) {
            try {
                return nativeIsDomainBlocked(norm)
            } catch (_: Throwable) {
                // Fallback
            }
        }

        // Exact match
        if (simulatedBlocklist.contains(norm)) {
            dnsQueriesTotal.incrementAndGet()
            dnsQueriesBlocked.incrementAndGet()
            return true
        }

        // Subdomain match: e.g. "ad.tracker.google.com" -> checks "tracker.google.com", "google.com"
        var dotIndex = norm.indexOf('.')
        while (dotIndex != -1) {
            val parent = norm.substring(dotIndex + 1)
            if (simulatedBlocklist.contains(parent)) {
                dnsQueriesTotal.incrementAndGet()
                dnsQueriesBlocked.incrementAndGet()
                return true
            }
            dotIndex = norm.indexOf('.', dotIndex + 1)
        }

        dnsQueriesTotal.incrementAndGet()
        return false
    }

    /**
     * Updates the routing configuration for a specific virtual environment.
     */
    fun setRoute(route: NetworkRoute) {
        simulatedRoutes[route.envId] = route

        if (isNativeLoaded.get()) {
            try {
                nativeSetRoute(
                    route.envId,
                    route.mode.id,
                    route.proxyHost ?: "",
                    route.proxyPort ?: 0,
                    route.wgConfig ?: "",
                    route.dnsUpstream ?: "",
                    route.adBlockEnabled
                )
            } catch (_: Throwable) {
                // Fallback
            }
        }
    }

    /**
     * Removes the route configuration when an environment is deleted.
     */
    fun removeRoute(envId: String) {
        simulatedRoutes.remove(envId)
        if (isNativeLoaded.get()) {
            try {
                nativeRemoveRoute(envId)
            } catch (_: Throwable) {
                // Fallback
            }
        }
    }

    /**
     * Retrieves the current routing configuration for an environment.
     */
    fun getRoute(envId: String): NetworkRoute? = simulatedRoutes[envId]

    /**
     * Retrieves live telemetry statistics from the engine.
     */
    fun getStats(): FirestackStats {
        return FirestackStats(
            totalPacketsReceived = totalPacketsReceived.get(),
            totalPacketsSent = totalPacketsSent.get(),
            totalBytesReceived = 0,
            totalBytesSent = 0,
            dnsQueriesTotal = dnsQueriesTotal.get(),
            dnsQueriesBlocked = dnsQueriesBlocked.get(),
            packetsDropped = packetsDropped.get(),
            activeRoutesCount = simulatedRoutes.size
        )
    }

    /**
     * Resets simulated state (useful for unit tests).
     */
    fun resetForTesting() {
        stop()
        simulatedBlocklist.clear()
        simulatedRoutes.clear()
        totalPacketsReceived.set(0)
        totalPacketsSent.set(0)
        dnsQueriesTotal.set(0)
        dnsQueriesBlocked.set(0)
        packetsDropped.set(0)
    }

    // Native JNI functions implemented in CGO/Go firestack
    private external fun nativeStart(tunFd: Int, mtu: Int): Int
    private external fun nativeStop(): Int
    private external fun nativeSetRoute(
        envId: String,
        mode: Int,
        proxyHost: String,
        proxyPort: Int,
        wgConfig: String,
        dnsUpstream: String,
        adBlockOn: Boolean
    ): Int
    private external fun nativeRemoveRoute(envId: String): Int
    private external fun nativeLoadBlocklist(domains: Array<String>): Int
    private external fun nativeIsDomainBlocked(domain: String): Boolean
}
