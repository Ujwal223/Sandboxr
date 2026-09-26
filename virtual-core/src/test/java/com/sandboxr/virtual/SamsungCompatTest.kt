package com.sandboxr.virtual

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.IBinder
import android.os.Parcel
import com.sandboxr.virtual.compat.SamsungCompat
import org.junit.After
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
class SamsungCompatTest {

    private lateinit var dummyContext: Context

    @Before
    fun setUp() {
        SamsungCompat.clear()
        dummyContext = object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = File("/tmp")
            override fun getPackageName(): String = "com.sandboxr.virtual"
        }
    }

    @After
    fun tearDown() {
        SamsungCompat.clear()
    }

    @Test
    fun testSamsungDetectionAndVersionInspection() {
        // Defaults on standard Robolectric environment
        val isSamsung = SamsungCompat.isSamsungDevice()
        // Must not crash on non-Samsung or Samsung device
        val oneUiVersion = SamsungCompat.getOneUiVersion()
        val knoxPresent = SamsungCompat.isKnoxPresent()

        if (isSamsung) {
            assertNotNull("OneUI version check should execute cleanly", oneUiVersion)
        } else {
            assertFalse("Non-Samsung device should return false for knoxPresent", knoxPresent)
        }
    }

    @Test
    fun testKnoxBypassBinderProxyCreationAndTransactions() {
        // Intercept Knox services
        val personaProxy = SamsungCompat.interceptServiceBinder("persona")
        assertNotNull("Persona proxy binder must not be null", personaProxy)
        assertTrue("Proxy must report binder is alive", personaProxy.isBinderAlive)
        assertTrue("Proxy ping must succeed", personaProxy.pingBinder())

        // Transact should return true safely without throwing Knox SecurityException
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            val success = personaProxy.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0)
            assertTrue("Transact on Knox proxy should succeed cleanly", success)
        } finally {
            data.recycle()
            reply.recycle()
        }

        // Custom Knox service proxy
        val knoxCustomProxy = SamsungCompat.interceptServiceBinder("knox_custom")
        assertNotNull("Knox custom proxy must not be null", knoxCustomProxy)
        assertEquals("com.samsung.android.knox.IKnoxCustomManager", knoxCustomProxy.interfaceDescriptor)
    }

    @Test
    fun testMultiWindowIntentFlagsApplication() {
        val testIntent = Intent("android.intent.action.VIEW")
        val processedIntent = SamsungCompat.applyMultiWindowIntentFlags(testIntent)

        assertNotNull("Processed intent must not be null", processedIntent)
        // If simulated on Samsung, flags are added; otherwise untouched
        if (SamsungCompat.isSamsungDevice()) {
            val flags = processedIntent.flags
            assertTrue("Intent must have FLAG_ACTIVITY_NEW_TASK", (flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
            assertTrue(
                "Intent must have ALLOW_SPLIT extra",
                processedIntent.getBooleanExtra("com.samsung.android.multiwindow.extra.ALLOW_SPLIT", false)
            )
        }
    }

    @Test
    fun testInitLifecycleDoesNotThrow() {
        SamsungCompat.init(dummyContext)
        // Re-invoking init should be idempotent
        SamsungCompat.init(dummyContext)
    }
}
