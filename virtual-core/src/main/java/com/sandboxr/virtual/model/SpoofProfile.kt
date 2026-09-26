package com.sandboxr.virtual.model

import java.security.SecureRandom
import java.util.UUID

/**
 * Hardware Identity Table for a virtual environment.
 * All values are generated with strict format compliance (e.g. Luhn algorithm for IMEI).
 */
data class SpoofProfile(
    val imei: String,
    val androidId: String,
    val macAddress: String,
    val serial: String,
    val advertisingId: String,
    val wifiBssid: String
) {
    companion object {
        private val random = SecureRandom()

        /**
         * Generates a realistic, cryptographically random, and Luhn-valid SpoofProfile.
         */
        fun generate(): SpoofProfile {
            return SpoofProfile(
                imei = generateLuhnImei(),
                androidId = generateHex64(),
                macAddress = generateMacAddress(),
                serial = generateSerial(),
                advertisingId = UUID.randomUUID().toString(),
                wifiBssid = generateMacAddress()
            )
        }

        /**
         * Generates a 15-digit Luhn-compliant IMEI.
         * First 14 digits are randomly chosen with standard TAC prefix (e.g. 86 / 35),
         * and the 15th digit is the calculated Luhn check digit.
         */
        fun generateLuhnImei(): String {
            val sb = StringBuilder("86") // Common TAC prefix
            for (i in 0 until 12) {
                sb.append(random.nextInt(10))
            }
            val checkDigit = computeLuhnCheckDigit(sb.toString())
            sb.append(checkDigit)
            return sb.toString()
        }

        /**
         * Computes the Luhn check digit for an input string of digits.
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
         * Validates a 15-digit IMEI against the Luhn algorithm.
         */
        fun isValidImei(imei: String): Boolean {
            if (imei.length != 15 || !imei.all { it.isDigit() }) return false
            val payload = imei.substring(0, 14)
            val expectedCheck = computeLuhnCheckDigit(payload)
            return (imei[14] - '0') == expectedCheck
        }

        private fun generateHex64(): String {
            val bytes = ByteArray(8)
            random.nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        private fun generateMacAddress(): String {
            val bytes = ByteArray(6)
            random.nextBytes(bytes)
            // Ensure unicast and locally administered address (bit 0 = 0, bit 1 = 1)
            bytes[0] = (bytes[0].toInt() and 0xFE or 0x02).toByte()
            return bytes.joinToString(":") { "%02X".format(it) }
        }

        private fun generateSerial(): String {
            val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            return (1..16)
                .map { chars[random.nextInt(chars.length)] }
                .joinToString("")
        }
    }
}
