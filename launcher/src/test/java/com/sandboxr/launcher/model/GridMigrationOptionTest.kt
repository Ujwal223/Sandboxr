/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.sandboxr.launcher.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GridMigrationOptionTest {

    @Test
    fun canMigrateFrom4x5To5x5NotAfterRestoreShouldReturnTrue() {
        val source = GridMigrationOption.from(4, 5)
        val dest = GridMigrationOption.from(5, 5)
        assertNotNull(source)
        assertNotNull(dest)
        assertTrue(source!!.canMigrate(dest!!, isAfterRestore = false))
    }

    @Test
    fun canMigrateFrom4x5To4x6NotAfterRestoreShouldReturnTrue() {
        val source = GridMigrationOption.from(4, 5)
        val dest = GridMigrationOption.from(4, 6)
        assertNotNull(source)
        assertNotNull(dest)
        assertTrue(source!!.canMigrate(dest!!, isAfterRestore = false))
    }

    @Test
    fun canMigrateFrom4x5To7x7NotAfterRestoreShouldReturnFalse() {
        val source = GridMigrationOption.from(4, 5)
        val dest = GridMigrationOption.from(7, 7)
        assertNotNull(source)
        assertNull(dest)
    }

    @Test
    fun canMigrateFrom8x8To4x5NotAfterRestoreShouldReturnFalse() {
        val source = GridMigrationOption.from(8, 8)
        assertNull(source)
    }

    @Test
    fun canMigrateFrom6x5To2x2NotAfterRestoreShouldReturnFalse() {
        val source = GridMigrationOption.from(6, 5)
        val dest = GridMigrationOption.from(2, 2)
        assertNotNull(source)
        assertNotNull(dest)
        assertFalse(source!!.canMigrate(dest!!, isAfterRestore = false))
    }

    @Test
    fun canMigrateFrom6x5To6x5NotAfterRestoreShouldReturnTrue() {
        val source = GridMigrationOption.from(6, 5)
        val dest = GridMigrationOption.from(6, 5)
        assertNotNull(source)
        assertNotNull(dest)
        assertTrue(source!!.canMigrate(dest!!, isAfterRestore = false))
    }

    @Test
    fun canMigrateFrom6x5To2x2AfterRestoreShouldReturnTrue() {
        val source = GridMigrationOption.from(6, 5)
        val dest = GridMigrationOption.from(2, 2)
        assertNotNull(source)
        assertNotNull(dest)
        assertTrue(source!!.canMigrate(dest!!, isAfterRestore = true))
    }

    @Test
    fun canMigrateAllPhonePairs() {
        val phoneGrids = listOf(
            GridMigrationOption.TwoByTwo,
            GridMigrationOption.ThreeByThree,
            GridMigrationOption.FourByFour,
            GridMigrationOption.FourByFive,
            GridMigrationOption.FourBySix,
            GridMigrationOption.FiveByFive,
            GridMigrationOption.FiveBySix,
            GridMigrationOption.SevenByThree,
            GridMigrationOption.EightByThree,
        )

        for (src in phoneGrids) {
            for (dst in phoneGrids) {
                assertTrue("Failed migration from ${src.columns}x${src.rows} to ${dst.columns}x${dst.rows}",
                    src.canMigrate(dst, isAfterRestore = false))
            }
        }
    }
}
