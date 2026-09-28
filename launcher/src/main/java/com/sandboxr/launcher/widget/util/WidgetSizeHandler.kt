/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.sandboxr.launcher.widget.util

import android.content.Context
import android.os.Bundle
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import java.util.concurrent.Executor
import javax.inject.Inject

/**
 * Helper class for handling widget updates and sizing options.
 */
@LauncherAppSingleton
open class WidgetSizeHandler
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {

    open fun updateSizeRangesAsync(
        widgetId: Int,
        spanX: Int,
        spanY: Int,
    ) {
    }

    open fun updateSizeRangesAsync(
        widgetId: Int,
        spanX: Int,
        spanY: Int,
        executor: Executor,
    ) {
    }

    open fun getWidgetSizeOptions(spanX: Int, spanY: Int): Bundle {
        return Bundle()
    }
}
