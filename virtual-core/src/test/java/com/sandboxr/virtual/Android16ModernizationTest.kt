package com.sandboxr.virtual

import com.sandboxr.virtual.client.stub.StubActivity
import com.sandboxr.virtual.client.stub.StubFragment
import com.sandboxr.virtual.compat.HiddenApiBypassHelper
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Android16ModernizationTest {

    @Test
    fun testHiddenApiBypassHelperExemptionsAndStrictModeCompliance() {
        val exempted = HiddenApiBypassHelper.exemptAll()
        assertTrue("exemptAll must return true", exempted)

        // StrictMode hidden API policy disable must execute cleanly
        HiddenApiBypassHelper.disableStrictModeHiddenApiPolicy()

        // Test field and method reflection fallback
        val testObj = TestReflectTarget()
        val result = HiddenApiBypassHelper.invokeMethod(testObj, "secretMethod", "testPayload")
        assertTrue("invokeMethod must return expected result", result == "reflected:testPayload")

        val fieldVal = HiddenApiBypassHelper.getFieldValue(testObj, "secretField")
        assertTrue("getFieldValue must match secretField", fieldVal == "initialSecret")

        val setSuccess = HiddenApiBypassHelper.setFieldValue(testObj, "secretField", "updatedSecret")
        assertTrue("setFieldValue must return true", setSuccess)
        assertTrue("Field value must be updated", testObj.getSecret() == "updatedSecret")
    }

    @Test
    fun testStubActivityEdgeToEdgeAndPredictiveBack() {
        val controller = Robolectric.buildActivity(StubActivity::class.java)
        val activity = controller.get()
        assertNotNull("StubActivity instance must not be null", activity)

        controller.create()
        controller.start()
        controller.resume()

        // Verify onBackPressedDispatcher has active callbacks registered for predictive back
        assertTrue(
            "onBackPressedDispatcher must have callbacks registered",
            activity.onBackPressedDispatcher.hasEnabledCallbacks()
        )

        // Simulate back press - should complete without crash or ANR
        activity.onBackPressedDispatcher.onBackPressed()

        controller.pause()
        controller.stop()
        controller.destroy()
    }

    @Test
    fun testStubFragmentEdgeToEdgeAndPredictiveBack() {
        val controller = Robolectric.buildActivity(StubActivity::class.java).setup()
        val activity = controller.get()
        val fragment = StubFragment()

        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        assertNotNull("StubFragment must not be null", fragment)
        assertNotNull("Fragment view must be created", fragment.view)

        // Trigger predictive back on activity with hosted fragment
        activity.onBackPressedDispatcher.onBackPressed()
    }

    private class TestReflectTarget {
        private var secretField: String = "initialSecret"

        private fun secretMethod(arg: String): String {
            return "reflected:$arg"
        }

        fun getSecret(): String = secretField
    }
}
