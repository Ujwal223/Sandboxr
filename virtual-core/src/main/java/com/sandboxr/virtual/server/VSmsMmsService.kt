package com.sandboxr.virtual.server

import android.content.Context
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.regex.Pattern

/**
 * Record representing an incoming SMS message inside a virtual environment.
 */
data class VirtualSmsRecord(
    val id: String,
    val envId: String,
    val sender: String,
    val body: String,
    val otpCode: String?,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Verifies if message falls within the Android 17 (API 37) 3-hour access delay window.
     */
    fun isWithinDeliveryWindow(currentTime: Long = System.currentTimeMillis()): Boolean {
        return (currentTime - timestamp) <= VSmsMmsService.OTP_DELIVERY_WINDOW_MS
    }
}

/**
 * Virtual SMS/MMS Service managing SMS message caching and OTP message delivery
 * for Android 17 (API 37) compliance.
 *
 * In Android 17 (API 37), apps intercepting READ_SMS are subject to an OTP SMS access delay
 * where one-time passwords may have a delayed delivery window of up to 3 hours.
 * VSmsMmsService provides a retained delivery window buffer ensuring virtual guest apps
 * receive SMS OTP verification codes reliably without timing out or being dropped.
 */
class VSmsMmsService private constructor(private val context: Context) {

    companion object {
        private const val TAG = "VSmsMmsService"

        // Android 17 (API 37) OTP 3-hour access delay window
        const val OTP_DELIVERY_WINDOW_MS = 3 * 60 * 60 * 1000L // 3 Hours (10,800,000 ms)

        // Standard patterns for 4 to 8 digit OTP / security codes
        private val OTP_PATTERN = Pattern.compile("\\b(\\d{4,8})\\b|\\b([A-Z0-9]{6})\\b")

        @Volatile
        private var instance: VSmsMmsService? = null

        fun get(context: Context): VSmsMmsService {
            return instance ?: synchronized(this) {
                instance ?: VSmsMmsService(context.applicationContext ?: context).also { instance = it }
            }
        }

        fun createForTesting(context: Context): VSmsMmsService {
            return VSmsMmsService(context)
        }
    }

    // Key: envId -> List<VirtualSmsRecord>
    private val environmentSmsMap = ConcurrentHashMap<String, CopyOnWriteArrayList<VirtualSmsRecord>>()

    /**
     * Extracts an OTP or verification code from SMS message body.
     */
    fun extractOtpCode(body: String): String? {
        val matcher = OTP_PATTERN.matcher(body)
        if (matcher.find()) {
            return matcher.group(1) ?: matcher.group(2)
        }
        return null
    }

    /**
     * Records an incoming SMS for a virtual environment and caches OTP messages
     * within the 3-hour delivery window.
     */
    fun recordIncomingSms(
        envId: String,
        sender: String,
        body: String,
        timestamp: Long = System.currentTimeMillis()
    ): VirtualSmsRecord {
        val otpCode = extractOtpCode(body)
        val record = VirtualSmsRecord(
            id = java.util.UUID.randomUUID().toString(),
            envId = envId,
            sender = sender,
            body = body,
            otpCode = otpCode,
            timestamp = timestamp
        )

        val list = environmentSmsMap.computeIfAbsent(envId) { CopyOnWriteArrayList() }
        list.add(record)
        Log.i(TAG, "Cached SMS from $sender in env $envId (OTP: ${otpCode ?: "None"}, timestamp=$timestamp)")
        return record
    }

    /**
     * Returns all SMS messages for an environment that are valid within the 3-hour delivery window.
     */
    fun getMessages(envId: String, includeExpired: Boolean = false): List<VirtualSmsRecord> {
        val list = environmentSmsMap[envId] ?: return emptyList()
        val now = System.currentTimeMillis()

        return if (includeExpired) {
            list.toList()
        } else {
            list.filter { it.isWithinDeliveryWindow(now) }
        }
    }

    /**
     * Queries valid OTP codes for an environment within the 3-hour delivery window.
     */
    fun getOtpMessages(envId: String): List<VirtualSmsRecord> {
        val now = System.currentTimeMillis()
        return (environmentSmsMap[envId] ?: emptyList()).filter {
            it.otpCode != null && it.isWithinDeliveryWindow(now)
        }
    }

    /**
     * Purges expired messages older than the 3-hour OTP window.
     */
    fun purgeExpiredMessages(currentTime: Long = System.currentTimeMillis()): Int {
        var purgedCount = 0
        environmentSmsMap.forEach { (_, list) ->
            val expired = list.filter { !it.isWithinDeliveryWindow(currentTime) }
            if (expired.isNotEmpty()) {
                list.removeAll(expired)
                purgedCount += expired.size
            }
        }
        if (purgedCount > 0) {
            Log.i(TAG, "Purged $purgedCount expired SMS records outside 3-hour OTP delivery window.")
        }
        return purgedCount
    }

    /**
     * Clears all cached SMS records (used in tests).
     */
    fun clear() {
        environmentSmsMap.clear()
    }
}
