package com.sandboxr.virtual

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.core.content.IntentCompat
import com.sandboxr.virtual.client.stub.StubActivity
import com.sandboxr.virtual.server.am.VActivityManagerService
import com.sandboxr.virtual.server.pm.VPackageManagerService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class VActivityManagerServiceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var vam: VActivityManagerService
    private lateinit var vpm: VPackageManagerService

    @Before
    fun setup() {
        val filesDir = tempFolder.newFolder("vam_test_files")
        val dummyContext = object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = filesDir
            override fun getPackageName(): String = "com.sandboxr.app"
        }
        mockContext = dummyContext
        vam = VActivityManagerService.get(dummyContext)
        vpm = VPackageManagerService.get(dummyContext)
    }

    @Test
    fun testIntentRewritingToStubActivity() {
        val envId = "env-vam-test"
        val guestPkg = "com.privacy.guestapp"
        val guestActivity = "com.privacy.guestapp.SplashActivity"

        val actInfo = ActivityInfo().apply {
            packageName = guestPkg
            name = guestActivity
            exported = true
        }

        val pkgInfo = PackageInfo().apply {
            packageName = guestPkg
            versionName = "1.0.0"
            applicationInfo = ApplicationInfo().apply {
                packageName = guestPkg
                sourceDir = "/envs/$envId/data/$guestPkg/base.apk"
            }
            activities = arrayOf(actInfo)
        }

        vpm.registerPackage(pkgInfo, envId)

        val originalIntent = Intent().apply {
            component = ComponentName(guestPkg, guestActivity)
            putExtra("test_extra_key", "test_value_123")
        }

        // Test rewrite
        val stubIntent = vam.createStubIntent(originalIntent, envId)
        assertNotNull("StubIntent must be generated for registered guest activity", stubIntent)
        assertEquals(StubActivity::class.java.name, stubIntent?.component?.className)

        // Verify embedded metadata
        assertEquals(envId, stubIntent?.getStringExtra(VActivityManagerService.EXTRA_ENV_ID))
        assertEquals(guestPkg, stubIntent?.getStringExtra(VActivityManagerService.EXTRA_TARGET_PKG))
        assertEquals(guestActivity, stubIntent?.getStringExtra(VActivityManagerService.EXTRA_TARGET_ACTIVITY))
        val embeddedOriginal = stubIntent?.let {
            IntentCompat.getParcelableExtra(it, VActivityManagerService.EXTRA_TARGET_INTENT, Intent::class.java)
        }
        assertNotNull("Original intent must be preserved in extras", embeddedOriginal)
    }

    @Test
    fun testProcessLifecycleTracking() {
        val envId = "env-proc-test"
        val pkg1 = "com.guest.app1"
        val pkg2 = "com.guest.app2"

        vam.recordProcessStart(pkg1, envId)
        vam.recordProcessStart(pkg2, envId)

        val running = vam.getRunningPackages(envId)
        assertEquals(2, running.size)
        assertTrue(running.contains(pkg1))
        assertTrue(running.contains(pkg2))

        vam.recordProcessStop(pkg1, envId)
        val remaining = vam.getRunningPackages(envId)
        assertEquals(1, remaining.size)
        assertFalse(remaining.contains(pkg1))
        assertTrue(remaining.contains(pkg2))
    }

    @Test
    fun testUnregisteredActivityIntentReturnsNull() {
        val unregIntent = Intent().apply {
            component = ComponentName("com.unregistered.app", "com.unregistered.app.UnknownActivity")
        }
        val stubIntent = vam.createStubIntent(unregIntent, "env-empty")
        assertNull("Stub intent must be null for non-installed package", stubIntent)
    }

    @Test
    fun testActiveProcessesInEnvironmentATerminatedWhenSwitchingToEnvironmentB() {
        val envA = "env-uuid-alpha"
        val envB = "env-uuid-beta"

        val pkgA1 = "com.social.network"
        val pkgA2 = "com.location.tracker"
        val pkgB1 = "com.work.chat"

        vam.recordProcessStart(pkgA1, envA)
        vam.recordProcessStart(pkgA2, envA)
        vam.recordProcessStart(pkgB1, envB)

        assertEquals(2, vam.getRunningPackages(envA).size)
        assertEquals(1, vam.getRunningPackages(envB).size)

        // Track lifecycle freeze listener
        var frozenEnv: String? = null
        val killedPkgs = mutableSetOf<String>()
        vam.addLifecycleListener(object : com.sandboxr.virtual.server.am.ProcessLifecycleListener {
            override fun onEnvironmentFrozen(envId: String, killedPackages: Set<String>) {
                frozenEnv = envId
                killedPkgs.addAll(killedPackages)
            }
        })

        // Switch from Environment A to Environment B (freezeWhenInactive is true by default)
        vam.onEnvironmentSwitched(envA, envB)

        // 1. All processes in Environment A must be terminated
        assertEquals(0, vam.getRunningPackages(envA).size)
        assertFalse("pkgA1 must not be running after freeze", vam.isProcessRunning(pkgA1, envA))
        assertFalse("pkgA2 must not be running after freeze", vam.isProcessRunning(pkgA2, envA))

        // 2. Active processes in Environment B must remain active and unaffected
        assertEquals(1, vam.getRunningPackages(envB).size)
        assertTrue("pkgB1 must remain running in target environment", vam.isProcessRunning(pkgB1, envB))

        // 3. Listener must receive notification
        assertEquals(envA, frozenEnv)
        assertEquals(setOf(pkgA1, pkgA2), killedPkgs)
    }

    @Test
    fun testEnvironmentWithFreezeDisabledKeepsProcessesAliveOnSwitch() {
        val envC = "env-uuid-charlie"
        val envD = "env-uuid-delta"
        val backgroundPkg = "com.background.downloader"

        vam.setFreezeWhenInactive(envC, false)
        vam.recordProcessStart(backgroundPkg, envC)

        // Switch from C to D
        vam.onEnvironmentSwitched(envC, envD)

        // Environment C had freeze disabled; process must stay alive
        assertEquals(1, vam.getRunningPackages(envC).size)
        assertTrue("Process must remain running when freezeWhenInactive is false", vam.isProcessRunning(backgroundPkg, envC))
    }
}
