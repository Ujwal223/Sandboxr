package com.sandboxr.virtual

import com.sandboxr.virtual.hardware.HardwareSpoofer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class HardwareSpooferTest {

    @Before
    fun setUp() {
        HardwareSpoofer.clearProfiles()
    }

    @Test
    fun testImeiLuhnValidationAndGeneration() {
        // Test known valid and invalid IMEIs
        val sampleImei = "86012345678901"
        val checkDigit = HardwareSpoofer.computeLuhnCheckDigit(sampleImei)
        val fullImei = "$sampleImei$checkDigit"
        assertTrue("Calculated Luhn IMEI should be valid", HardwareSpoofer.isValidImei(fullImei))

        // Generate 100 random IMEIs and ensure 100% pass Luhn check
        for (i in 1..100) {
            val random = java.util.Random(i.toLong())
            val imei = HardwareSpoofer.generateLuhnImei(random)
            assertEquals("IMEI length must be 15 digits", 15, imei.length)
            assertTrue("Generated IMEI '$imei' must pass Luhn check", HardwareSpoofer.isValidImei(imei))
        }
    }

    @Test
    fun testDeterministicProfilePerEnvironmentUuid() {
        val envUuid = UUID.randomUUID().toString()

        val profile1 = HardwareSpoofer.generateDeterministic(envUuid)
        val profile2 = HardwareSpoofer.generateDeterministic(envUuid)

        // Values must persist deterministically per environment UUID
        assertEquals("IMEI must be deterministic", profile1.imei, profile2.imei)
        assertEquals("Android ID must be deterministic", profile1.androidId, profile2.androidId)
        assertEquals("MAC address must be deterministic", profile1.macAddress, profile2.macAddress)
        assertEquals("Serial must be deterministic", profile1.serial, profile2.serial)
        assertEquals("Advertising ID must be deterministic", profile1.advertisingId, profile2.advertisingId)
        assertEquals("WiFi BSSID must be deterministic", profile1.wifiBssid, profile2.wifiBssid)

        // Verify valid formats
        assertTrue("IMEI must pass Luhn check", HardwareSpoofer.isValidImei(profile1.imei))
        assertEquals("Android ID must be 16 hex chars", 16, profile1.androidId.length)
        assertTrue("MAC must match XX:XX:XX:XX:XX:XX", profile1.macAddress.matches(Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")))
        assertEquals("Serial must be 16 characters", 16, profile1.serial.length)

        // Ensure different UUID generates different identity
        val differentUuid = UUID.randomUUID().toString()
        val differentProfile = HardwareSpoofer.generateDeterministic(differentUuid)
        assertNotEquals("Different UUID must have different IMEI", profile1.imei, differentProfile.imei)
        assertNotEquals("Different UUID must have different Android ID", profile1.androidId, differentProfile.androidId)
        assertNotEquals("Different UUID must have different Serial", profile1.serial, differentProfile.serial)
    }

    @Test
    fun testGetOrCreateProfilePersistence() {
        val envId = "test-env-42"
        val profile1 = HardwareSpoofer.getOrCreateProfile(null, envId)
        val profile2 = HardwareSpoofer.getOrCreateProfile(null, envId)

        assertEquals("Subsequent getOrCreateProfile calls must return same profile", profile1, profile2)
        assertTrue("Profile IMEI must pass Luhn check", HardwareSpoofer.isValidImei(profile1.imei))
    }
}
