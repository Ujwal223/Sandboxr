package com.sandboxr.virtual

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import com.sandboxr.virtual.compat.XiaomiCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class XiaomiCompatTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = File("/tmp")
            override fun getPackageName(): String = "com.sandboxr.virtual"
            override fun getPackageManager(): PackageManager {
                return org.robolectric.RuntimeEnvironment.getApplication().packageManager
            }
        }
    }

    @Test
    fun testDeviceAndRomDetection() {
        // Safe execution on standard environment
        val isXiaomi = XiaomiCompat.isXiaomiDevice()
        val isMiuiOrHyperOs = XiaomiCompat.isMiuiOrHyperOs()
        val romVersion = XiaomiCompat.getRomVersion()

        // Should execute cleanly without exception
        if (isXiaomi) {
            assertTrue("Xiaomi device must return true for isMiuiOrHyperOs", isMiuiOrHyperOs)
        }
    }

    @Test
    fun testAutostartSettingsIntentGeneration() {
        val intent = XiaomiCompat.getAutostartSettingsIntent(context)
        assertNotNull("Autostart intent must not be null", intent)
        assertTrue(
            "Autostart intent must include FLAG_ACTIVITY_NEW_TASK",
            (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        )
    }

    @Test
    fun testBatterySaverSettingsIntentGeneration() {
        val intent = XiaomiCompat.getBatterySaverSettingsIntent(context)
        assertNotNull("Battery saver intent must not be null", intent)
        assertTrue(
            "Battery saver intent must include FLAG_ACTIVITY_NEW_TASK",
            (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        )
    }

    @Test
    fun testCustomPermissionsEditorIntentGeneration() {
        val intent = XiaomiCompat.getCustomPermissionsEditorIntent(context)
        assertNotNull("Custom permissions intent must not be null", intent)
        assertTrue(
            "Custom permissions intent must include FLAG_ACTIVITY_NEW_TASK",
            (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        )
    }

    @Test
    fun testBackgroundExemptionPromptCheck() {
        // On non-Xiaomi/non-MIUI environments, should return false
        if (!XiaomiCompat.isMiuiOrHyperOs()) {
            assertFalse(
                "Non-Xiaomi devices should not prompt for background exemption",
                XiaomiCompat.shouldPromptBackgroundExemption(context)
            )
        }
    }
}
