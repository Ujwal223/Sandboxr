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

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor

/**
 * Interface representing a stream of values that can be listened to.
 */
interface ListenableStream<T> {
    fun forEach(executor: Executor, action: (T) -> Unit): SafeCloseable
}

/**
 * Mutable implementation of [ListenableStream].
 */
open class MutableListenableStream<T> : ListenableStream<T> {

    private val listeners = CopyOnWriteArrayList<ListenerEntry<T>>()

    override fun forEach(executor: Executor, action: (T) -> Unit): SafeCloseable {
        val entry = ListenerEntry(executor, action)
        listeners.add(entry)
        return SafeCloseable { listeners.remove(entry) }
    }

    open fun dispatchValue(value: T) {
        // Group by executor so only one Runnable is posted per executor per dispatch
        val grouped = listeners.groupBy { it.executor }
        for ((executor, entries) in grouped) {
            executor.execute {
                for (entry in entries) {
                    if (listeners.contains(entry)) {
                        entry.action(value)
                    }
                }
            }
        }
    }

    private data class ListenerEntry<T>(
        val executor: Executor,
        val action: (T) -> Unit
    )
}

/**
 * Interface representing a value reference that notifies observers on changes.
 */
interface ListenableRef<T> : ListenableStream<T> {
    val value: T
}

/**
 * Mutable implementation of [ListenableRef].
 */
class MutableListenableRef<T>(initialValue: T) : MutableListenableStream<T>(), ListenableRef<T> {

    @Volatile
    override var value: T = initialValue
        private set

    override fun forEach(executor: Executor, action: (T) -> Unit): SafeCloseable {
        val closeable = super.forEach(executor, action)
        val current = value
        executor.execute { action(current) }
        return closeable
    }

    override fun dispatchValue(value: T) {
        this.value = value
        super.dispatchValue(value)
    }

    fun diffAndDispatch(newValue: T) {
        if (value != newValue) {
            dispatchValue(newValue)
        }
    }

    fun asListenable(): ListenableRef<T> = this
}
