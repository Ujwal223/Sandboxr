package com.sandboxr.virtual

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.sandboxr.virtual.server.gms.GmsInterceptor
import com.sandboxr.virtual.server.pm.VPackageManagerService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GmsInterceptorTest {

    private lateinit var interceptor: GmsInterceptor
    private lateinit var context: Context
    private lateinit var vpm: VPackageManagerService

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        interceptor = GmsInterceptor.get()
        interceptor.clear()
        vpm = VPackageManagerService.get(context)
    }

    @After
    fun tearDown() {
        interceptor.clear()
    }

    @Test
    fun testDefaultPolicyBlocksGmsAndReturnsServiceMissing() {
        val envId = "env-privacy-default"

        // Default must be disabled (blocked)
        assertFalse(interceptor.isGmsEnabled(envId))

        // Availability check must return SERVICE_MISSING (code 1)
        val status = interceptor.checkAvailability(envId, hostGmsInstalled = true)
        assertEquals(GmsInterceptor.SERVICE_MISSING, status)

        // All GMS core ecosystem packages must be blocked
        assertTrue(interceptor.shouldBlockPackageQuery("com.google.android.gms", envId))
        assertTrue(interceptor.shouldBlockPackageQuery("com.android.vending", envId))
        assertTrue(interceptor.shouldBlockPackageQuery("com.google.android.gsf", envId))
        assertTrue(interceptor.shouldBlockPackageQuery("com.google.android.gms.chimera", envId))

        // Third-party packages must NOT be blocked
        assertFalse(interceptor.shouldBlockPackageQuery("org.mozilla.firefox", envId))
        assertFalse(interceptor.shouldBlockPackageQuery("org.thoughtcrime.securesms", envId))
    }

    @Test
    fun testEnablingGmsAllowsQueriesAndReturnsServiceSuccess() {
        val envId = "env-gms-allowed"
        interceptor.setGmsEnabled(envId, true)

        assertTrue(interceptor.isGmsEnabled(envId))

        // When enabled and host has GMS, status must be SERVICE_SUCCESS (code 0)
        val status = interceptor.checkAvailability(envId, hostGmsInstalled = true)
        assertEquals(GmsInterceptor.SERVICE_SUCCESS, status)

        assertFalse(interceptor.shouldBlockPackageQuery("com.google.android.gms", envId))
        assertFalse(interceptor.shouldBlockPackageQuery("com.android.vending", envId))
    }

    @Test
    fun testServiceIntentInterception() {
        val envId = "env-intent-test"
        val gmsServiceIntent = Intent().apply {
            component = ComponentName("com.google.android.gms", "com.google.android.gms.chimera.GmsIntentOperationService")
        }
        val normalServiceIntent = Intent().apply {
            component = ComponentName("com.guest.app", "com.guest.app.MyService")
        }

        // When GMS is disabled, GMS service intents must be intercepted & suppressed
        assertTrue(interceptor.interceptServiceIntent(gmsServiceIntent, envId))
        assertFalse(interceptor.interceptServiceIntent(normalServiceIntent, envId))

        // When GMS is enabled, GMS service intents must pass through
        interceptor.setGmsEnabled(envId, true)
        assertFalse(interceptor.interceptServiceIntent(gmsServiceIntent, envId))
    }

    @Test
    fun testGracefulDegradationWhenGmsIsDisabled() {
        val envBlocked = "env-blocked"
        val envAllowed = "env-allowed"

        interceptor.setGmsEnabled(envBlocked, false)
        interceptor.setGmsEnabled(envAllowed, true)

        // Mock app checking GMS availability and deciding fallback
        fun resolveMapEngine(envId: String): String {
            val availability = interceptor.checkAvailability(envId, hostGmsInstalled = true)
            return if (availability == GmsInterceptor.SERVICE_SUCCESS) {
                "GoogleMapsEngine"
            } else {
                // Graceful fallback to OpenStreetMap without throwing exceptions
                "OpenStreetMapEngine"
            }
        }

        assertEquals("OpenStreetMapEngine", resolveMapEngine(envBlocked))
        assertEquals("GoogleMapsEngine", resolveMapEngine(envAllowed))
    }

    @Test
    fun testPackageManagerIntegrationWithGmsInterceptor() {
        val envBlocked = "env-pm-blocked"
        val envAllowed = "env-pm-allowed"

        interceptor.setGmsEnabled(envBlocked, false)
        interceptor.setGmsEnabled(envAllowed, true)

        val gmsPkgInfo = PackageInfo().apply {
            packageName = "com.google.android.gms"
            versionName = "24.0.0"
            applicationInfo = ApplicationInfo().apply {
                packageName = "com.google.android.gms"
                sourceDir = "/system/priv-app/PrebuiltGmsCore.apk"
            }
        }

        vpm.registerPackage(gmsPkgInfo, envBlocked)
        vpm.registerPackage(gmsPkgInfo, envAllowed)

        // Blocked environment must return null for getPackageInfo
        assertNull(
            "Blocked environment must hide GMS package",
            vpm.getPackageInfo("com.google.android.gms", 0, envBlocked)
        )
        assertNull(
            "Blocked environment must hide GMS application",
            vpm.getApplicationInfo("com.google.android.gms", 0, envBlocked)
        )

        // Allowed environment must return PackageInfo normally
        val retrievedAllowed = vpm.getPackageInfo("com.google.android.gms", 0, envAllowed)
        assertNotNull("Allowed environment must return GMS package info", retrievedAllowed)
        assertEquals("com.google.android.gms", retrievedAllowed?.packageName)
    }
}
