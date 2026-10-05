/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.sandboxr.launcher

import android.os.Build
import android.view.MotionEvent
import androidx.annotation.RequiresApi

/** Handles motion events from trackpad and multi-finger gestures. */
object MotionEventsUtils {

    /** MotionEvent.CLASSIFICATION_MULTI_FINGER_SWIPE is hidden in older SDKs. */
    const val CLASSIFICATION_MULTI_FINGER_SWIPE = 4

    /** MotionEvent.AXIS_GESTURE_SWIPE_FINGER_COUNT is hidden. */
    private const val AXIS_GESTURE_SWIPE_FINGER_COUNT = 53

    @JvmStatic
    fun isTrackpadScroll(event: MotionEvent): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            event.classification == MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
        } else {
            false
        }
    }

    @JvmStatic
    fun isTrackpadMultiFingerSwipe(event: MotionEvent): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            event.classification == CLASSIFICATION_MULTI_FINGER_SWIPE
        } else {
            false
        }
    }

    @JvmStatic
    fun isTrackpadThreeFingerSwipe(event: MotionEvent): Boolean {
        return isTrackpadMultiFingerSwipe(event) &&
                event.getAxisValue(AXIS_GESTURE_SWIPE_FINGER_COUNT) == 3f
    }

    @JvmStatic
    fun isTrackpadFourFingerSwipe(event: MotionEvent): Boolean {
        return isTrackpadMultiFingerSwipe(event) &&
                event.getAxisValue(AXIS_GESTURE_SWIPE_FINGER_COUNT) == 4f
    }

    @JvmStatic
    fun isTrackpadMotionEvent(event: MotionEvent): Boolean {
        return isTrackpadScroll(event) || isTrackpadMultiFingerSwipe(event)
    }
}
