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

    open fun asListenable(): ListenableStream<T> = this

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
open class MutableListenableRef<T>(initialValue: T) : MutableListenableStream<T>(), ListenableRef<T> {

    @Volatile
    override var value: T = initialValue
        protected set

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

    override fun asListenable(): ListenableRef<T> = this
}

/**
 * Interface representing a value reference that additionally reports changes with diff events.
 */
interface DiffAwareRef<T, D> : ListenableRef<T> {
    fun forEachWithDiff(executor: Executor, action: (T, D?) -> Unit): SafeCloseable
}

/**
 * Mutable implementation of [DiffAwareRef].
 */
class MutableDiffAwareRef<T, D>(initialValue: T) : MutableListenableRef<T>(initialValue), DiffAwareRef<T, D> {
    private val diffListeners = CopyOnWriteArrayList<Pair<Executor, (T, D?) -> Unit>>()

    override fun forEachWithDiff(executor: Executor, action: (T, D?) -> Unit): SafeCloseable {
        val entry = Pair(executor, action)
        diffListeners.add(entry)
        val current = value
        executor.execute { action(current, null) }
        return SafeCloseable { diffListeners.remove(entry) }
    }

    fun dispatchValue(value: T, diff: D) {
        super.dispatchValue(value)
        val grouped = diffListeners.groupBy { it.first }
        for ((executor, entries) in grouped) {
            executor.execute {
                for (entry in entries) {
                    if (diffListeners.contains(entry)) {
                        entry.second(value, diff)
                    }
                }
            }
        }
    }

    fun asDiffAware(): DiffAwareRef<T, D> = this
}

typealias ListenableDiffAwareRef<T, D> = DiffAwareRef<T, D>
