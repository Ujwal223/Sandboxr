package com.sandboxr.virtual

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import com.sandboxr.virtual.server.VSmsMmsService
import com.sandboxr.virtual.server.am.VActivityManagerService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Android17ModernizationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var vams: VActivityManagerService
    private lateinit var vsms: VSmsMmsService

    @Before
    fun setUp() {
        val filesDir = tempFolder.newFolder("vams_test_files")
        val dummyContext = object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = filesDir
            override fun getPackageName(): String = "com.ujwal.sandboxr"
        }
        vams = VActivityManagerService.createForTesting(dummyContext)
        vams.clear()

        vsms = VSmsMmsService.createForTesting(dummyContext)
        vsms.clear()
    }

    @Test
    fun testApi37ActivityNonRecreationOnKeyboardNavTouchColorModeChanges() {
        // Android 17 (API 37) masks
        val configKeyboard = 0x0010
        val configKeyboardHidden = 0x0020
        val configNavigation = 0x0040
        val configTouchscreen = 0x0008
        val configColorMode = 0x4000

        // In API 37, these changes should NOT require recreation
        assertFalse(
            "Keyboard change must not recreate activity on API 37",
            vams.shouldRecreateOnConfigChange(configKeyboard, targetSdkVersion = 37)
        )
        assertFalse(
            "KeyboardHidden change must not recreate activity on API 37",
            vams.shouldRecreateOnConfigChange(configKeyboardHidden, targetSdkVersion = 37)
        )
        assertFalse(
            "Navigation change must not recreate activity on API 37",
            vams.shouldRecreateOnConfigChange(configNavigation, targetSdkVersion = 37)
        )
        assertFalse(
            "Touchscreen change must not recreate activity on API 37",
            vams.shouldRecreateOnConfigChange(configTouchscreen, targetSdkVersion = 37)
        )
        assertFalse(
            "ColorMode change must not recreate activity on API 37",
            vams.shouldRecreateOnConfigChange(configColorMode, targetSdkVersion = 37)
        )

        // Combined API 37 non-restarting changes
        val combinedNonRestart = configKeyboard or configNavigation or configColorMode
        assertFalse(
            "Combined non-restarting changes must not recreate activity on API 37",
            vams.shouldRecreateOnConfigChange(combinedNonRestart, targetSdkVersion = 37)
        )

        // Configuration change that still requires recreation (e.g. Locale or FontScale)
        val configLocale = 0x0004
        assertTrue(
            "Locale change must still recreate activity",
            vams.shouldRecreateOnConfigChange(configLocale, targetSdkVersion = 37)
        )

        // onConfigurationChanged dispatches in-place without restart for API 37 mask
        val handledInPlace = !vams.onConfigurationChanged(Configuration(), combinedNonRestart)
        assertTrue("Config change must be handled in-place", handledInPlace)
    }

    @Test
    fun testOtpSmsThreeHourAccessDelayWindow() {
        val envId = "env-otp-test"
        val now = System.currentTimeMillis()

        // 1. Message received right now
        val currentRecord = vsms.recordIncomingSms(
            envId = envId,
            sender = "BANK-ALERT",
            body = "Your verification OTP code is 482910. Do not share.",
            timestamp = now
        )
        assertEquals("482910", currentRecord.otpCode)
        assertTrue(currentRecord.isWithinDeliveryWindow(now))

        // 2. Message received 2.5 hours ago (within 3-hour window)
        val twoAndHalfHoursAgo = now - (2 * 3600 * 1000L + 30 * 60 * 1000L)
        val delayedRecord = vsms.recordIncomingSms(
            envId = envId,
            sender = "SECURE-AUTH",
            body = "Your login code is 771234",
            timestamp = twoAndHalfHoursAgo
        )
        assertEquals("771234", delayedRecord.otpCode)
        assertTrue(
            "Message within 2.5 hours must be within 3-hour delivery window",
            delayedRecord.isWithinDeliveryWindow(now)
        )

        // 3. Message received 3.5 hours ago (outside 3-hour window)
        val threeAndHalfHoursAgo = now - (3 * 3600 * 1000L + 30 * 60 * 1000L)
        val expiredRecord = vsms.recordIncomingSms(
            envId = envId,
            sender = "OLD-SERVICE",
            body = "Your code is 123456",
            timestamp = threeAndHalfHoursAgo
        )
        assertFalse(
            "Message 3.5 hours old must be outside 3-hour delivery window",
            expiredRecord.isWithinDeliveryWindow(now)
        )

        // Query active OTP messages
        val activeOtps = vsms.getOtpMessages(envId)
        assertEquals("Must return 2 active OTP messages within 3-hour window", 2, activeOtps.size)

        // Purge expired
        val purged = vsms.purgeExpiredMessages(now)
        assertEquals("Must purge 1 expired message", 1, purged)
        assertEquals("2 messages should remain in history", 2, vsms.getMessages(envId, includeExpired = true).size)
    }

    @Test
    fun testStubManifestAdaptiveLayoutEnforcement() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())

        val manifestXml = manifestFile.readText()

        // Verify resizeableActivity is explicitly enabled and not disabled
        assertFalse("Manifest must not set resizeableActivity to false", manifestXml.contains("android:resizeableActivity=\"false\""))
        assertTrue("Manifest must declare resizeableActivity=true for stub activities", manifestXml.contains("android:resizeableActivity=\"true\""))

        // Verify colorMode and density are declared in configChanges
        assertTrue("Manifest must include colorMode in configChanges", manifestXml.contains("colorMode"))
        assertTrue("Manifest must include density in configChanges", manifestXml.contains("density"))

        // Verify no fixed screen orientations on stub activities
        assertFalse("Manifest must not force fixed portrait", manifestXml.contains("android:screenOrientation=\"portrait\""))
        assertFalse("Manifest must not force fixed landscape", manifestXml.contains("android:screenOrientation=\"landscape\""))
    }
}
