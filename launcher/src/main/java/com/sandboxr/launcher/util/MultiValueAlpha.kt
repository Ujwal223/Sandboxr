/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.view.View
import com.sandboxr.launcher.LauncherAnimUtils.VIEW_ALPHA
import com.sandboxr.launcher.anim.AlphaUpdateListener

/**
 * Utility class to handle separating a single value as a factor of multiple values.
 */
open class MultiValueAlpha @JvmOverloads constructor(
    view: View,
    size: Int,
    private val mHiddenVisibility: Int = View.INVISIBLE
) : MultiPropertyFactory<View>(view, VIEW_ALPHA, size, ALPHA_AGGREGATOR, 1f) {

    private var mUpdateVisibility: Boolean = false

    fun setUpdateVisibility(updateVisibility: Boolean) {
        mUpdateVisibility = updateVisibility
    }

    override fun apply(value: Float) {
        super.apply(value)
        if (mUpdateVisibility) {
            AlphaUpdateListener.updateVisibility(target, mHiddenVisibility)
        }
    }

    companion object {
        private val ALPHA_AGGREGATOR = FloatBiFunction { a, b -> a * b }
    }
}
