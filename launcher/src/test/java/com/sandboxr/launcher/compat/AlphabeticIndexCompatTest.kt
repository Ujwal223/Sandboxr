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

package com.sandboxr.launcher.compat

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlphabeticIndexCompatTest {

    private lateinit var context: Context
    private lateinit var indexCompat: AlphabeticIndexCompat

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        indexCompat = AlphabeticIndexCompat(context)
    }

    @Test
    fun testComputeSectionNameLatin() {
        assertEquals("A", indexCompat.computeSectionName("Android"))
        assertEquals("B", indexCompat.computeSectionName("Browser"))
        assertEquals("C", indexCompat.computeSectionName("Camera"))
    }

    @Test
    fun testComputeSectionNameDigit() {
        assertEquals("#", indexCompat.computeSectionName("1Password"))
        assertEquals("#", indexCompat.computeSectionName("7-Zip"))
    }

    @Test
    fun testComputeSectionNameEmpty() {
        val misc = indexCompat.computeSectionName("")
        assertNotNull(misc)
    }
}
