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

package com.sandboxr.launcher.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.os.UserHandle
import com.sandboxr.launcher.graphics.IconShape
import org.junit.Assert.assertEquals
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
class BaseIconFactoryTest {

    private lateinit var context: Context
    private lateinit var factory: BaseIconFactory

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        factory = BaseIconFactory(context, fillResIconDpi = 0, iconBitmapSize = 120)
    }

    @Test
    fun testCreateIconBitmapFromRawBitmap() {
        val input = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
        val info = factory.createIconBitmap(input)

        assertNotNull(info)
        assertEquals(120, info.icon.width)
        assertEquals(120, info.icon.height)
    }

    @Test
    fun testCreateBadgedIconBitmapAdaptive() {
        val fg = ColorDrawable(Color.RED)
        val bg = ColorDrawable(Color.BLUE)
        val adaptive = AdaptiveIconDrawable(bg, fg)

        val info = factory.createBadgedIconBitmap(adaptive)
        assertNotNull(info)
        assertEquals(120, info.icon.width)
        assertEquals(120, info.icon.height)
    }

    @Test
    fun testCreateBadgedIconBitmapWithEnvironmentColor() {
        val drawable = ColorDrawable(Color.GREEN)
        val envColor = Color.parseColor("#00E5FF")

        val info = factory.createBadgedIconBitmap(
            drawable = drawable,
            envColor = envColor
        )
        assertNotNull(info)
        assertEquals(120, info.icon.width)
    }

    @Test
    fun testCreateBadgedIconBitmapThemed() {
        val drawable = AdaptiveIconDrawable(ColorDrawable(Color.BLACK), ColorDrawable(Color.WHITE))
        val info = factory.createBadgedIconBitmap(
            drawable = drawable,
            isThemed = true
        )
        assertNotNull(info)
        assertTrue((info.flags and BitmapInfo.FLAG_THEMED) != 0)
    }

    @Test
    fun testCreateShapedIconBitmapSquircle() {
        val drawable = ColorDrawable(Color.MAGENTA)
        val squircle = IconShape.Squircle()
        val path = squircle.getPath(60f)

        val shaped = factory.createShapedIconBitmap(drawable, path)
        assertNotNull(shaped)
        assertEquals(120, shaped.icon.width)
        assertEquals(120, shaped.icon.height)
    }
}
