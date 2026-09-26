package com.sandboxr.network.model

/**
 * Network routing modes per environment.
 * Maps 1:1 with Go firestack NetworkMode and PRD Section 7.2.
 */
enum class NetworkMode(val id: Int) {
    DIRECT(0),
    SOCKS5(1),
    WIREGUARD(2),
    BLOCKED(3);

    companion object {
        fun fromId(id: Int): NetworkMode = entries.firstOrNull { it.id == id } ?: DIRECT
    }
}

/**
 * Encapsulates routing rules and proxy parameters for an environment.
 */
data class NetworkRoute(
    val envId: String,
    val mode: NetworkMode = NetworkMode.DIRECT,
    val proxyHost: String? = null,
    val proxyPort: Int? = null,
    val wgConfig: String? = null,
    val dnsUpstream: String? = null,
    val adBlockEnabled: Boolean = true
)

/**
 * Real-time telemetry snapshot from the network engine.
 */
data class FirestackStats(
    val totalPacketsReceived: Long = 0,
    val totalPacketsSent: Long = 0,
    val totalBytesReceived: Long = 0,
    val totalBytesSent: Long = 0,
    val dnsQueriesTotal: Long = 0,
    val dnsQueriesBlocked: Long = 0,
    val packetsDropped: Long = 0,
    val activeRoutesCount: Int = 0
)
