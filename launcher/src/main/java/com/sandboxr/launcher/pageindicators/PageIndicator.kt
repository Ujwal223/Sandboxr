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

package com.sandboxr.launcher.pageindicators

/**
 * Base contract for workspace page indicators.
 */
interface PageIndicator {

    fun setScroll(currentScroll: Int, totalScroll: Int)

    fun setActiveMarker(activePage: Int)

    fun setMarkersCount(numMarkers: Int)

    fun setPauseScroll(pause: Boolean, isTwoPanels: Boolean) {
        // No-op by default
    }

    fun setShouldAutoHide(shouldAutoHide: Boolean) {
        // No-op by default
    }

    fun pauseAnimations() {
        // No-op by default
    }

    fun skipAnimationsToEnd() {
        // No-op by default
    }

    fun setPaintColor(color: Int) {
        // No-op by default
    }
}
