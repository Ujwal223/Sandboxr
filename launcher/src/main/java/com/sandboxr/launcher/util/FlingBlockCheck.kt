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

import android.os.SystemClock

/**
 * Determines whether a fling should be blocked. We block flings when crossing thresholds
 * to new states, and unblock after a short duration pause.
 */
class FlingBlockCheck {

    private var mBlockFling = false
    private var mBlockFlingTime: Long = 0

    fun blockFling() {
        mBlockFling = true
        mBlockFlingTime = SystemClock.uptimeMillis()
    }

    fun unblockFling() {
        mBlockFling = false
        mBlockFlingTime = 0
    }

    fun onEvent() {
        if (SystemClock.uptimeMillis() - mBlockFlingTime >= UNBLOCK_FLING_PAUSE_DURATION) {
            mBlockFling = false
        }
    }

    fun isBlocked(): Boolean = mBlockFling

    companion object {
        // Allow flinging to a new state after waiting this many milliseconds.
        private const val UNBLOCK_FLING_PAUSE_DURATION: Long = 200
    }
}
