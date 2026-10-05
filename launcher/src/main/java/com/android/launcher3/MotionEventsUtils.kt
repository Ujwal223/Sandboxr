/*
 * Copyright (C) 2026 Sandboxr Platform
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.android.launcher3

import android.view.MotionEvent

object MotionEventsUtils {
    const val CLASSIFICATION_MULTI_FINGER_SWIPE =
        com.sandboxr.launcher.MotionEventsUtils.CLASSIFICATION_MULTI_FINGER_SWIPE

    @JvmStatic
    fun isTrackpadScroll(event: MotionEvent): Boolean =
        com.sandboxr.launcher.MotionEventsUtils.isTrackpadScroll(event)

    @JvmStatic
    fun isTrackpadMultiFingerSwipe(event: MotionEvent): Boolean =
        com.sandboxr.launcher.MotionEventsUtils.isTrackpadMultiFingerSwipe(event)

    @JvmStatic
    fun isTrackpadThreeFingerSwipe(event: MotionEvent): Boolean =
        com.sandboxr.launcher.MotionEventsUtils.isTrackpadThreeFingerSwipe(event)

    @JvmStatic
    fun isTrackpadFourFingerSwipe(event: MotionEvent): Boolean =
        com.sandboxr.launcher.MotionEventsUtils.isTrackpadFourFingerSwipe(event)

    @JvmStatic
    fun isTrackpadMotionEvent(event: MotionEvent): Boolean =
        com.sandboxr.launcher.MotionEventsUtils.isTrackpadMotionEvent(event)
}
