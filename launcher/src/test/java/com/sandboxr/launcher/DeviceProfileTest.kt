/*
 * Copyright (C) 2026 The Android Open Source Project
 * Copyright (C) 2026 Sandboxr Platform
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandboxr.launcher

import android.content.Context
import android.graphics.Rect
import com.sandboxr.launcher.util.NavigationMode
import com.sandboxr.launcher.util.WindowBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceProfileTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun testDeviceProfileDefaultConstructor() {
        val dp = DeviceProfile()
        assertNotNull(dp)
        assertEquals(5, dp.inv?.numRows ?: 5)
        assertEquals(5, dp.inv?.numColumns ?: 5)
        assertFalse(dp.isTablet)
        assertFalse(dp.isTwoPanels)
    }

    @Test
    fun testInvariantDeviceProfileSingleton() {
        val idp1 = InvariantDeviceProfile.INSTANCE(context)
        assertNotNull(idp1)
        val idp2 = InvariantDeviceProfile.get(context)
        assertEquals(idp1, idp2)
        assertTrue(idp1.numRows > 0)
        assertTrue(idp1.numColumns > 0)
    }

    @Test
    fun testIDPChangeListener() {
        val idp = InvariantDeviceProfile(context)
        var changed = false
        var modelChanged = false

        val listener = InvariantDeviceProfile.OnIDPChangeListener { propChanged ->
            changed = true
            modelChanged = propChanged
        }

        idp.addOnChangeListener(listener)
        idp.notifyChange(true)
        assertTrue("Listener should be triggered", changed)
        assertTrue("Model properties should have changed", modelChanged)

        changed = false
        idp.removeOnChangeListener(listener)
        idp.notifyChange(false)
        assertFalse("Removed listener should not be triggered", changed)
    }

    @Test
    fun testPhoneProfileMetrics() {
        // Standard phone: 360x780 @ 160dpi (360dp sw < 600dp)
        val bounds = WindowBounds(Rect(0, 0, 360, 780), Rect(0, 24, 0, 48))
        val idp = InvariantDeviceProfile(context)
        val dp = idp.getDeviceProfile(context, bounds)

        assertNotNull(dp)
        assertFalse("Phone (<600dp) should not be classified as tablet", dp.isTablet)
        assertEquals(idp.numRows, dp.inv?.numRows ?: idp.numRows)
        assertEquals(idp.numColumns, dp.inv?.numColumns ?: idp.numColumns)
    }

    @Test
    fun testTabletProfileMetrics() {
        // Tablet: 800x1280 @ 160dpi (800dp sw >= 600dp)
        val bounds = WindowBounds(Rect(0, 0, 800, 1280), Rect(0, 24, 0, 48))
        val idp = InvariantDeviceProfile(context)
        val dp = idp.getDeviceProfile(context, bounds)

        assertNotNull(dp)
        assertTrue("800dp smallest width should be classified as tablet", dp.isTablet)
    }

    @Test
    fun testPxFromDpConversion() {
        // 160 dpi -> 1 dp == 1 px
        assertEquals(16, DeviceProfile.pxFromDp(16f, 160))
        // 320 dpi -> 1 dp == 2 px
        assertEquals(32, DeviceProfile.pxFromDp(16f, 320))
        // 480 dpi -> 1 dp == 3 px
        assertEquals(48, DeviceProfile.pxFromDp(16f, 480))
    }

    @Test
    fun testNavigationModeDetection() {
        assertTrue(NavigationMode.NO_BUTTON.hasGestures)
        assertFalse(NavigationMode.THREE_BUTTONS.hasGestures)
        assertTrue(NavigationMode.TWO_BUTTONS.hasGestures)
    }

    @Test
    fun testSizeSpecCalculatedValue() {
        val fixedSpec = com.sandboxr.launcher.responsive.SizeSpec(fixedSize = 48f)
        assertEquals(48, fixedSpec.getCalculatedValue(1000))

        val percentSpec = com.sandboxr.launcher.responsive.SizeSpec(ofAvailableSpace = 0.15f)
        assertEquals(150, percentSpec.getCalculatedValue(1000))

        val matchWsSpec = com.sandboxr.launcher.responsive.SizeSpec(matchWorkspace = true)
        assertEquals(64, matchWsSpec.getCalculatedValue(1000, workspaceValue = 64))

        val maxRestrictedSpec = com.sandboxr.launcher.responsive.SizeSpec(
            ofAvailableSpace = 0.5f,
            maxSize = 200
        )
        assertEquals(200, maxRestrictedSpec.getCalculatedValue(1000))
    }

    @Test
    fun testSizeSpecRemainderSpace() {
        val remainderSpec = com.sandboxr.launcher.responsive.SizeSpec(ofRemainderSpace = 0.25f)
        assertEquals(100, remainderSpec.getRemainderSpaceValue(400))

        val noRemainderSpec = com.sandboxr.launcher.responsive.SizeSpec()
        assertEquals(10, noRemainderSpec.getRemainderSpaceValue(0, defaultValue = 10))
    }

    @Test
    fun testPaddingFormulaCalculation() {
        // formula: a * (extraSpacePx - c) + b
        val formula = com.sandboxr.launcher.DevicePaddings.PaddingFormula(a = 0.5f, b = 10f, c = 20f)
        // 0.5 * (100 - 20) + 10 = 0.5 * 80 + 10 = 50
        assertEquals(50, formula.calculate(100))
    }

    @Test
    fun testDevicePaddingEmptySpaceHandling() {
        val top = com.sandboxr.launcher.DevicePaddings.PaddingFormula(a = 0.4f, b = 0f, c = 0f)
        val bottom = com.sandboxr.launcher.DevicePaddings.PaddingFormula(a = 0.4f, b = 0f, c = 0f)
        val hotseat = com.sandboxr.launcher.DevicePaddings.PaddingFormula(a = 0.2f, b = 0f, c = 0f)

        val padding = com.sandboxr.launcher.DevicePaddings.DevicePadding(
            maxEmptySpacePx = 100,
            workspaceTopPadding = top,
            workspaceBottomPadding = bottom,
            hotseatBottomPadding = hotseat
        )

        // Sum of formula outputs for 100: 40 + 40 + 20 = 100 == maxEmptySpacePx (diff <= 3)
        assertTrue(padding.isValid())
        assertEquals(100, padding.maxEmptySpacePx)
    }

    @Test
    fun testResponsiveSpecGroupBreakpointSelection() {
        val spec1 = object : com.sandboxr.launcher.responsive.IResponsiveSpec {
            override val maxAvailableSize: Int = 400
            override val dimensionType = com.sandboxr.launcher.responsive.ResponsiveSpec.DimensionType.WIDTH
            override val specType = com.sandboxr.launcher.responsive.ResponsiveSpec.Companion.ResponsiveSpecType.Workspace
        }
        val spec2 = object : com.sandboxr.launcher.responsive.IResponsiveSpec {
            override val maxAvailableSize: Int = 800
            override val dimensionType = com.sandboxr.launcher.responsive.ResponsiveSpec.DimensionType.WIDTH
            override val specType = com.sandboxr.launcher.responsive.ResponsiveSpec.Companion.ResponsiveSpecType.Workspace
        }
        val group = com.sandboxr.launcher.responsive.ResponsiveSpecGroup(
            aspectRatio = 1.0f,
            widthSpecs = listOf(spec2, spec1),
            heightSpecs = listOf(spec1)
        )

        val matched1 = group.getSpec(com.sandboxr.launcher.responsive.ResponsiveSpec.DimensionType.WIDTH, 350)
        assertEquals(400, matched1.maxAvailableSize)

        val matched2 = group.getSpec(com.sandboxr.launcher.responsive.ResponsiveSpec.DimensionType.WIDTH, 700)
        assertEquals(800, matched2.maxAvailableSize)
    }
}
