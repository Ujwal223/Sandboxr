/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.sandboxr.launcher.util

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Supports various IO utility functions.
 */
object IOUtils {

    private const val BUF_SIZE = 0x1000

    @Throws(IOException::class)
    @JvmStatic
    fun toByteArray(file: File): ByteArray {
        FileInputStream(file).use { input ->
            return toByteArray(input)
        }
    }

    @Throws(IOException::class)
    @JvmStatic
    fun toByteArray(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        copy(input, out)
        return out.toByteArray()
    }

    @Throws(IOException::class)
    @JvmStatic
    fun copy(from: InputStream, to: OutputStream): Long {
        val buf = ByteArray(BUF_SIZE)
        var total: Long = 0
        while (true) {
            val r = from.read(buf)
            if (r == -1) break
            to.write(buf, 0, r)
            total += r.toLong()
        }
        return total
    }

    @JvmStatic
    fun closeSilently(c: Closeable?) {
        if (c != null) {
            try {
                c.close()
            } catch (_: Exception) {}
        }
    }

    @JvmStatic
    fun closeSilently(c: AutoCloseable?) {
        if (c != null) {
            try {
                c.close()
            } catch (_: Exception) {}
        }
    }
}
