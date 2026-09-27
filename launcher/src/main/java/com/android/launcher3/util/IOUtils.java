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

package com.android.launcher3.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * AOSP compatibility bridge for {@link com.sandboxr.launcher.util.IOUtils}.
 */
public final class IOUtils {

    public static byte[] toByteArray(File file) throws IOException {
        return com.sandboxr.launcher.util.IOUtils.toByteArray(file);
    }

    public static byte[] toByteArray(InputStream in) throws IOException {
        return com.sandboxr.launcher.util.IOUtils.toByteArray(in);
    }

    public static long copy(InputStream from, OutputStream to) throws IOException {
        return com.sandboxr.launcher.util.IOUtils.copy(from, to);
    }

    public static void closeSilently(Closeable c) {
        com.sandboxr.launcher.util.IOUtils.closeSilently(c);
    }

    public static void closeSilently(AutoCloseable c) {
        com.sandboxr.launcher.util.IOUtils.closeSilently(c);
    }

    private IOUtils() {}
}
