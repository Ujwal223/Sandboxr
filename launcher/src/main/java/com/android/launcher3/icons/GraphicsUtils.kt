/*
 * Copyright (C) 2018 The Android Open Source Project
 * Copyright (C) 2026 Sandboxr Platform
 */
package com.android.launcher3.icons

import android.graphics.Matrix
import android.graphics.Path

open class GraphicsUtils : com.sandboxr.launcher.icons.GraphicsUtils() {
    companion object {
        @JvmStatic
        fun generateIconShape(size: Int, path: Path): IconShape = IconShape(path)

        @JvmStatic
        fun Path.resize(fromSize: Float, toSize: Int): Path {
            val matrix = Matrix()
            val scale = toSize.toFloat() / fromSize
            matrix.setScale(scale, scale)
            transform(matrix)
            return this
        }
    }
}
