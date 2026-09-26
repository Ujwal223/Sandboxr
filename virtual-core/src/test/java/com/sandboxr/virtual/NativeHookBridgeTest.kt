package com.sandboxr.virtual

import com.sandboxr.virtual.hardware.NativeHookBridge
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class NativeHookBridgeTest {

    @Before
    fun setUp() {
        NativeHookBridge.clear()
        NativeHookBridge.init()
    }

    @After
    fun tearDown() {
        NativeHookBridge.clear()
    }

    @Test
    fun testSystemPropertySerialNoSpoofing() {
        val spoofedSerial = "SNDBXR-9988776655"
        NativeHookBridge.setProperty("ro.serialno", spoofedSerial)

        // Verify Java framework System.getProperty returns spoofed value
        val javaProp = System.getProperty("ro.serialno")
        assertEquals("System.getProperty('ro.serialno') must return spoofed value", spoofedSerial, javaProp)

        // Verify NativeHookBridge getProperty returns spoofed value
        val bridgeProp = NativeHookBridge.getProperty("ro.serialno")
        assertEquals("NativeHookBridge.getProperty('ro.serialno') must return spoofed value", spoofedSerial, bridgeProp)

        // Verify getNativeProp returns spoofed value
        val nativeProp = NativeHookBridge.getNativeProp("ro.serialno")
        assertNotNull("getNativeProp must not return null", nativeProp)
        assertEquals("Native getprop must return spoofed value", spoofedSerial, nativeProp)
    }

    @Test
    fun testMultipleHardwarePropertiesSpoofing() {
        val props = mapOf(
            "ro.serialno" to "SN-ENV-1001",
            "ro.boot.serialno" to "BOOT-SN-1001",
            "ro.build.id" to "AP2A.240805.005",
            "ro.product.model" to "Pixel 9 Pro",
            "ro.product.brand" to "google",
            "ro.product.device" to "caiman"
        )

        for ((key, value) in props) {
            NativeHookBridge.setProperty(key, value)
            assertEquals("System.getProperty($key) must match", value, System.getProperty(key))
            assertEquals("NativeHookBridge.getProperty($key) must match", value, NativeHookBridge.getProperty(key))
        }
    }

    @Test
    fun testMacAddressSpoofing() {
        val spoofedMac = "02:1A:2B:3C:4D:5E"
        NativeHookBridge.setMacAddress(spoofedMac)
        assertEquals("Spoofed MAC address must match configured value", spoofedMac, NativeHookBridge.getMacAddress())
    }

    @Test
    fun testSysfsMacAddressInterception() {
        val virtualMac = "02:FA:CE:DE:AD:01"
        NativeHookBridge.setMacAddress(virtualMac)

        // Native app attempting to read MAC address from /sys/class/net/wlan0/address gets virtual MAC
        val sysfsMac = NativeHookBridge.readMacFromSysfs("/sys/class/net/wlan0/address")
        assertEquals(
            "Native app attempting to read MAC address from /sys/class/net/wlan0/address must get virtual MAC",
            virtualMac,
            sysfsMac
        )
    }
}
