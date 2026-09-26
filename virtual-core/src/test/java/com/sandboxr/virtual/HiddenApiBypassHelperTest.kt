package com.sandboxr.virtual

import com.sandboxr.virtual.compat.HiddenApiBypassHelper
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Emulate Android 14 (API 34)
class HiddenApiBypassHelperTest {

    @Test
    fun testExemptAll() {
        val result = HiddenApiBypassHelper.exemptAll()
        assertTrue("HiddenApiBypassHelper.exemptAll() should return true", result)
    }

    @Test
    fun testHiddenBinderInterfaceReflectionOnAndroid14() {
        // 1. Verify exemption succeeds without crashing
        val exempt = HiddenApiBypassHelper.exemptAll()
        assertTrue("Exemption must succeed on Android 14", exempt)

        // 2. Reflect hidden ServiceManager class
        val serviceManagerClass = Class.forName("android.os.ServiceManager")
        assertNotNull("ServiceManager class must be accessible via reflection", serviceManagerClass)

        // 3. Reflect hidden sCache field in ServiceManager
        val sCacheField = HiddenApiBypassHelper.getDeclaredField(serviceManagerClass, "sCache")
        sCacheField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val sCache = sCacheField.get(null) as? Map<*, *>
        assertNotNull("ServiceManager.sCache field must be accessible via HiddenApiBypassHelper", sCache)

        // 4. Test hidden ServiceManager.getService reflection without crashing
        val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
        assertNotNull("getService method must be reflectable", getServiceMethod)

        // Invoke hidden ServiceManager.getService via Helper
        HiddenApiBypassHelper.invokeStaticMethod(
            serviceManagerClass,
            "getService",
            "activity"
        )

        // 5. Test reflection against hidden IPackageManager & IActivityManager interfaces
        val pmInterfaceClass = Class.forName("android.content.pm.IPackageManager")
        assertNotNull("IPackageManager interface must be reflectable", pmInterfaceClass)

        val amInterfaceClass = Class.forName("android.app.IActivityManager")
        assertNotNull("IActivityManager interface must be reflectable", amInterfaceClass)
    }

    @Test
    fun testHiddenFieldAccess() {
        class TestTarget {
            private val secretHardwareToken = "SEC_TOKEN_99182"
        }

        val target = TestTarget()
        val token = HiddenApiBypassHelper.getFieldValue(target, "secretHardwareToken")
        assertTrue("Hidden field must be retrieved via reflection helper", token == "SEC_TOKEN_99182")

        HiddenApiBypassHelper.setFieldValue(target, "secretHardwareToken", "UPDATED_TOKEN_445")
        val updated = HiddenApiBypassHelper.getFieldValue(target, "secretHardwareToken")
        assertTrue("Hidden field must be updated via reflection helper", updated == "UPDATED_TOKEN_445")
    }
}
