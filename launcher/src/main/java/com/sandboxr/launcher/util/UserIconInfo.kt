/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.os.Process
import android.os.UserHandle
import com.android.users.UserType

/**
 * Encapsulates user type and profile information for user badges and icon generation.
 */
data class UserIconInfo @JvmOverloads constructor(
    @JvmField val user: UserHandle = Process.myUserHandle(),
    @JvmField val type: UserType = UserType.MAIN,
    @JvmField val userSerial: Long = 0L,
) {
    val isMain: Boolean get() = type == UserType.MAIN
    val isWork: Boolean get() = type == UserType.WORK
    val isPrivate: Boolean get() = type == UserType.PRIVATE
    val isCloned: Boolean get() = type == UserType.CLONED
}
