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

package com.sandboxr.launcher.util

import android.os.HandlerThread
import android.os.Looper
import android.os.Process

object Executors {
    @JvmField
    val MAIN_EXECUTOR = LooperExecutor(Looper.getMainLooper())

    @JvmField
    val UI_HELPER_EXECUTOR: LooperExecutor = run {
        val thread = HandlerThread("UiThreadHelper", Process.THREAD_PRIORITY_FOREGROUND)
        thread.start()
        LooperExecutor(thread.looper)
    }

    @JvmField
    val MODEL_EXECUTOR: LooperExecutor = run {
        val thread = HandlerThread("launcher-loader", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()
        LooperExecutor(thread.looper)
    }

    @JvmField
    val ORDERED_BG_EXECUTOR: LooperExecutor = run {
        val thread = HandlerThread("BackgroundExecutor", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()
        LooperExecutor(thread.looper)
    }

    @JvmField
    val THREAD_POOL_EXECUTOR: java.util.concurrent.ThreadPoolExecutor = java.util.concurrent.ThreadPoolExecutor(
        maxOf(Runtime.getRuntime().availableProcessors(), 2),
        maxOf(Runtime.getRuntime().availableProcessors(), 2),
        1L,
        java.util.concurrent.TimeUnit.SECONDS,
        java.util.concurrent.LinkedBlockingQueue()
    )
}
