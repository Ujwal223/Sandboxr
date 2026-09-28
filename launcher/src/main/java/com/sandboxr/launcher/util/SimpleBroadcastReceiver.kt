/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PatternMatcher.PATTERN_LITERAL
import androidx.annotation.AnyThread
import com.sandboxr.launcher.dagger.ApplicationContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.function.Consumer

/**
 * Broadcast Receiver with utility methods for asynchronous lifecycle registration.
 */
class SimpleBroadcastReceiver
@AssistedInject
constructor(
    @ApplicationContext private val context: Context,
    @Assisted("executor") private val executor: LooperExecutor,
    @Assisted("callbackExecutor") private val callbackExecutor: LooperExecutor,
    @Assisted private val intentConsumer: Consumer<Intent>,
) : BroadcastReceiver(), SafeCloseable {

    constructor(
        context: Context,
        executor: LooperExecutor,
        intentConsumer: Consumer<Intent>,
    ) : this(
        context = context,
        executor = executor,
        callbackExecutor = executor,
        intentConsumer = intentConsumer,
    )

    override fun onReceive(context: Context, intent: Intent) {
        intentConsumer.accept(intent)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @AnyThread
    @JvmOverloads
    fun register(
        filter: IntentFilter,
        flags: Int = 0,
        permission: String? = null,
        completionCallback: Runnable? = null,
    ) = apply {
        executor.execute {
            try {
                context.registerReceiver(this, filter, permission, callbackExecutor.handler, flags)
            } catch (e: Exception) {
                // Ignore registration exceptions
            }

            if (completionCallback != null) {
                callbackExecutor.execute(completionCallback)
            }
        }
    }

    override fun close() {
        executor.execute {
            try {
                context.unregisterReceiver(this)
            } catch (e: IllegalArgumentException) {
                // Already unregistered or not registered
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("executor") executor: LooperExecutor,
            @Assisted("callbackExecutor") callbackExecutor: LooperExecutor = executor,
            @Assisted intentConsumer: Consumer<Intent>,
        ): SimpleBroadcastReceiver
    }

    companion object {
        @JvmStatic
        fun packageFilter(pkg: String?, vararg actions: String): IntentFilter =
            actionsFilter(*actions).apply {
                addDataScheme("package")
                if (!pkg.isNullOrEmpty()) addDataSchemeSpecificPart(pkg, PATTERN_LITERAL)
            }

        @JvmStatic
        fun actionsFilter(vararg actions: String): IntentFilter =
            IntentFilter().apply { actions.forEach { addAction(it) } }
    }
}
