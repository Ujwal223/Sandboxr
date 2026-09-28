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

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

/**
 * Executor implementation that runs tasks on a provided [Looper].
 */
open class LooperExecutor(val looper: Looper) : Executor {

    val handler: Handler = Handler(looper)

    val thread: Thread
        get() = looper.thread

    override fun execute(command: Runnable) {
        if (Looper.myLooper() == looper) {
            command.run()
        } else {
            handler.post(command)
        }
    }

    fun post(command: Runnable) {
        handler.post(command)
    }

    fun postDelayed(command: Runnable, delayMillis: Long) {
        handler.postDelayed(command, delayMillis)
    }

    fun <T> submit(callable: () -> T): java.util.concurrent.Future<T> {
        val future = java.util.concurrent.FutureTask(callable)
        execute(future)
        return future
    }

    open fun elevatePriority(caller: Int) {}
    open fun restorePriority(caller: Int) {}

    companion object {
        const val CALLER_LOADER_TASK = 1 shl 0
        const val CALLER_ICON_CACHE = 1 shl 1
    }
}
