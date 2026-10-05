/*
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

package com.sandboxr.launcher.debug

import java.util.StringJoiner
import java.util.function.IntFunction
import java.util.function.LongFunction

/**
 * Utility for formatting flag diffs and debugging bitwise state transitions.
 */
object FlagDebugUtils {

    /** Appends the [flagName] to [this] when the [flag] is set in [flags]. */
    @JvmStatic
    fun StringJoiner.appendFlag(flags: Int, flag: Int, flagName: String) {
        if (flags and flag != 0) {
            add(flagName)
        }
    }

    /** Appends the [flagName] to [this] when the [flag] is set in [flags]. */
    @JvmStatic
    fun StringJoiner.appendFlag(flags: Long, flag: Long, flagName: String) {
        if (flags and flag != 0L) {
            add(flagName)
        }
    }

    /**
     * Produces a human-readable representation of the [current] flags, followed by a diff from [previous].
     */
    @JvmStatic
    fun formatFlagChange(current: Int, previous: Int, flagSerializer: IntFunction<String>): String {
        val result = StringJoiner(" ")
        result.add("[" + flagSerializer.apply(current) + "]")
        val changed = current xor previous
        val added = current and changed
        if (added != 0) {
            result.add("+[" + flagSerializer.apply(added) + "]")
        }
        val removed = previous and changed
        if (removed != 0) {
            result.add("-[" + flagSerializer.apply(removed) + "]")
        }
        return result.toString()
    }

    /**
     * Produces a human-readable representation of the [current] flags, followed by a diff from [previous].
     */
    @JvmStatic
    fun formatFlagChange(
        current: Long,
        previous: Long,
        flagSerializer: LongFunction<String>
    ): String {
        val result = StringJoiner(" ")
        result.add("[" + flagSerializer.apply(current) + "]")
        val changed = current xor previous
        val added = current and changed
        if (added != 0L) {
            result.add("+[" + flagSerializer.apply(added) + "]")
        }
        val removed = previous and changed
        if (removed != 0L) {
            result.add("-[" + flagSerializer.apply(removed) + "]")
        }
        return result.toString()
    }
}
