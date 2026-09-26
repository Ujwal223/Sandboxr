package com.sandboxr.virtual

import android.content.Context
import android.content.ContextWrapper
import com.sandboxr.virtual.hardware.HardwareSpoofer
import com.sandboxr.virtual.hardware.NativeHookBridge
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

class HardwareSpoofVerificationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private val testEnvUuid = UUID.randomUUID().toString()

    @Before
    fun setUp() {
        val filesDir = tempFolder.newFolder("test_files")
        mockContext = object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = filesDir
        }
        NativeHookBridge.clear()
        NativeHookBridge.init()
    }

    @After
    fun tearDown() {
        NativeHookBridge.clear()
        HardwareSpoofer.clearProfiles()
    }

    @Test
    fun testDiagnosticZeroHardwareLeaks() {
        // 1. Generate and apply deterministic environment profile
        val profile = HardwareSpoofer.getOrCreateProfile(mockContext, testEnvUuid)
        HardwareSpoofer.applyProfile(profile)

        // 2. Validate IMEI diagnostic query (passes Luhn and matches profile)
        assertTrue("IMEI must be Luhn-compliant", HardwareSpoofer.isValidImei(profile.imei))
        assertEquals("Environment IMEI must match profile", profile.imei, HardwareSpoofer.getOrCreateProfile(mockContext, testEnvUuid).imei)

        // 3. Validate Serial diagnostic query (Java System.getProperty)
        val javaSerial = System.getProperty("ro.serialno")
        assertEquals("Reported Java ro.serialno must match environment profile", profile.serial, javaSerial)

        // 4. Validate Serial diagnostic query (Native getprop)
        val nativeSerial = NativeHookBridge.getNativeProp("ro.serialno")
        assertNotNull("Native getprop for ro.serialno must not be null", nativeSerial)
        assertEquals("Reported Native ro.serialno must match environment profile", profile.serial, nativeSerial)

        // 5. Validate MAC address diagnostic query (sysfs read)
        val sysfsMac = NativeHookBridge.readMacFromSysfs("/sys/class/net/wlan0/address")
        assertEquals("Reported sysfs MAC address must match environment profile", profile.macAddress, sysfsMac)

        // 6. Validate 64-bit Android ID format (16 hex chars)
        assertEquals("Android ID must be 16-character hex", 16, profile.androidId.length)
        assertTrue("Android ID must contain only hex characters", profile.androidId.matches(Regex("^[0-9a-f]{16}$")))

        // 7. Validate Advertising ID format
        assertNotNull("Advertising ID must be valid UUID", UUID.fromString(profile.advertisingId))

        // 8. Validate WiFi BSSID format
        assertTrue("WiFi BSSID must be valid MAC format", profile.wifiBssid.matches(Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")))
    }
}
