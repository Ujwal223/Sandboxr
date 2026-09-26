package com.sandboxr.virtual.hardware

import android.content.Context
import android.util.Log
import com.sandboxr.virtual.model.SpoofProfile
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

/**
 * Hardware Identity Generator, Validator, and Spoofer Engine.
 * Generates mathematically valid, Luhn-compliant IMEIs, 64-bit hex Android IDs,
 * IEEE 802 MAC addresses, and deterministic hardware profiles per environment UUID.
 */
object HardwareSpoofer {
    private const val TAG = "HardwareSpoofer"
    private val profilesByEnv = ConcurrentHashMap<String, SpoofProfile>()

    /**
     * Retrieves or deterministically generates and persists a SpoofProfile for the specified environment UUID.
     */
    fun getOrCreateProfile(context: Context? = null, envId: String): SpoofProfile {
        return profilesByEnv.computeIfAbsent(envId) { id ->
            val loaded = context?.let { loadProfileFromFile(it, id) }
            loaded ?: generateDeterministic(id).also { generated ->
                context?.let { saveProfileToFile(it, id, generated) }
            }
        }
    }

    /**
     * Generates a deterministic, Luhn-valid SpoofProfile derived cryptographically from the environment UUID.
     * Guaranteed to produce the exact same profile for the same UUID.
     */
    fun generateDeterministic(envUuid: String): SpoofProfile {
        val digest = MessageDigest.getInstance("SHA-256").digest(envUuid.toByteArray(Charsets.UTF_8))
        val seed = ByteBuffer.wrap(digest).long
        val prng = Random(seed)

        val imei = generateLuhnImei(prng)
        val androidId = generateHex64(prng)
        val macAddress = generateMacAddress(prng)
        val serial = generateSerial(prng)
        val advertisingId = java.util.UUID.nameUUIDFromBytes(digest).toString()
        val wifiBssid = generateMacAddress(prng)

        return SpoofProfile(
            imei = imei,
            androidId = androidId,
            macAddress = macAddress,
            serial = serial,
            advertisingId = advertisingId,
            wifiBssid = wifiBssid
        )
    }

    /**
     * Validates a 15-digit IMEI against the Luhn (Mod 10) algorithm.
     */
    fun isValidImei(imei: String): Boolean {
        if (imei.length != 15 || !imei.all { it.isDigit() }) return false
        val payload = imei.substring(0, 14)
        val expectedCheck = computeLuhnCheckDigit(payload)
        return (imei[14] - '0') == expectedCheck
    }

    /**
     * Computes the Luhn Mod 10 check digit for an input string of digits.
     */
    fun computeLuhnCheckDigit(numberString: String): Int {
        var sum = 0
        val parity = (numberString.length + 1) % 2
        for (i in numberString.indices) {
            var digit = numberString[i] - '0'
            if (i % 2 == parity) {
                digit *= 2
                if (digit > 9) {
                    digit -= 9
                }
            }
            sum += digit
        }
        val rem = sum % 10
        return if (rem == 0) 0 else 10 - rem
    }

    /**
     * Generates a 15-digit Luhn-compliant IMEI with realistic TAC prefix using the provided PRNG.
     */
    fun generateLuhnImei(prng: Random): String {
        val tacPrefixes = listOf("86", "35", "01", "49", "52")
        val prefix = tacPrefixes[prng.nextInt(tacPrefixes.size)]
        val sb = StringBuilder(prefix)
        while (sb.length < 14) {
            sb.append(prng.nextInt(10))
        }
        val checkDigit = computeLuhnCheckDigit(sb.toString())
        sb.append(checkDigit)
        return sb.toString()
    }

    /**
     * Generates a 16-character (64-bit) lowercase hexadecimal Android ID.
     */
    fun generateHex64(prng: Random): String {
        val bytes = ByteArray(8)
        prng.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates a valid IEEE 802 MAC address with locally administered and unicast bits set.
     */
    fun generateMacAddress(prng: Random): String {
        val bytes = ByteArray(6)
        prng.nextBytes(bytes)
        bytes[0] = (bytes[0].toInt() and 0xFE or 0x02).toByte()
        return bytes.joinToString(":") { "%02X".format(it) }
    }

    /**
     * Generates a 16-character alphanumeric device serial number.
     */
    fun generateSerial(prng: Random): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return (1..16)
            .map { chars[prng.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * Applies the given SpoofProfile to the active environment via NativeHookBridge and system property tables.
     */
    fun applyProfile(profile: SpoofProfile) {
        NativeHookBridge.setProperty("ro.serialno", profile.serial)
        NativeHookBridge.setProperty("ro.boot.serialno", profile.serial)
        NativeHookBridge.setProperty("ro.build.id", "AP2A.240805.005")
        NativeHookBridge.setMacAddress(profile.macAddress)
        Log.i(TAG, "Applied hardware spoof profile: IMEI=${profile.imei}, Serial=${profile.serial}, MAC=${profile.macAddress}")
    }

    fun clearProfiles() {
        profilesByEnv.clear()
    }

    /**
     * Deletes any persisted hardware profile on disk and in memory for the specified environment.
     */
    fun deleteProfile(context: Context, envId: String) {
        profilesByEnv.remove(envId)
        val primaryFile = File(context.filesDir.parentFile ?: context.filesDir, "envs/$envId/hardware_profile.json")
        if (primaryFile.exists()) {
            primaryFile.delete()
        }
        val legacyFile = File(context.filesDir, "envs/$envId/hardware_profile.json")
        if (legacyFile.exists()) {
            legacyFile.delete()
        }
    }

    private fun loadProfileFromFile(context: Context, envId: String): SpoofProfile? {
        val primaryFile = File(context.filesDir.parentFile ?: context.filesDir, "envs/$envId/hardware_profile.json")
        val legacyFile = File(context.filesDir, "envs/$envId/hardware_profile.json")
        val targetFile = when {
            primaryFile.exists() -> primaryFile
            legacyFile.exists() -> legacyFile
            else -> return null
        }

        return try {
            val json = JSONObject(targetFile.readText())
            val profile = SpoofProfile(
                imei = json.getString("imei"),
                androidId = json.getString("androidId"),
                macAddress = json.getString("macAddress"),
                serial = json.getString("serial"),
                advertisingId = json.getString("advertisingId"),
                wifiBssid = json.getString("wifiBssid")
            )
            // Migrate legacy file to primary location if needed
            if (targetFile == legacyFile && primaryFile != legacyFile) {
                saveProfileToFile(context, envId, profile)
                legacyFile.delete()
            }
            profile
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to load hardware profile for $envId: ${t.message}")
            null
        }
    }

    private fun saveProfileToFile(context: Context, envId: String, profile: SpoofProfile) {
        val baseDir = context.filesDir.parentFile ?: context.filesDir
        val dir = File(baseDir, "envs/$envId")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "hardware_profile.json")
        try {
            val json = JSONObject().apply {
                put("imei", profile.imei)
                put("androidId", profile.androidId)
                put("macAddress", profile.macAddress)
                put("serial", profile.serial)
                put("advertisingId", profile.advertisingId)
                put("wifiBssid", profile.wifiBssid)
            }
            file.writeText(json.toString(2))
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to save hardware profile for $envId", t)
        }
    }
}
