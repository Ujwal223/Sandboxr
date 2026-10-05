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

package com.sandboxr.launcher.ui

import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EnvironmentCardMergeTest {

    @Test
    fun testEnvironmentOverlayViewInstantiation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val overlayView = EnvironmentOverlayView(context)

        assertNotNull(overlayView)
        var selectedEnv: String? = null
        overlayView.setOnEnvironmentSelectedListener { card ->
            selectedEnv = card.id
        }
    }

    @Test
    fun testPinSystemCardFirst() {
        val cards = listOf(
            EnvironmentCardData(
                id = "env-work",
                name = "Work",
                color = Color.Green,
                isSystem = false
            ),
            EnvironmentCardData(
                id = "system",
                name = "System",
                color = Color.Blue,
                isSystem = true
            ),
            EnvironmentCardData(
                id = "env-personal",
                name = "Personal",
                color = Color.Red,
                isSystem = false
            )
        )

        val pinned = pinSystemCardFirst(cards)
        assertEquals(3, pinned.size)
        assertTrue(pinned[0].isSystem)
        assertEquals("system", pinned[0].id)
    }

    @Test
    fun testReorderVirtualEnvironmentsPreservesSystemAtZero() {
        val cards = listOf(
            EnvironmentCardData(
                id = "system",
                name = "System",
                color = Color.Blue,
                isSystem = true
            ),
            EnvironmentCardData(
                id = "env-1",
                name = "Env 1",
                color = Color.Green,
                isSystem = false
            ),
            EnvironmentCardData(
                id = "env-2",
                name = "Env 2",
                color = Color.Red,
                isSystem = false
            )
        )

        // Swap virtual index 0 ("env-1") and virtual index 1 ("env-2")
        val reordered = reorderVirtualEnvironments(cards, 0, 1)

        assertEquals("system", reordered[0].id)
        assertEquals("env-2", reordered[1].id)
        assertEquals("env-1", reordered[2].id)
    }
}
