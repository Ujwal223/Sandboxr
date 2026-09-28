/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.launcher3.util

fun interface FlagOp {
    fun apply(flags: Int): Int

    fun addFlag(flag: Int): FlagOp = FlagOp { flags -> apply(flags) or flag }
    fun removeFlag(flag: Int): FlagOp = FlagOp { flags -> apply(flags) and flag.inv() }
    fun setFlag(flag: Int, enable: Boolean): FlagOp = if (enable) addFlag(flag) else removeFlag(flag)

    companion object {
        @JvmField
        val NO_OP: FlagOp = FlagOp { flags -> flags }
    }
}
