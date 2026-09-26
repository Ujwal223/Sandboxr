package com.sandboxr.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sandboxr.virtual.model.SpoofProfile
import java.util.UUID

/**
 * Supported per-environment network configurations.
 * Aligns with PRD Section 7.2.
 */
enum class NetworkConfig {
    DIRECT,
    SOCKS5,
    WIREGUARD,
    BLOCKED
}

/**
 * Supported clipboard isolation modes per environment.
 * Aligns with PRD Section 7.2 & Phase 9.
 */
enum class ClipboardMode {
    SHARED,
    ISOLATED,
    BLOCKED
}

/**
 * Room Entity representing a SANDBOXR Container Environment.
 * Matches all specifications defined in PRD Section 7.2 (Virtual Environment Data Model).
 */
@Entity(tableName = "environments")
data class EnvironmentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "color_tag")
    val colorTag: Long,

    @ColumnInfo(name = "icon_emoji")
    val iconEmoji: String? = null,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "network_config")
    val networkConfig: NetworkConfig = NetworkConfig.DIRECT,

    @ColumnInfo(name = "proxy_host")
    val proxyHost: String? = null,

    @ColumnInfo(name = "proxy_port")
    val proxyPort: Int? = null,

    @ColumnInfo(name = "wg_config")
    val wgConfig: String? = null,

    @ColumnInfo(name = "dns_upstream")
    val dnsUpstream: String? = null,

    @ColumnInfo(name = "ad_block_enabled")
    val adBlockEnabled: Boolean = true,

    @ColumnInfo(name = "clipboard_mode")
    val clipboardMode: ClipboardMode = ClipboardMode.ISOLATED,

    @ColumnInfo(name = "gms_enabled")
    val gmsEnabled: Boolean = false,

    @Embedded(prefix = "hw_")
    val hardwareIds: SpoofProfile = SpoofProfile.generate(),

    @ColumnInfo(name = "freeze_when_inactive")
    val freezeWhenInactive: Boolean = true,

    @ColumnInfo(name = "notif_isolation")
    val notifIsolation: Boolean = true,

    @ColumnInfo(name = "is_system")
    val isSystem: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SYSTEM_ENV_ID = "system_default_environment"

        /**
         * Creates the canonical unvirtualized System environment.
         * The System environment is pinned first, uses zero hooks, and directs to host OS.
         */
        fun createSystemEnvironment(): EnvironmentEntity {
            return EnvironmentEntity(
                id = SYSTEM_ENV_ID,
                displayName = "System",
                colorTag = 0xFF4A90E2L,
                iconEmoji = null,
                sortOrder = 0,
                networkConfig = NetworkConfig.DIRECT,
                adBlockEnabled = false,
                clipboardMode = ClipboardMode.SHARED,
                gmsEnabled = true,
                freezeWhenInactive = false,
                notifIsolation = false,
                isSystem = true,
                createdAt = 0L
            )
        }
    }
}
