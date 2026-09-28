/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.ComponentName
import android.os.UserHandle
import java.util.Objects

/**
 * A key that uniquely identifies a component for a particular user.
 */
open class ComponentKey(
    @JvmField val componentName: ComponentName,
    @JvmField val user: UserHandle,
) {
    private val mHashCode: Int = Objects.hash(componentName, user)

    override fun hashCode(): Int = mHashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComponentKey) return false
        return componentName == other.componentName && user == other.user
    }

    override fun toString(): String {
        return "${componentName.flattenToString()}#$user"
    }
}
