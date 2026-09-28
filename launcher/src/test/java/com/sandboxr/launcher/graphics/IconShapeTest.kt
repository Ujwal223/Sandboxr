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

package com.sandboxr.launcher.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IconShapeTest {

    @Test
    fun testCircleShapeDrawAndPath() {
        val circle = IconShape.Circle()
        val path = circle.getPath(50f)
        assertNotNull(path)
        assertFalse(path.isEmpty)

        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        circle.drawShape(canvas, 0f, 0f, 50f, Paint())
        assertNotNull(bmp)
    }

    @Test
    fun testRoundedSquareShapeDrawAndPath() {
        val roundedSquare = IconShape.RoundedSquare(radiusRatio = 0.35f)
        val path = roundedSquare.getPath(50f)
        assertNotNull(path)
        assertFalse(path.isEmpty)

        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        roundedSquare.drawShape(canvas, 0f, 0f, 50f, Paint())
        assertNotNull(bmp)
    }

    @Test
    fun testSquircleSuperellipseShape() {
        val squircle = IconShape.Squircle(exponent = 4.0f)
        val path = squircle.getPath(50f)
        assertNotNull(path)
        assertFalse(path.isEmpty)

        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        squircle.drawShape(canvas, 0f, 0f, 50f, Paint())
        assertNotNull(bmp)
    }

    @Test
    fun testTearDropShapeDrawAndPath() {
        val tearDrop = IconShape.TearDrop(radiusRatio = 0.3f)
        val path = tearDrop.getPath(50f)
        assertNotNull(path)
        assertFalse(path.isEmpty)

        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        tearDrop.drawShape(canvas, 0f, 0f, 50f, Paint())
        assertNotNull(bmp)
    }
}
