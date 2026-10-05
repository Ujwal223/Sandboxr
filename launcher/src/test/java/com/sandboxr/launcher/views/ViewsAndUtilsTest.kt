/*
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

package com.sandboxr.launcher.views

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sandboxr.launcher.util.ComponentKey
import com.sandboxr.launcher.util.PackageManagerHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViewsAndUtilsTest {

    private class MockFloatingView(context: Context) : AbstractFloatingView(context) {
        init {
            mIsOpen = true
        }

        override fun handleClose(animate: Boolean) {
            mIsOpen = false
        }

        override fun isOfType(type: Int): Boolean = (type and TYPE_FOLDER) != 0
    }

    @Test
    fun testAbstractFloatingViewLifecycle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockView = MockFloatingView(context)

        assertTrue(mockView.isOpen)
        assertTrue(mockView.isOfType(AbstractFloatingView.TYPE_FOLDER))
        assertFalse(mockView.isOfType(AbstractFloatingView.TYPE_SNACKBAR))

        mockView.close(false)
        assertFalse(mockView.isOpen)
    }

    @Test
    fun testBaseDragLayerInstantiation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dragLayer = BaseDragLayer<Context>(context, null)
        assertNotNull(dragLayer)
    }

    @Test
    fun testRecyclerViewFastScrollerInstantiation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scroller = RecyclerViewFastScroller(context)
        assertNotNull(scroller)
    }

    @Test
    fun testPackageManagerHelper() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = PackageManagerHelper(context)
        assertNotNull(helper)
    }
}
