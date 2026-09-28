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

import android.content.Context
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import javax.inject.Inject

/**
 * Interface for providing default values for preferences and other configuration settings.
 */
@LauncherAppSingleton
interface DefaultsValueProvider {
    val enableTwoLineToggle: Boolean
    val addIconToHome: Boolean

    companion object {
        @JvmStatic
        fun get(context: Context): DefaultsValueProvider = BaseDefaultsValueProvider()
    }
}

/** Default implementation of [DefaultsValueProvider]. */
@LauncherAppSingleton
class BaseDefaultsValueProvider @Inject constructor() : DefaultsValueProvider {
    override val enableTwoLineToggle: Boolean = false
    override val addIconToHome: Boolean = true
}
